package cos210.finalproject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class UniqueChordWindow {
	private final int NUM_COL = 5;
	public UniqueChordWindow(Song s) {
		this(s.getChords(), s.getName());
	}
	public UniqueChordWindow(Set<Chord> cs) {
		this(cs, "Chords");
	}
	public UniqueChordWindow(Set<Chord> cs, String title) {
		JFrame jf = new JFrame(title);
		JPanel jp = new JPanel();
		jf.add(jp);
		BoxLayout bl = new BoxLayout(jp, BoxLayout.PAGE_AXIS);
		jp.setLayout(bl);
		this.putChords(jp, cs);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.pack();
		jf.setResizable(false);
		jf.setVisible(true);
	}
	public void putChords(JPanel jp, Set<Chord> cs) {//method to override to change display
		JLabel jl;
		JPanel jpjr[] = new JPanel[(cs.size() + NUM_COL) / NUM_COL];
		FlowLayout fl = new FlowLayout();
		int i = 0;
		int j = 0;
		for(Chord chord : cs) {
			if (i % NUM_COL == 0) {
				if (i != 0) {
					jp.add(jpjr[j]);
					j++;
				}
				jpjr[j] = new JPanel();
				jpjr[j].setLayout(fl);
			}
			jl = new JLabel(chord.getName());
			jl.setFont(new Font("Courier New", 1, 24));
			jl.setSize(new Dimension(100, 100));
			if (i % 2 == 0) {
				jl.setBackground(new Color(200,200,200));
			}
			jl.setOpaque(true);
			Border padding = BorderFactory.createEmptyBorder(20,20,20,20);
			Border line = BorderFactory.createLineBorder(Color.BLACK);
			jl.setBorder(BorderFactory.createCompoundBorder(line,padding));
			jpjr[j].add(jl);
			i++;
		}
		jp.add(jpjr[j]);
	}
}
