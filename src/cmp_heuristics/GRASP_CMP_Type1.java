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
		stubs_migr = new ArrayList<ServerStub>();
		stubs_after = new ArrayList<ServerStub>();
		this.dc = dc;
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					stubs_migr.add(new ServerStub(s));
					stubs_after.add(new ServerStub(s));
					
					// DISTINCTION MIGR - AFTER MIGR
				}
			}
		}
	}

	@Override
	protected CMPSolution greedy_rand_constr(List<Container> toPlace, float alfa) {
		
		CMPSolution sol = new CMPSolution();
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();

		while(!toPlace.isEmpty()) {
			costs.clear();
			RCL.clear();
			Container m = toPlace.remove(0);
			
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for(int i=0; i<stubs_after.size(); i++) {
				double tmp = incrementalCost(m,stubs_after.get(i));
				costs.add(new Double(tmp));
				if(tmp < min ) min = tmp;
				if(tmp > max && tmp < Double.POSITIVE_INFINITY) max = tmp;
			}
			
			for(int i=0;i<costs.size();i++) {
				if(costs.get(i).doubleValue() < min + alfa*(max - min)) {
					RCL.add(stubs_after.get(i));
				}
			}
			
			boolean found = false;
			while(!RCL.isEmpty() || found) {
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				Response r = canMigrate(m,dc.getPlacement().get(m).getId(), e.getId());
				found = r.getAnswer();
				if(found) {
					e.forceAllocation(m, stubs_after, sol, dc);
					sol.getTable().put(m, new Integer(e.getId()));
					updateLinks(r.getFlow());
					sol.getFlows().put(m, r.getFlow());
				}
				
				
			}
			
			
			
		}
		
		return null;
	}

	private Response canMigrate(Container m, int id, int id2) {
		// TODO Auto-generated method stub
		return null;
	}

	private void updateLinks(ArrayList<LinkFlow> flow) {
		// TODO Auto-generated method stub
		
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
