package Tools;

public class Block {

	private String name;

	private long offset;

	public Block(String s, int i) {
		this.name = s;
		this.offset = i;
	}

	public String getName() {
		return name;
	}

	public long getOffset() {
		return offset;
	}
}
