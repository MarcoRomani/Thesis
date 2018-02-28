package cmp_heuristics;

import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Node;

public interface CMPNeighborhood {

	public boolean hasNext();
	public CMPSolution next();
	public void setUp(CMPDataCenter dc, Map<Container, Boolean> inpuTable, List<ServerStub> stubs,  DefaultDirectedWeightedGraph<Node, LinkStub> graph, CMPSolution sol);
	public void clear();
}
