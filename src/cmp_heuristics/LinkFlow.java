package cmp_heuristics;

import general.Link;

public class LinkFlow {

	protected  Link link;
	protected double flow;
	
	public LinkFlow(Link l, double f) {
		link = l;
		flow = f;
	}

	public Link getLink() {
		return link;
	}

	public double getFlow() {
		return flow;
	}
	
	public void setFlow(double flow) {
		this.flow = flow;
	}
	
	@Override
	public String toString() {
		return "["+link.toString()+","+flow+"]";
	}
}
