package cos210.finalproject;


public class Note {
	private String name = null;
	private int pitch = 60;
	private int relativePitch = 0;
	
	public Note(int pitch) {
		this.pitch = pitch;
		this.setRelativePitch(this.pitch);
	}
	public String getName() {
		return this.name;
	}
	public int getPitch() {
		return this.pitch;
	}
	
	public int getRelativePitch() {
		return this.relativePitch;
	}
	public int setRelativePitch(int pitch) {
		this.relativePitch = pitch % 12;
		
		return this.relativePitch;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj.toString().equals(this.toString());
	}

	@Override
	public int hashCode() {
		return this.relativePitch;
	}
 }
