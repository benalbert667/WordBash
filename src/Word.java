public class Word {
	
	private String s;
	private int x;
	private double y;
	private double speed; //in pixels per second
	
	public Word(String s) {
		this.s = formatWord(s);
		x = -1;
		y = -1;
		speed = 0;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Word) {
			Word w = (Word) o;
			return w.getWord().equals(this.getWord());
			
		} else if (o instanceof String) {
			String s = (String) o;
			return s.equals(this.getWord());
		}
		
		return false;
	}
	
	public void setX(int x) { this.x = x; }
	public void setY(double y) { this.y = y; }
	public void setSpeed(double speed) { this.speed = speed; }
	
	public int getX() { return x; }
	public double getY() { return y; }
	public double getSpeed() { return speed; }
	public String getWord() { return s; }
	
	public boolean isActive() { return !(x == -1 && y == -1); }
	
	private String formatWord(String s) {
		String returnString = "";
		
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			
			if ((c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z'))
				returnString += c;
		}
		
		return returnString.toLowerCase();
	}
	
}
