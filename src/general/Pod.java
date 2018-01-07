package general;

import java.util.ArrayList;

public abstract class Pod implements Comparable<Pod> {

	public static int pod_id = 0;
	protected int id;
	protected int racks_number;
	protected ArrayList<Rack> racks = new ArrayList<Rack>();
	protected ArrayList<Switch> core;
	protected ArrayList<Switch> edge = new ArrayList<Switch>();
	protected ArrayList<Switch> aggregation = new ArrayList<Switch>();
	
	protected int lower_index;
	protected int upper_index;
	
	protected abstract void build();

	public int getId() {
		return id;
	}

	public int getRacks_number() {
		return racks_number;
	}

	public ArrayList<Rack> getRacks() {
		return racks;
	}

	public ArrayList<Switch> getCore() {
		return core;
	}

	public ArrayList<Switch> getEdge() {
		return edge;
	}

	public ArrayList<Switch> getAggregation() {
		return aggregation;
	}

	public int getLower_index() {
		return lower_index;
	}

	public int getUpper_index() {
		return upper_index;
	}
	
	public boolean containsServer(int i) {
		return (i >= lower_index && i <= upper_index);
	}
}
