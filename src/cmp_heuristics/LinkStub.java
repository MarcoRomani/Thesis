package cmp_heuristics;

import general.Link;
import general.Node;

public class LinkStub {

	protected Link realLink;
	protected double resCapacity;
	protected Node source;
	protected Node target;
	
	public LinkStub(Link l) {
		this.realLink = l;
		resCapacity = l.getResidCapacity();
		source = l.getMySource();
		target = l.getMyTarget();
	}

	public Link getRealLink() {
		return realLink;
	}

	public double getResCapacity() {
		return resCapacity;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}

	public void setResCapacity(double resCapacity) {
		this.resCapacity = resCapacity;
	}
	
	
}
