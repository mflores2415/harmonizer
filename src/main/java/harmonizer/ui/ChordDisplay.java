package harmonizer.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ChordDisplay {
	private final JPanel chordDisplay;
	private final JLabel chordNext;
	private final JLabel chordCurrent;
	private volatile String current = "—";
	private volatile String next = "—";
	private volatile long currentTick;
	public ChordDisplay() {
		chordCurrent = createChordLabel("—", true);
		chordNext = createChordLabel("—", false);
		this.chordDisplay = new JPanel();
		chordDisplay.setLayout(new GridLayout(1, 2, 16, 0));
		chordDisplay.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));
		chordDisplay.add(createChordCard("Current chord", chordCurrent, true));
		chordDisplay.add(createChordCard("Next chord", chordNext, false));
	}

	private JLabel createChordLabel(String text, boolean current) {
		JLabel label = new JLabel(text, JLabel.CENTER);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, current ? 34 : 28));
		label.setForeground(current ? new Color(28, 54, 93) : new Color(82, 91, 105));
		label.setBorder(BorderFactory.createEmptyBorder(16, 12, 20, 12));
		return label;
	}

	private JPanel createChordCard(String title, JLabel chord, boolean current) {
		JPanel card = new JPanel(new java.awt.BorderLayout());
		card.setBackground(current ? new Color(239, 245, 253) : new Color(246, 247, 249));
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(218, 224, 232)),
				BorderFactory.createEmptyBorder(8, 12, 4, 12)));
		JLabel heading = new JLabel(title, JLabel.CENTER);
		heading.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		heading.setForeground(new Color(90, 99, 112));
		card.add(heading, java.awt.BorderLayout.NORTH);
		card.add(chord, java.awt.BorderLayout.CENTER);
		return card;
	}
	
	public JPanel getDisplay() {
		return chordDisplay;
	}
	public String getChords() {
		return current + " " + next;
	}
	public void set(String chords) {
		String[] pair = chords.split(" ", 2);
		current = pair[0];
		next = pair.length > 1 ? pair[1] : "";
		if (SwingUtilities.isEventDispatchThread()) {
			update();
		} else {
			SwingUtilities.invokeLater(this::update);
		}
	}
	
	public void setTick(long tick) {
		this.currentTick = tick;
	}
	
	public long getTick() {
		return currentTick;
	}
	private void update() {
		this.chordCurrent.setText(current);
		this.chordNext.setText(next);
	}
}
