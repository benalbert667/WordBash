import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

public class Field extends JPanel implements Runnable {

	private static final int W_HEIGHT = 550; //window height
	private static final int W_WIDTH = 400; //window width
	
	//TEXT
	private static final int WORD_SIZE = 18; //text size of falling words
	private static final int PLAYER_SIZE = 20; //text size of typed chars
	private static final int SCORE_SIZE = 30; //text size of score num
	private static final int GAME_OVER_SIZE = 33; //text size of game over message
	private static final int GAME_START_SIZE = 25; //text size of game start message
	private static final String FONT = "Courier New";
	
	//SPEED CONTROLS
	private static final int MAX_DEPLOY_DELAY = 5000; //max time between words
	private static final int MAX_S = 50; //max orig speed of words
	private static final int MIN_S = 25; //min orig speed of words
	
	//COLORS
	private static final Color W_C_COLOR = new Color(15, 217, 193); //completing words
	private static final Color B_COLOR = new Color(238, 238, 238); //background
	
	//OTHER
	private static final int BAR_HEIGHT = W_HEIGHT - PLAYER_SIZE*3; //height of bar
	private static final int BLINK_S = 200; //speed of textbox blinking
	private static final Random RAND = new Random(); //random constant
	private static final String WORDS_FILE_NAME = "/Files/10000-english-words.txt";
	
	private Thread animator; //main thread
	private List<Word> words; //holds all words
	private List<Ellipse2D> explosions; //word bash animation
	private String player; //player text
	private int score; //score
	private int highScore; //highscore
	
	private boolean inGame; //true when playing game
	private boolean gameOver; //true when game has ended
	private boolean gameStart; //true when game is opened
	
	private boolean cubeBlink; //alternates every BLINK_S milliseconds
	
	public Field() {
		words = new ArrayList<>();
		explosions = new CopyOnWriteArrayList<>();
		
		setBackground(B_COLOR);
		setFocusable(true);
		setPreferredSize(new Dimension(W_WIDTH, W_HEIGHT));
		setDoubleBuffered(true);
		
		addKeyListener(new Typer());
		
		initGameStart();
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		
		animator = new Thread(this);
		animator.start();
	}
	
	//INITIATE MODES
	private void initGame() {
		words.clear();
		
		player = "";
		
		inGame = true;
		gameOver = false;
		gameStart = false;
		
		score = 0;
		
		refillWords();
		
	}
	
