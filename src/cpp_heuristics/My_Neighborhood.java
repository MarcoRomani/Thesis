package cpp_heuristics;

import java.util.ArrayList;

import general.DataCenter;

public interface My_Neighborhood {

	public void setUp(DataCenter dc,ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u,CPPSolution sol);
	public void clear();
}
