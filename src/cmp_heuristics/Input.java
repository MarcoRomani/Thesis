package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;

public class Input {

	protected List<List<Container>> clusters_obl = new ArrayList<List<Container>>();
	protected List<Container> singles_obl = new ArrayList<Container>();
	protected List<List<Container>> clusters_opt  = new ArrayList<List<Container>>();
	protected List<Container> singles_opt=new ArrayList<Container>();
	
	public Input(List<List<Container>> clusters_obl, List<Container> singles_obl, List<List<Container>> clusters_opt, List<Container> singles_opt) {
		this.clusters_obl = clusters_obl;
		this.singles_obl = singles_obl;
		this.clusters_opt = clusters_opt;
		this.singles_opt = singles_opt;
	}

	public List<List<Container>> getClustersOBL() {
		return clusters_obl;
	}

	public List<Container> getSinglesOBL() {
		return singles_obl;
	}
	public List<List<Container>> getClustersOPT() {
		return clusters_opt;
	}

	public List<Container> getSinglesOPT() {
		return singles_opt;
	}
}
