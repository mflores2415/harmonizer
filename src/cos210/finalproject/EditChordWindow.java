package cos210.finalproject;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EditChordWindow  { 
		
		private String currentChord;
		private String oldChords;
		private Song song;
		private long currentTick;
		private long prevTick;
		private JComboBox<String> listRoot;
		private JComboBox<String> listType;
		private String currentRoot;
		private String currentType;
		private JFrame jf;
		private String prevChord;
		private static final String[] noteNamesSharp = {
				"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
		};
		private static final String[] noteNamesFlat = {
				"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
		};
		public EditChordWindow(long currentTick, Song song, String oldChords) {
			this.song = song;
			this.currentTick = currentTick;
			this.prevTick = this.song.getPrevTick(currentTick);
			this.prevChord = null;
			if (prevTick != -1) this.prevChord = this.song.getMap().get(this.prevTick).getName();
			if(currentTick == 0)  this.currentTick = this.song.getFirstTick();
			this.currentChord = this.song.getMap().get(this.currentTick).getName();
			this.oldChords = oldChords;
			this.currentRoot = this.currentChord.split("m")[0];
			this.currentType = this.currentChord.substring(this.currentRoot.length(), this.currentChord.length());
			drawWindow();
			
		}
		@SuppressWarnings("serial")
		public void drawWindow() {
			jf = new JFrame("Edit Chord WIndow");
			JPanel panelMain = new JPanel();
			panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));
			jf.add(panelMain);
			jf.setSize(400, 200);
			jf.setVisible(true);
			jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			JPanel jp0 = new JPanel(new FlowLayout());
			JLabel jb1 = new JLabel("Current Chord: ");
			JLabel jb = new JLabel(this.currentChord);
			
			jp0.add(jb1);
			jp0.add(jb);		

			
			JPanel jp2 = new JPanel();
			listRoot = new JComboBox<String>(this.song.isFlatKey() ? noteNamesFlat : noteNamesSharp);
			listRoot.setSelectedItem(this.currentRoot);
			JLabel labelRoot = new JLabel("Root:");
			String[] listTypes = new String[] {"maj", "min", "maj(m7)", "maj(M7)", "min(m7)", "min(M7)"};
			listType = new JComboBox<String>(listTypes);
			listType.setSelectedItem(this.currentType);
			JLabel labelType = new JLabel("Type:");
			
			jp2.add(labelRoot);
			jp2.add(listRoot);
			jp2.add(labelType);
			jp2.add(listType);
		
			
			JPanel jp4 = new JPanel();
			JButton accept = new JButton("Accept");
			accept.setAction(new AbstractAction("Accept") {
				public void actionPerformed(ActionEvent ae) {
					editChords();
				}
			});
			jp4.add(accept);
			panelMain.add(jp0);
			panelMain.add(jp2);
			panelMain.add(jp4);
			jf.setVisible(true);
		}
	
		public void editChords() {
			String oldChords = this.oldChords;
			String newChords = (String) this.listRoot.getSelectedItem() + this.listType.getSelectedItem() + " " + oldChords.split(" ")[1];
			String prevChords = this.prevChord + " " + this.listRoot.getSelectedItem() + this.listType.getSelectedItem();
			this.song.editChord(currentTick, newChords, oldChords, prevTick, this.prevChord + " " + currentChord, prevChords);
			jf.dispose();
		}
	}

