package cpp_heuristics;

import java.util.List;

import general.DataCenter;
/**
 * 
 * @author Marco
 * Interface for CPP neighborhood that iterates on all the solution nearby.
 * setUp and clear are used for some pre-processing and post-processing 
 * 
 */
public interface CPPNeighborhood {

	public boolean hasNext();
	public CPPSolution next();
	public void setUp(DataCenter dc,List<ServerStub> stubs, List<ServerStub> stubs_u,CPPSolution sol);
	public void clear();
}
