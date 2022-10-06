package cos210.finalproject;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class FileInfoDisplay {
	private JLabel fileInfo;
	private JPanel jp;
	public FileInfoDisplay () {
		this.fileInfo = new JLabel();
		this.jp = new JPanel();
		this.jp.add(fileInfo);
	}
	
	public void set(Song song) {
		String info = "<html>";
		info += "<p>File Name: " + song.getFileName() + "</p>";
		info += "<p>Song Name: " + song.getName() + "</p>";
 		info += "<p>Time Signature: " + song.getTimeSignature() + "</p>";
		info += "<p>Key: " + song.getKey() + "</p>";
		if (song.getTempo() != 0) {
			info += "<p>Temp: " +song.getTempo() + "</p>";
		}
		info += "</html>";			
		this.fileInfo.setText(info);
	}
	
	
	public JPanel get() {
		return this.jp;
	}
	
}
