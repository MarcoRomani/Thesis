package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Pod;
import general.Rack;
import general.Server;

public class GRASP_CMP_Type1 extends GRASP_CMP_Scheme {

	public GRASP_CMP_Type1(CMPDataCenter dc, List<Container> mandatory, List<Container> optional) {
		this.mandatory = mandatory;
		this.optional = optional;
		stubs = new ArrayList<ServerStub>();
		this.dc = dc;
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					stubs.add(new ServerStub(s));
					
					// DISTINCTION MIGR - AFTER MIGR
				}
			}
		}
	}
	
	@Override
	protected CMPSolution greedy_rand_constr(List<Container> toPlace, float alfa) {
		
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();

		while(!toPlace.isEmpty()) {
			costs.clear();
			RCL.clear();
			Container m = toPlace.remove(0);
			
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for(int i=0; i<stubs.size(); i++) {
				double tmp = incrementalCost(m,stubs.get(i));
				costs.add(new Double(tmp));
				if(tmp < min ) min = tmp;
				if(tmp > max && tmp < Double.POSITIVE_INFINITY) max = tmp;
			}
			
			for(int i=0;i<costs.size();i++) {
				if(costs.get(i).doubleValue() < min + alfa*(max - min)) {
					RCL.add(stubs.get(i));
				}
			}
			
			boolean found = false;
			while(!RCL.isEmpty() || found) {
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				findPath(dc.getPlacement().get(m), e.getId());
				e.
			}
			
			
			
		}
		
		return null;
	}

	@Override
	protected double incrementalCost(Container c, ServerStub s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void changeNeighborhood() {
		// TODO Auto-generated method stub

	}

}
