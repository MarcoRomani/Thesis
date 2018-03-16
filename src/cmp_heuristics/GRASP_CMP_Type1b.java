package cmp_heuristics;

import java.util.List;

import general.CMPDataCenter;
import general.CPUcalculator;
import general.Container;
import general.Rack;
import general.Server;

public class GRASP_CMP_Type1b extends GRASP_CMP_Type1ALT {

	public GRASP_CMP_Type1b(CMPDataCenter dc, Input input) {
		super(dc, input);
		
	}
	
	@Override
	protected double computeRequirement(List<Container> cluster, Server s) {
		double cpureq = 0;
		for (Container c : cluster) {
			cpureq += CPUcalculator.utilization(c, s);
		}
		return cpureq;
	}
	
	@Override
	protected double computeCapacity(Rack r) {
		double rackcpu = 0;
		for (Server s : r.getHosts()) {
			if (stubs_after.get(s.getId()).isState()) {
				rackcpu += stubs_after.get(s.getId()).getRes_mem() - Server.overUtilization_constant*s.getCpu();
			}
		}
		return rackcpu;
	}

}
