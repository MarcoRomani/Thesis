package general;

public class Switch extends Node {

	public static int switch_id = 100000;
	private int id;
	
	public Switch() {
		this.id = switch_id;
		switch_id += 1;
	}

	public int getId() {
		return id;
	}
}
