package edu.mccc.cos210.sp2016.ds.midi;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.Instrument;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

public class MidiTest implements MetaEventListener, ControllerEventListener {
	private static final int META_EndofTrack = 47;
	private Sequencer sequencer;
	private Synthesizer synth;
	public static void main(String[] sa) throws Exception {
		String midiFile = "wwiwiwo.mid";
		if (sa.length == 1) {
			midiFile = sa[0];
		}
		new MidiTest().doIt(midiFile); 
	} 
	private void doIt(String midiFile) throws Exception {
		PrintWriter pw = new PrintWriter(new FileWriter("instruments.txt"));
		this.sequencer = MidiSystem.getSequencer(false);
		Sequence sequence = MidiSystem.getSequence(new File(midiFile));
		this.sequencer.setSequence(sequence);
		this.sequencer.setLoopCount(0);
		this.sequencer.addControllerEventListener(this, initControllers());
		this.sequencer.addMetaEventListener(this);
		this.synth = MidiSystem.getSynthesizer();
		this.synth.open();
		Instrument[] ia = synth.getLoadedInstruments();
		for (int i = 0; i < ia.length; i++) {
			pw.println(ia[i]);
		}
		pw.close();
		this.sequencer.getTransmitter().setReceiver(new MyReceiver(this.synth.getReceiver()));
		this.sequencer.open();
		this.sequencer.start();
	}
	private int[] initControllers() {
		int[] controllers = new int[128];
		for (int i = 0; i < controllers.length; i++) {
			controllers[i] = i;
		}
		return controllers;
	}
	public class MyReceiver implements Receiver {
		private Receiver receiver;
		public MyReceiver(Receiver receiver) {
			this.receiver = receiver;
		}
		@Override
		public void send(MidiMessage message, long timeStamp) {
			System.out.println(formatMidiMessage(message, timeStamp));
			this.receiver.send(message, timeStamp);
		}
		@Override
		public void close() {
			this.receiver.close();
		}
	}
	@Override
	public void meta(MetaMessage meta) {
		System.out.println("+ " + formatMetaMessage(meta));
		if (meta.getType() == META_EndofTrack) {
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
				// ignore
			} finally {
				this.synth.close();
				this.sequencer.close();
				System.exit(0);
			}
		}
	}
	@Override
	public void controlChange(ShortMessage event) {
		System.out.println("* " + formatShortMessage(event));
	}
	public static String formatMidiMessage(MidiMessage msg, long timeStamp) {
		StringBuilder sb = new StringBuilder();
		if (timeStamp != -1) {
			sb.append(formatTimeStamp(timeStamp));
			sb.append(" ");
		}
		byte[] message = msg.getMessage();
		for (int i = 0; i < message.length; i++) {
			sb.append(toHex(message[i]));
			sb.append(" ");
		}
		sb.append(getMessageType((byte) msg.getStatus(), message.length >= 2 ? message[1] : -1, message.length >= 3 ? message[2] : -1));
		return sb.toString();
	}
	private static String formatTimeStamp(long timeStamp) {
		long residual = timeStamp;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			residual = timeStamp >>> 8;
			sb.append(toHex((byte) (residual & 0x0ff)));
		}
		return sb.toString();
	}
	public static String formatMetaMessage(MetaMessage msg) {
		StringBuilder sb = new StringBuilder();
		sb.append(toHex(msg.getStatus()));
		sb.append(" ");
		sb.append(toHex(msg.getType()));
		byte[] data = msg.getData();
		for (int i = 0; i < data.length; i++) {
			sb.append(" ");
			sb.append(toHex(data[i]));
		}
		sb.append(" --");
		String type = metaTypeNames.get(msg.getType());
		if (type == null) {
			type = "Unknown";
		}
		sb.append(type);
		sb.append("--> ");
		for (byte b : data) {
			Character c = (char) b;
			if (" ~`\\!@#$%^&*()_-+={}[]|:;\"'<>,.?/".contains(c.toString())) {
				sb.append(c);
			} else {
				if (c > 255 || Character.isISOControl(c) || Character.isWhitespace(c)) {
					sb.append(".");
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}
	public static String formatShortMessage(ShortMessage msg) {
		StringBuilder sb = new StringBuilder();
		sb.append(toHex(msg.getStatus()));
		for (int i = 0; i < msg.getLength(); i++) {
			switch (i) {
				case 0:
					sb.append(" ");
					sb.append(toHex(msg.getData1()));
					break;
				case 1:
					sb.append(" ");
					sb.append(toHex(msg.getData2()));
					break;
				default:
					break;
			}
		}
		sb.append(" --> ");
		sb.append(controllerNumbers[msg.getData1()]);
		return sb.toString();
	}
	private static String toHex(int n) {
		byte[] ba = new byte[1];
		ba[0] = (byte) (n & 0x000000ff);
		String[] hex = {
			"0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "a", "b", "c", "d", "e", "f"
		};
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < ba.length; i++) {
			int left = (ba[i] & 0xff) / 16;
			int right = (ba[i] & 0xff) % 16;
			sb.append(hex[left] + hex[right]);
		}
		return sb.toString();
	}
	public static final String[] metaType = new String[] {
		"0", "Sequence Number",
		"1", "Text Event",
		"2", "Copyright Notice",
		"3", "Sequence/Track Name",
		"4", "Instrument Name",
		"5", "Lyric",
		"6", "Marker",
		"7", "Cue Point",
		"32", "MIDI Channel Prefix",
		"47", "End of Track",
		"81", "Set Tempo",
		"84", "SMPTE Offset",
		"88", "Time Signature",
		"89", "Key Signature",
		"127", "Sequencer-Specific Meta Event"
	};
	public static ArrayList<String> metaTypeNames = new ArrayList<String>(128);
	static {
		for (int i = 0; i < 128; i++) {
			metaTypeNames.add(i, null);
		}
		for (int i = 0; i < metaType.length; i += 2) {
			metaTypeNames.add(Integer.parseInt(metaType[i]), metaType[i + 1]);
		}
	}
	public static final String[] controllerNumbers = new String[] {
		"Bank Select",
		"Modulation Wheel/Lever",
		"Breath Controller",
		"Undefined",
		"Foot Controller",
		"Portamento Time",
		"Data Entry MSB",
		"Channel Volume",
		"Balance",
		"Undefined",
		"Pan",
		"Expression Controller",
		"Effect Control 1",
		"Effect Control 2",
		"Undefined",
		"Undefined",
		"General Purpose Controller 1",
		"General Purpose Controller 2",
		"General Purpose Controller 3",
		"General Purpose Controller 4",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Bank Select LSB",
		"Modulation Wheel/Lever LSB",
		"Breath Controller LSB",
		"Undefined",
		"Foot Controller LSB",
		"Portamento Time LSB",
		"Data Entry LSB",
		"Channel Volume LSB",
		"Balance LSB",
		"Undefined",
		"Pan LSB",
		"Expression Controller LSB",
		"Effect Control 1 LSB",
		"Effect Control 2 LSB",
		"Undefined",
		"Undefined",
		"General Purpose Controller 1 LSB",
		"General Purpose Controller 2 LSB",
		"General Purpose Controller 3 LSB",
		"General Purpose Controller 4 LSB",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Damper Pedal (Sustain)",
		"Portamento On/Off",
		"Sostenuto",
		"Soft Pedal",
		"Legato Footswitch",
		"Hold 2",
		"Sound Controller 1 (Sound Variation)",
		"Sound Controller 2 (Timbre/Harmonic Intensity)",
		"Sound Controller 3 (Release Time)",
		"Sound Controller 4 (Attack Time)",
		"Sound Controller 5 (Brightness)",
		"Sound Controller 6 (No Default)",
		"Sound Controller 7 (No Default)",
		"Sound Controller 8 (No Default)",
		"Sound Controller 9 (No Default)",
		"Sound Controller 10 (No Default)",
		"General Purpose Controller 5",
		"General Purpose Controller 6",
		"General Purpose Controller 7",
		"General Purpose Controller 8",
		"Portamento Control",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Effects 1 Depth",
		"Effects 2 Depth",
		"Effects 3 Depth",
		"Effects 4 Depth",
		"Effects 5 Depth",
		"Data Increment",
		"Data Decrement",
		"Non-Registered Parameter Number LSB",
		"Non-Registered Parameter Number MSB",
		"Registered Parameter Number LSB",
		"Registered Parameter Number MSB",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Undefined",
		"Channel Mode - All Sound Off",
		"Channel Mode - Reset All Controllers",
		"Channel Mode - Local Control",
		"Channel Mode - All Notes Off",
		"Channel Mode - Omni Mode Off",
		"Channel Mode - Omni Mode On",
		"Channel Mode - Mono Mode",
		"Channel Mode - Poly Mode On"
	};
	public static String getMessageType(byte status, byte byte1, byte byte2) {
		StringBuilder sb = new StringBuilder();
		byte mask = (byte) (status & 0xf0);
		switch (mask) {
			case (byte) 0x80:
				sb.append("--> Note Off");
				break;
			case (byte) 0x90:
				if (byte2 == 0) {
					sb.append("--> Note Off");
				} else {
					sb.append("--> Note On");
				}
				break;
			case (byte) 0xa0:
				sb.append("--> Polyphonic Key Pressure/Aftertouch");
				break;
			case (byte) 0xb0:
				sb.append("--Control Change--> ");
				sb.append(controllerNumbers[byte1]);
				break;
			case (byte) 0xc0:
				sb.append("--> Program Change");
				break;
			case (byte) 0xd0:
				sb.append("--> Channel Pressure/Aftertouch");
				break;
			case (byte) 0xe0:
				sb.append("--> Pitch Bend Change");
				break;
			case (byte) 0xf0:
				sb.append("--> System Exclusive");
				break;
			default:
				sb.append("");
				break;
		}
		return sb.toString();
	}
}
