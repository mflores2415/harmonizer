package cos210.finalproject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public class Song{
	private Map<Long, Chord> chords = new TreeMap<Long, Chord>();
	private int length = 0;
	private String fileName = null;
	private String fileDir = null;
	private Sequence sequence = null;
	private String name = null;
	private int tempo = 0;
	private String timeSignature = null;
	private String keySignature = "Unknown";
	private Track track = null;
	private ArrayList<Long> ticks = new ArrayList<Long>();
	private ChordDisplay cd = null;
	private boolean isAnalyzed = false;
	private boolean keyFlat;
	public Song(String fileName, String fileDir, ChordDisplay cd) throws Exception {
		this.fileName = fileName;
		this.fileDir = fileDir;
		importMidi(this.fileName, this.fileDir);
		this.cd = cd;
	}
	
	public String getName() {
		return this.name;
	}
	public String getTimeSignature() {
		return this.timeSignature;
	}
	public int getTempo() {
		return this.tempo;
	}
	public String getKey() {
		return this.keySignature;
	}
	public void setKey(String key) {
		this.keySignature = key;
	}
	public int getLength() {
		return this.length;
	}
	public String getFileName() {
		return this.fileName;
	}

	public void editChord(long tick, String newChords, String oldChords, long prevTick, String oldPrevChords, String prevChords) {
		chords.get(tick).setName(name);
		this.track.remove(new MidiEvent(makeMessageSingle(5, oldChords), tick));
		this.track.add(new MidiEvent(makeMessageSingle(5, newChords), tick));
		if(prevTick != -1) {
			this.track.remove(new MidiEvent(makeMessageSingle(5, oldPrevChords), prevTick));
			this.track.add(new MidiEvent(makeMessageSingle(5, prevChords), prevTick));
		}
		this.cd.set(newChords);

		
	}
	public Set<Chord> getChords() {
		Set<Chord> uniqueChords = new HashSet<Chord>();		
		Collection<Chord> allChords = chords.values();
		uniqueChords.addAll(allChords);
		return uniqueChords;
	}
	
	public void importMidi(String filename, String filedir) throws Exception {
		File file = new File(this.fileDir + this.fileName);
		this.sequence = MidiSystem.getSequence(file);
		analyzeMidi();

	}
	
	private void analyzeMidi() {
		Track[] tracks = this.sequence.getTracks();
		this.track = tracks[tracks.length - 1];
		Set<Note> currentNotes = new HashSet<Note>();
		long prevTick = -1;
		Chord prevChord = null;
		Chord currentChord = null;
		for (Track track : tracks) {
			for (int i = 0; i < track.size(); i++) {				
				MidiEvent event = track.get(i);
				long tick = event.getTick();
				MidiMessage msg = event.getMessage();
				byte[] mess= msg.getMessage(); 
				byte mask = (byte) (msg.getStatus() & 0xf0);
				byte byte2 =  mess.length >= 3 ? mess[2] : -1;
				if (msg instanceof MetaMessage) {
					MetaMessage metaMsg = (MetaMessage) msg;
					byte[] metaData = metaMsg.getData();
					switch (metaMsg.getType()) {
						case 3: 
						try {
							if (this.name == null || this.name == "Drums" || this.name == "Piano" || this.name == "Piano Template") {
								String name = new String(metaData, "UTF-8");
								if (name != "Drums" || name != "Piano" || name != "Piano Template") {
									this.name = name;
								}
							}
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
							break;
						case 1:
							try {
								if (new String(metaData, "UTF-8").equals("Harm Analyzed")) {
									this.isAnalyzed = true;
								} 
							} catch (UnsupportedEncodingException e) {
								System.out.println("fail");
							}
							break;
						case 88:
							this.timeSignature = new Integer(metaData[0]).toString() + " / " + new Integer((int) Math.pow(2, metaData[1])).toString();
							break;
						case 89:
							switch (metaData[0]) {
								case -7:
									this.keySignature = (metaData[1] == 0 ? "Cb" : "Ab");
									break;
								case -6:
									this.keySignature = (metaData[1] == 0 ? "Gb" : "Eb");
									break;
								case -5:
									this.keySignature = (metaData[1] == 0 ? "Db" : "Bb");
									break;
								case -4:
									this.keySignature = (metaData[1] == 0 ? "Ab": "F");
									break;
								case -3:
									this.keySignature = (metaData[1] == 0 ? "Eb" : "C");
									break;
								case -2:
									this.keySignature = (metaData[1] == 0 ? "Bb" : "G");
									break;
								case -1:
									this.keySignature = (metaData[1] == 0 ? "F" : "D");
									break;
								case 0:
									this.keySignature = (metaData[1] == 0 ? "C" : "A");
									break;
								case 1:
									this.keySignature = (metaData[1] == 0 ? "G": "E");
									break;
								case 2:
									this.keySignature = (metaData[1] == 0 ? "D" : "B");
									break;
								case 3:
									this.keySignature = (metaData[1] == 0 ? "A" : "F#");
									break;
								case 4:
									this.keySignature = (metaData[1] == 0 ? "E" : "C#");
									break;
								case 5:
									this.keySignature = (metaData[1] == 0 ? "B" : "G#");
									break;
								case 6:
									this.keySignature = (metaData[1] == 0 ? "F#" : "D#");
									break;
								case 7:
									this.keySignature = (metaData[1] == 0 ? "C#" : "A#");
									break;
								default:
									this.keySignature = "Unknown";
									break;
							}
							this.keySignature += (metaData[1] == 0 ? " major" : " minor");
							this.keyFlat = (metaData[0] < 0 ? true : false);					
					}
				}
				
				if (mask == (byte) 0x80) {	
					if (currentNotes.size() >= 3) {
						currentChord = new Chord(currentNotes, isFlatKey());
						if (!currentChord.getName().equals("Err")) {
							chords.put(tick, currentChord);
						}
					}
					currentNotes = new HashSet<Note>();
				}
				if (mask == (byte) 0x90 && msg.getStatus() != 153) {
					if(byte2 != 0) {
						if(mess[1] >= 0) {
							currentNotes.add(new Note((int) mess[1]));
						}
					} else {
						if (currentNotes.size() >= 3) {
							currentChord = new Chord(currentNotes, isFlatKey());
							if (!currentChord.getName().equals("Err")) {
								chords.put(tick, currentChord);
							}
							currentNotes = new HashSet<Note>();
						}
					}
				}
			}
			if (!this.isAnalyzed) {
				track.add(
						new MidiEvent(
							makeMessage(1, "Harm", "Analyzed"),
							0
						)
				);	
			}
		}
		int r = 0;
		for(Map.Entry<Long, Chord> entry : chords.entrySet()) {
			currentChord = entry.getValue();
			Long tick = entry.getKey();
			if(r == 0) {
				this.ticks.add(tick);
			}

			if (!currentChord.getName().equals("Err") && prevChord != null && !currentChord.equals(prevChord)) {
				this.ticks.add(tick);
			}
			r++;
			prevChord = currentChord;
		}
		if(!this.isAnalyzed) {
			prevChord = null;
			Long currentTick = 0L;
			prevTick = -1;
			int count = 0;
			for (Map.Entry<Long, Chord>  entry: chords.entrySet()) {
				count++;
				currentChord = entry.getValue();
				currentTick = entry.getKey();
				if (!currentChord.getName().equals("Err")) {
					if (count == 2) {
						prevTick = currentTick;
					}
					if (prevChord != null && !currentChord.equals(prevChord)) {
						
						if (prevTick != -1) {
							tracks[tracks.length - 1].add(
								new MidiEvent(
									makeMessage(5, prevChord.getName(), currentChord.getName()),
									prevTick
								)
							);
						}
						prevTick = entry.getKey();
					}
				}		
				prevChord = currentChord;
			}
			try {
				if (currentChord != null) {
					byte[] b = (currentChord.getName() + " END").getBytes();
					
					tracks[tracks.length - 1].add(
							new MidiEvent ( 
									new MetaMessage(5, b, b.length  ), 
									currentTick));
					ticks.add(currentTick);
				}
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}		
		}
	}
	public Sequence getSequence() {
		return this.sequence;
	}
	private MidiMessage makeMessage(int type, String first, String next){
		String s = first + " " + next;
		byte[] b = s.getBytes();
		try {
			return new MetaMessage(type, b, b.length);
		} catch (InvalidMidiDataException e) {
			return null;
		}
	}
	
	private MidiMessage makeMessageSingle(int type, String s) {
		byte[] b = s.getBytes();
		try {
			return new MetaMessage(type, b, b.length);
		} catch (InvalidMidiDataException e) {
			return null;
		}		
	}
	
	public long getFirstTick() {
		return this.ticks.get(0);
	}
	public long getPrevTick(long currentTick) {
		int index = this.ticks.indexOf(currentTick);
		try {
		return this.ticks.get(index - 1);
		} catch (Exception e){
			return -1L;
			
		}
	}
	
	public long getNextTick(long currentTick) {
		int index = this.ticks.indexOf(currentTick);
		try {
		return this.ticks.get(index + 1);
		} catch (Exception e){
			return -1L;
			
		}
		
	}
	
	
	public boolean isFlatKey() {
		return this.keyFlat;
	}
	public Map<Long, Chord> getMap() {
		return this.chords;
	}
}
