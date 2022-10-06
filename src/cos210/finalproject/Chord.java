package cos210.finalproject;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class Chord {
	private String name = null;
	private String type = null;
	private int root = 0;
	private Set<Note> notes = new HashSet<Note>();
	private boolean flatKey;
	private static final int[] noteValues = {
			1,2,4,8,16,32,64,128,256,512,1024,2048
	};
	private static final String[] noteNamesSharp = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};
	private static final String[] noteNamesFlat = {
			"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
	};
	public Chord(Set<Note> notes, boolean flatKey) {
		this.notes = notes;
		this.flatKey = flatKey;
		analyze();
	}
	private void analyze() {
		int noteTotal = 0;
		for (Note note : this.notes) {
			noteTotal += noteValues[note.getRelativePitch()];
		}
		switch (noteTotal) {
		// C root chords
			case 137: case 141: case 169: case 139:
				this.root = 0;
				this.type = "min";
				break;
			case 145: case 147: case 149: case 177: case 401:
				this.root = 0;
				this.type = "maj";
				break;
			case 1169: case 1168: case 1680: case 1153: case 1041:
				this.root = 0;
				this.type = "maj(m7)";
				break;
			case 2193: case 2065: case 2177: 
				this.root = 0;
				this.type = "maj(M7)";
				break;
			case 2185: case 2184: case 2057:
				this.root = 0;
				this.type = "min(M7)";
				break;
			case 1161: case 1033:
				this.root = 0;
				this.type = "min(m7)";
				break;
		// C#/Db root chords
			case 290: case 298: case 354:
				this.root = 1;
				this.type = "maj";
				break;
			case 274: case 282: case 338: case 1298:
				this.root = 1;
				this.type = "min";
				break;
			case 2338: case 2346: case 2336: case 2082: 
				this.root = 1;
				this.type = "maj(m7)";
				break;
			case 291: case 259: case 35: case 299:
				this.root = 1;
				this.type = "maj(M7)";
				break;
			case 2322: case 2330:
				this.root = 1;
				this.type = "min(m7)";
				break;
			case 275: case 283: case 273:
				this.root = 1;
				this.type = "min(M7)";
				break;
		// D root chords
			case 580: case 596: case 708: case 588:
				this.root = 2;
				this.type = "maj";
				break;
			case 548: case 564: case 676: case 684:
				this.root = 2;
				this.type = "min";
				break;
			case 581: case 577: case 517:
				this.root = 2;
				this.type = "maj(m7)";
				break;
			case 582: case 598: case 710:
				this.root = 2;
				this.type = "maj(M7)";
				break;
			case 549: case 565: case 677:
				this.root = 2;
				this.type = "min(m7)";
				break;
			case 550: case 566: case 678:
				this.root = 2;
				this.type = "min(M7)";
		// D#/Eb root chords
			case 1160: case 1192: case 1416:
				this.root = 3;
				this.type = "maj";
				break;
			case 1096: case 1352: case 1128:
				this.root = 3;
				this.type = "min";
				break;
			case 1162: case 1154: case 1194: case 1418:
				this.root = 3;
				this.type = "maj(m7)";
				break;
			case 1164: case 1196: case 1420:
				this.root = 3;
				this.type = "maj(M7)";
				break;
			case 1098: case 1130: 
				this.root = 3;
				this.type = "min(m7)";
				break;
			case 1100: case 1092:
				this.root = 3;
				this.type = "min(M7)";
				break;
		// E root chords
			case 2320: case 2384: case 2832:
				this.root = 4;
				this.type = "maj";
				break;
			case 2192: case 2256: case 2704:
				this.root = 4;
				this.type = "min";
				break;
			case 2324: 
				this.root = 4;
				this.type = "maj(m7)";
				break;
			case 2328:
				this.root = 4;
				this.type = "maj(M7)";
				break;
			case 2196:
				this.root = 4;
				this.type = "min(m7)";
				break;
			case 2200: case 2264:
				this.root = 4;
				this.type = "min(M7)";
		// F root chords
			case 545: case 673: case 609:
				this.root = 5;
				this.type = "maj";
				break;
			case 289:
				this.root = 5;
				this.type = "min";
				break;
			case 553: case 552:
				this.root = 5;
				this.type = "maj(m7)";
				break;
			case 561:
				this.root = 5;
				this.type = "maj(M7)";
				break;
			case 305:
				this.root = 5;
				this.type = "min(M7)";
				break;
			case 297:
				this.root = 5;
				this.type = "min(m7)";
				break;
				
		// F#/Gb root chords
			case 1090: case 1346:
				this.root = 6;
				this.type = "maj";
				break;
			case 578:
				this.root = 6;
				this.type = "min";
				break;
			case 1106:
				this.root = 6;
				this.type = "maj(m7)";
				break;
			case 1122:
				this.root = 6;
				this.type = "maj(M7)";
				break;
			case 610:
				this.root = 6;
				this.type = "min(M7)";
				break;
			case 594:
				this.root = 6;
				this.type = "min(m7)";
		// G root chords
			case 2180:
				this.root = 7;
				this.type = "maj";
				break;
			case 1156:
				this.root = 7;
				this.type = "min";
				break;
			case 2212:
				this.root = 7;
				this.type = "maj(m7)";
				break;
			case 2244:
				this.root = 7;
				this.type = "maj(M7)";
				break;
			case 1120:
				this.root = 7;
				this.type = "min(M7)";
				break;
			case 1188:
				this.root = 7;
				this.type = "min(m7)";
				break;
		// G#/Ab root chords
			case 265:
				this.root = 8;
				this.type = "maj";
				break;
			case 2312:
				this.root = 8;
				this.type = "min";
				break;
			case 329:
				this.root = 8;
				this.type = "maj(m7)";
				break;
			case 393:
				this.root = 8;
				this.type = "maj(M7)";
				break;
			case 2440:
				this.root = 8;
				this.type = "min(M7)";
				break;
			case 2376:
				this.root = 8;
				this.type = "min(m7)";
				break;
		// A root chords
			case 530:
				this.root = 9;
				this.type = "maj";
				break;
			case 529:
				this.root = 9;
				this.type = "min";
				break;
			case 658:
				this.root = 9;
				this.type = "maj(m7)";
				break;
			case 786:
				this.root = 9;
				this.type = "maj(M7)";
				break;
			case 785:
				this.root = 9;
				this.type = "min(M7)";
				break;
			case 657:
				this.root = 9;
				this.type = "min(m7)";
				break;
		// A#/Bb root chords
			case 1060:
				this.root = 10;
				this.type = "maj";
				break;
			case 1058:
				this.root = 10;
				this.type = "min";
				break;
			case 1316:
				this.root = 10;
				this.type = "maj(m7)";
				break;
			case  1572:
				this.root = 10;
				this.type = "maj(M7)";
				break;
			case 1570:
				this.root = 10;
				this.type = "min(M7)";
				break;
			case 1314:
				this.root = 10;
				this.type = "min(m7)";
				break;
		// B root chords
			case 2120:
				this.root = 11;
				this.type = "maj";
				break;
			case 2116:
				this.root = 11;
				this.type = "min";
				break;
			case 2632:
				this.root = 11;
				this.type = "maj(m7)";
				break;
			case 3144:
				this.root = 11;
				this.type = "maj(M7)";
				break;
			case 3140:
				this.root = 11;
				this.type = "min(M7)";
				break;
			case 2628:
				this.root = 11;
				this.type = "min(m7)";
				break;
		// Error Msg
			default:
				this.root = 4;
				this.type = "rr";
				break;
		}		
		this.name = toString();

	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return (this.flatKey ? noteNamesFlat[this.root] + this.type : noteNamesSharp[this.root] + this.type);
	}
	@Override
	public boolean equals(Object obj) {
		return obj.toString().equals(this.toString());
	}
	@Override
	public int hashCode() {	
		return (int) new BigInteger(this.toString().getBytes()).intValue();
	}
	
	
	
}
