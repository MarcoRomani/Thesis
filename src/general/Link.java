package general;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Link extends DefaultWeightedEdge{

	
	private double initCapacity;
	private double resCapacity;
	private Node source;
	private Node target;
	
	public Link(Node s, Node t, double c) {
		source = s;
		target = t;
		initCapacity = c;
		resCapacity = initCapacity;
	}

	public double getInitCapacity() {
		return initCapacity;
	}

	public double getResCapacity() {
		return resCapacity;
	}

	public Node getMySource() {
		return source;
	}

	public Node getMyTarget() {
		return target;
	}
	
	@Override
	public String toString() {
		return "("+source.toString()+","+target.toString()+")";
	}
}
