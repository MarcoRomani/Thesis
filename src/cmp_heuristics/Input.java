package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;

public class Input {

	protected List<List<Container>> clusters = new ArrayList<List<Container>>();
	protected List<Container> singles = new ArrayList<Container>();
	
	public Input(List<List<Container>> clusters, List<Container> singles) {
		this.clusters = clusters;
		this.singles = singles;
	}

	public List<List<Container>> getClusters() {
		return clusters;
	}

	public List<Container> getSingles() {
		return singles;
	}
}
