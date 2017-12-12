package general;

public class Link {

	private float initCapacity;
	private float resCapacity;
	private Node source;
	private Node target;
	
	public Link(Server s, Server t, float c) {
		source = s;
		target = t;
		initCapacity = c;
		resCapacity = initCapacity;
	}

	public float getInitCapacity() {
		return initCapacity;
	}

	public float getResCapacity() {
		return resCapacity;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}
	
	@Override
	public String toString() {
		return "("+source.toString()+","+target.toString()+")";
	}
}
