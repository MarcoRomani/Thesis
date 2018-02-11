package cmp_heuristics;

public class LinkFlow {

	protected  LinkStub link;
	protected double flow;
	
	public LinkFlow(LinkStub l, double f) {
		link = l;
		flow = f;
	}

	public LinkStub getLink() {
		return link;
	}

	public double getFlow() {
		return flow;
	}
}
