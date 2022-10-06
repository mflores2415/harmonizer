package edu.mccc.cos210.sp2016.ds.midi;

import java.io.File;
import java.util.Arrays;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public class MidiReader {
	private static String MIDI_FILE = "./encourage.mid";
	//private static String MIDI_FILE = "./wwiwiwo.mid";
	//private static String MIDI_FILE = "./march.mid";
	//private static String MIDI_FILE = "./fg.mid";
	//private static String MIDI_FILE = "./drums.mid";
	public static void main(String[] sa) throws Exception {
		new MidiReader().doIt();
	}
	private void doIt() throws Exception {
		Sequence sequence = MidiSystem.getSequence(new File(MIDI_FILE));
		float divisionType = sequence.getDivisionType();
		if (divisionType != Sequence.PPQ) {
			System.err.println("PPQ is only supported Format");
			System.exit(-1);
		}
		int resolution = sequence.getResolution();
		long tickLength = sequence.getTickLength();
		System.out.println(resolution + " " + tickLength);
		Track[] tracks = sequence.getTracks();
		for (Track track : tracks) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent me = track.get(i);
				long tick = me.getTick();
				MidiMessage mm = me.getMessage();
				int status = mm.getStatus();
				byte[] ba = mm.getMessage();
				System.out.printf("%d %d %s\n", tick, status, Arrays.toString(ba));
			}
		}
	}
}

//
// Note ON  = 1001nnnn 0kkkkkkkk 0vvvvvvv  ;  vvvvvvv != 0
// Note OFF = 1000nnnn 0kkkkkkkk 0vvvvvvv  ; 		            128 - 143
//          = 1001nnnn 0kkkkkkkk 0vvvvvvv  ;  vvvvvvv == 0		144 - 159
//     nnnn = channel; 1001 is DRUMS
//     kkkkkkk = note
//     vvvvvvv = velocity
//
// End-of-Track = FF 2f 00
//
// Time Signature = FF 58 04 nn dd cc bb
//     nn = numerator, dd = denominator (negative power of two)
//
// Key Signature = FF 59 02 sf mi
//     sf = 0; key of C
//         +1; 1 sharp
//         -1; 1 flat
//         etc.
//     mi = 0; major key
//          1; minor key
//

