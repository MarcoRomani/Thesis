package cpp_heuristics;

import java.util.List;

import general.Customer;
import general.DataCenter;
import general.Rack;
import general.Server;

public class GRASP_CPP_Type3c extends GRASP_CPP_Type3 {

	public GRASP_CPP_Type3c(DataCenter dc, List<CPPNeighborhood> neighs) {
		super(dc, neighs);
		
	}
	
	public GRASP_CPP_Type3c(DataCenter dc) {
		super(dc);
	}

	@Override
	protected Double computeCapacity(Rack r) {
		double tmp=0;
		for(Server s: r.getHosts()) {
			if(s.isUnderUtilized()) tmp+= stubs.get(s.getId()).getRes_out()+stubs.get(s.getId()).getRes_in() +stubs.get(s.getId()).getRes_mem();
		}
		return new Double(tmp);
	}

	@Override
	protected Double computeRequirement(Customer c) {
		return super.computeRequirement(c);
	}

}
