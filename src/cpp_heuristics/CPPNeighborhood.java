package cpp_heuristics;

import java.util.List;

import general.DataCenter;

public interface CPPNeighborhood {

	public boolean hasNext();
	public CPPSolution next();
	public void setUp(DataCenter dc,List<ServerStub> stubs, List<ServerStub> stubs_u,CPPSolution sol);
	public void clear();
}
