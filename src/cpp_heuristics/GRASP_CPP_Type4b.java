package cpp_heuristics;

import java.util.List;

import general.Customer;
import general.DataCenter;
import general.Rack;
import general.Server;

public class GRASP_CPP_Type4b extends GRASP_CPP_Type1Indexing {

	public GRASP_CPP_Type4b(DataCenter dc, List<CPPNeighborhood> neighs) {
		super(dc, neighs);
	}
	
	public GRASP_CPP_Type4b(DataCenter dc) {
		super(dc);
	}

	@Override
	protected Double computeCapacity(Rack r) {
		double tmp=0;
		for(Server s: r.getHosts()) {
			if(s.isUnderUtilized()) tmp+= stubs.get(s.getId()).getRes_out()+stubs.get(s.getId()).getRes_out();
		}
		return new Double(tmp);
	}

	@Override
	protected Double computeRequirement(Customer c) {
		double tmp=0;
		
		return new Double(tmp);
	}

}
