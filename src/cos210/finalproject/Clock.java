package cos210.finalproject;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Clock {
	private Timer timer;
	private JProgressBar progressBar;
	private MusicPlayer musicPlayer;
	public String s;
	private Runnable runnable;
	JLabel time;

	public Clock() {
		time = new JLabel("00:00");
		time.setFont(new Font("Courier New",1,20));		
		progressBar = new JProgressBar(0, 100);
	}
	
	public JLabel get() {
		return this.time;
	}
	
	public void setTime(String time) {
		this.time.setText(time);
	}
	public void createClock() {
		MusicPlayer mp = this.musicPlayer;
		progressBar.setMaximum((int)mp.getMSLength());
		progressBar.setMinimum(0);
		progressBar.setValue(0);
		progressBar.setString("00:00");
		progressBar.setStringPainted(true);
		runnable = new Runnable() {
			public void run() {
				timer = new Timer(50, new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						progressBar.setValue((int) mp.getMSPosition());
						s = (String.format("%02d:%02d", 
							    TimeUnit.MICROSECONDS.toMinutes(mp.getMSPosition()),
							    TimeUnit.MICROSECONDS.toSeconds(mp.getMSPosition()) - 
							    TimeUnit.MINUTES.toSeconds(TimeUnit.MICROSECONDS.toMinutes(mp.getMSPosition()))));
						progressBar.setString(s);
						progressBar.setStringPainted(true);
						progressBar.repaint();
						if (mp.getMSPosition() == mp.getMSLength()) {
							timer.stop();
							mp.isStopped(true);
							progressBar.setValue(0);
						}
					}	
			});
		}
	};
	
	SwingUtilities.invokeLater(runnable);
	}
	void updateProgress(final int newValue) {
        progressBar.setValue(newValue);
    }

    public void setValue(final int j) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateProgress(j);
            }
        });
    }

    public Runnable run() {
    	return runnable;
    }
	public JProgressBar getProgressBar() {
		return this.progressBar;
	}
	public void play() {
		timer.start();
	}
	public void pause() {
		timer.stop();
	}
	public void replay() {
		timer.restart();
	}
	public void setMusicPlayer(MusicPlayer mp) {
		this.musicPlayer = mp;
	}

}
