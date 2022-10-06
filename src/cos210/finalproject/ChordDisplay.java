package cos210.finalproject;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class ChordDisplay {
	JPanel chordDisplay;
	JPanel chordLabels;
	JLabel chordNext;
	JLabel chordCurrent;
	String current;
	String next;
	Long currentTick = 0L;
	public ChordDisplay() {
		
		this.chordCurrent = new JLabel("Ha");
		this.chordCurrent.setFont(new Font("Courier New", 1, 24 ));
		this.chordCurrent.setBackground(new Color(200,200,200));
		this.chordCurrent.setOpaque(true);
		Border padding = BorderFactory.createEmptyBorder(20,20,20,20);
		Border line = BorderFactory.createLineBorder(Color.BLACK);
		this.chordCurrent.setBorder(BorderFactory.createCompoundBorder(line,padding));
		
		this.chordNext = new JLabel("rm");
		this.chordNext.setFont(new Font("Courier New", 1, 24 ));
		this.chordCurrent.setBackground(new Color(200,200,200));
		this.chordNext.setOpaque(true);
		this.chordNext.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),padding));
		this.chordDisplay = new JPanel();
		this.chordDisplay.add(this.chordCurrent);
		this.chordDisplay.add(this.chordNext);
	}
	
	public JPanel getDisplay() {
		return chordDisplay;
	}
	public String getChords() {
		return this.current+ " " + this.next;
	}
	public void set(String chords) {
		String[] tmp = chords.split(" ");
		this.current = tmp[0];
		this.next = tmp[1];
		update();
	}
	
	public void setTick(long tick) {
		this.currentTick = tick;
	}
	
	public long getTick() {
		return this.currentTick;
		
	}
	public void update() {
		this.chordCurrent.setText(current);
		this.chordNext.setText(next);
	}
}
