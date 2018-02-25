package cmp_heuristics;

import java.util.List;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Node;

public interface CMPNeighborhood {

	public boolean hasNext();
	public CMPSolution next();
	public void setUp(CMPDataCenter dc,List<ServerStub> stubs,  DefaultDirectedWeightedGraph<Node, LinkStub> graph, CMPSolution sol);
	public void clear();
}