	private void initGameOver() {
		words.clear();
		
		player = "";
		
		inGame = false;
		gameOver = true;
		gameStart = false;
		
		Font f = new Font(FONT, Font.BOLD, GAME_OVER_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		Word w = new Word("again");
		w.setY(W_HEIGHT/2 + fm.getHeight()*2);
		w.setX(W_WIDTH/2 - fm.charWidth('\''));
		words.add(w);
	}
	
	private void initGameStart() {
		words.clear();
		
		player = "";
		
		inGame = false;
		gameOver = false;
		gameStart = true;
		
		score = 0;
		
		Font f = new Font(FONT, Font.BOLD, GAME_START_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		Word w = new Word("start");
		w.setY(W_HEIGHT/2);
		w.setX((int)(W_WIDTH/2.0 - fm.stringWidth("type \'start\' to begin")/2.0 + fm.stringWidth("type \'")));
		words.add(w);
	}
	
	//PAINT METHODS
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		g.setColor(Color.black);
		g.drawLine(0, BAR_HEIGHT, W_WIDTH, BAR_HEIGHT);
		
		if (inGame) {
			paintScore(g);
			paintWords(g, WORD_SIZE);
		} else if (gameOver) {
			paintGameOver(g);
			paintWords(g, GAME_OVER_SIZE);
		} else if (gameStart) {
			paintGameStart(g);
			paintWords(g, GAME_START_SIZE);
		}
		
		paintPlayer(g);
		paintExplosions(g);
	}
	
	private void paintWords(Graphics g, int size) {
		Font f = new Font(FONT, Font.BOLD, size);
		
		Iterator<Word> dupe = words.iterator();
		
		//paints each word (x pos is leftmost point of str)
		while(dupe.hasNext()) {
			Word w = dupe.next();
			
			if(!w.isActive())
				break;
			
			String s = w.getWord();
			
			if (w.getSpeed() == 0) {
				g.setColor(Color.black);
			} else {
				double m = -255/(double)(BAR_HEIGHT);
				int r = (int)(m*w.getY() + 255);
				
				g.setColor(new Color(255 - r, 0, 0));
			}
			
			g.setFont(f);
			g.drawString(s, w.getX(), (int)w.getY());
			
			if (player.length() > 0 && s.startsWith(player)) {
				String s2 = s.substring(0, player.length());
				
				g.setColor(W_C_COLOR);
				g.drawString(s2, w.getX(), (int)w.getY());
			}
			
		}
	}
	
	private void paintScore(Graphics g) {
		Font f = new Font(FONT, Font.BOLD, SCORE_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		String s = "" + score;
		
		g.setColor(Color.black);
		g.setFont(f);
		g.drawString(s, W_WIDTH - fm.stringWidth(s), (int)(fm.getHeight()*0.7));
	}
	
	private void paintPlayer(Graphics g) {
		Font f = new Font(FONT, Font.BOLD, PLAYER_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		g.setColor(Color.black);
		g.setFont(f);
		
		g.drawString(player, (W_WIDTH/2) - (fm.stringWidth(player)/2), W_HEIGHT - fm.getHeight());
		
		if(cubeBlink || !player.isEmpty()) {
			int x = W_WIDTH/2 - fm.stringWidth(player)/2 - 10;
			int y = W_HEIGHT - fm.getHeight()*2;
			int width = fm.stringWidth(player) + 20;
			int height = (int)(fm.getHeight()*1.5);
			
			g.drawRect(x, y, width, height);
		}
	}
	
	private void paintGameOver(Graphics g) {
		Font f = new Font(FONT, Font.BOLD, GAME_OVER_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		String gameover = "game over";
		String finalscore = "final score: " + score;
		String tryagain = "try \'again\'?";
		
		g.setColor(new Color(150, 150, 150));
		g.setFont(f);
		
		g.drawString(gameover, W_WIDTH/2 - fm.stringWidth(gameover)/2, W_HEIGHT/2 - fm.getHeight()*2);
		g.drawString(finalscore, W_WIDTH/2 - fm.stringWidth(finalscore)/2, W_HEIGHT/2);
		g.drawString(tryagain, W_WIDTH/2 - fm.stringWidth(tryagain)/2, W_HEIGHT/2 + fm.getHeight()*2);
	}
	
	
	private void paintGameStart(Graphics g) {
		Font f = new Font(FONT, Font.BOLD, GAME_START_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		String start = "type \'start\' to begin";
		
		g.setColor(new Color(150, 150, 150));
		g.setFont(f);
		
		g.drawString(start, W_WIDTH/2 - fm.stringWidth(start)/2, W_HEIGHT/2);
	}
	
	private void paintExplosions(Graphics g) {
		Graphics2D g2D = (Graphics2D)g;
		
		for(Ellipse2D e : explosions) {
			
			if (gameOver) {
				g.setColor(new Color(255, 0, 0));
				g2D.setStroke(new BasicStroke(90));
			} else {
				g.setColor(W_C_COLOR);
			}
			
			g.drawOval((int)e.getMinX(),
					(int)e.getMinY(),
					(int)e.getWidth(),
					(int)e.getHeight());
			
		}
	}
	
	//WORD COMPLETED CHECK
	private void wordCompletedCheck() {
		Iterator<Word> dupe = words.iterator();
		
		while (dupe.hasNext()) {
			Word w = dupe.next();
			
			if (w.equals(player) && w.isActive()) {
				score++;
				words.remove(w);
				player = "";
				
				Font f = new Font(FONT, Font.BOLD, WORD_SIZE);
				FontMetrics fm = getFontMetrics(f);
				
				Ellipse2D e = new Ellipse2D.Double();
				
				e.setFrame(w.getX() + fm.stringWidth(w.getWord())/2,
						w.getY() - fm.stringWidth(w.getWord())/4,
						0, 0);
				
				explosions.add(e);
				break;
			}
		}
	}
	
	//WORD LOST CHECK
	private void wordLostCheck() {
		Iterator<Word> dupe = words.iterator();
		
		while(dupe.hasNext()) {
			Word w = dupe.next();
			
			if(!w.isActive())
				break;
			
			if (w.getY() >= BAR_HEIGHT) {
				Font f = new Font(FONT, Font.BOLD, WORD_SIZE);
				FontMetrics fm = getFontMetrics(f);
				
				Ellipse2D e = new Ellipse2D.Double(
						w.getX() + fm.stringWidth(w.getWord())/2,
						BAR_HEIGHT, 0, 0);
				explosions.add(e);
				
				initGameOver();
				break;
			}
		}
	}
	
	//REFILL WORDS
	private void refillWords() {
		
		InputStream is = Field.class.getResourceAsStream(WORDS_FILE_NAME);
		Scanner fileScan = new Scanner(is);
		
		while(fileScan.hasNextLine()) {
			words.add(new Word(fileScan.nextLine().trim()));
		}
		
		fileScan.close();
		
	}
	
	//DEPLOY WORDS
	private void deployWord() {
		//Deploys a single word
		//Sets speeds and x pos randomly
		Font f = new Font(FONT, Font.BOLD, WORD_SIZE);
		FontMetrics fm = getFontMetrics(f);
		
		while (true) {
			Word w = words.get(RAND.nextInt(words.size())); //choose a random word
			
			if(w.isActive())
				continue;
			
			words.remove(w);
			
			w.setY(0);
			w.setX(RAND.nextInt(W_WIDTH - fm.stringWidth(w.getWord())));
			w.setSpeed((RAND.nextInt(MAX_S - MIN_S + 1) + MIN_S) * (score/75.0 + 1)); //add score for scale
			
			words.add(0, w);
			
			break;
		}
	}
	
	//GET NEXT DEPLOY TIME
	private long getNextDeployTime() {
		long tDiff = MAX_DEPLOY_DELAY + 1;
		double i = 0;
		double prob;
		
		double denom = Math.sqrt(Math.PI);
		
		while(tDiff > MAX_DEPLOY_DELAY) {
			prob = Math.pow(Math.E, -1*Math.pow((i-5), 2)) / denom;
			
			if(RAND.nextDouble() < prob)
				tDiff = (long)(i * MAX_DEPLOY_DELAY/10.0);
			
			if(i >= 10)
				i = 0;
			else
				i += 0.01;
		}
		
		return tDiff + System.currentTimeMillis();
	}
	
	//ANIMATION METHODS
	private void moveWords(long t) {
		for(Word w : words) {
			if(!w.isActive())
				continue;
			
			double newY = w.getY() + (w.getSpeed()*(t/1000.0));
			w.setY(newY);
		}
	}
	
	private void moveExplosions(long t) {
		Iterator<Ellipse2D> dupe = explosions.iterator();
		
		while(dupe.hasNext()) {
			Ellipse2D e = dupe.next();
			
			if (e.contains(0, 0, W_WIDTH, W_HEIGHT)) {
				explosions.remove(e);
				continue;
			}
				
			double x = e.getX() - t*0.6;
			double y = e.getY() - t*0.6;
			double w = e.getWidth() + t*1.2;
			double h = e.getHeight() + t*1.2;
				
			e.setFrame(x, y, w, h);
				
		}
	}
	
	private void animate(long t, long tPassed) {
		cubeBlink = (t % BLINK_S) == (t % (BLINK_S*2));
		moveWords(tPassed);
		moveExplosions(tPassed);
		repaint();
	}
	
	//MAIN RUN METHOD
	@Override
	public void run() {
		
		long time = 0, timeHold, nextDeploy = 0;
		
		while(true) {
			timeHold = time;
			time = System.currentTimeMillis();
			
			if(inGame) {
				
				if (time >= nextDeploy) {
					deployWord();
					nextDeploy = getNextDeployTime();
				}
				
				wordLostCheck();
			
			} else if((gameOver || gameStart) && words.isEmpty())
				initGame();
			
			wordCompletedCheck();
			
			animate(time, time - timeHold);
			repaint();
		}
		
	}
	
	//TYPER CLASS
	private class Typer implements KeyListener {
		
		@Override
		public void keyTyped(KeyEvent e) {
			
			char c = e.getKeyChar();
			
			if (c == 8 && player.length() > 0) {
				player = player.substring(0, player.length() - 1);
			} else if (c >= 'a' && c <= 'z') {
				player = player + c;
			}
			
			
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			
		}
		
		@Override
		public void keyReleased(KeyEvent e) {
			
		}
		
	}
}