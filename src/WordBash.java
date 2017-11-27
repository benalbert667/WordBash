import java.awt.EventQueue;

import javax.swing.JFrame;

public class WordBash extends JFrame {

	public WordBash() {
		add(new Field());
        
        setResizable(false);
        pack();
        
        setTitle("WordBash");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {                
                JFrame ex = new WordBash();
                ex.setVisible(true);                
            }
        });

	}

}
