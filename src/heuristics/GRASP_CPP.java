package heuristics;

import java.util.ArrayList;

import general.*;

public class GRASP_CPP {

	
	private DataCenter dc;
	private ArrayList<Customer> req;
	
	public CPPSolution grasp(int maxIter, int seed) {
		
		CPPSolution best;
		for(int i=0; i<maxIter; i++) {
			
		    CPPSolution incumbent;
			
			incumbent = this.greedy_rand_construction(seed);
			
			if(!(checkFeasibility(incumbent))) {
				this.repair(incumbent);
			}
			
			LocalSearch ls= new LocalSearch(Neighborhood n);
			incumbent = ls.search(incumbent);
			
			Evaluator ev = new CPPEvaluator();
			if(ev.evaluate(incumbent) > ev.evaluate(best)) {
				best = incumbent;
			}
		}
		
		return best;
	}

	
	
	
	
	private void repair(CPPSolution incumbent) {
		// TODO Auto-generated method stub
		
	}





	private boolean checkFeasibility(CPPSolution incumbent) {
		// TODO Auto-generated method stub
		return false;
	}





	private CPPSolution greedy_rand_construction(Container vm,int seed) {
		// TODO Auto-generated method stub
		CPPSolution sol = new CPPSolution();
		ArrayList<Server> E = new ArrayList<Server>();
		for(Pod p:dc.getPods()) {
			for(Rack r: p.getRacks()) {
				E.addAll(r.getHosts());
			}
		}
		
		ArrayList<Float> costs = new ArrayList<Float>();
		for(Server e:E) {
			costs.add(this.incrementalCost(vm,e));
		}
		
		while(!(E.size() == 0)){
			float c_min = 1000;
			float c_max = 0;
			for(Float ce: costs) {
				if(ce.floatValue() < c_min) {
					c_min = ce.floatValue();
				}
				if(ce.floatValue() > c_max) {
					c_max = ce.floatValue();
				}
			}
			
			ArrayList<Server> RCL = new ArrayList<Server>();
			
			
		}
	}





	private Float incrementalCost(Container vm, Server e) {
		// TODO Auto-generated method stub
		
	}
}
