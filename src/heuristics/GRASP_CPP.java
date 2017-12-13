package heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import general.*;

public class GRASP_CPP {

	
	private DataCenter dc;
	private ArrayList<Customer> req;
	
	public CPPSolution grasp(int maxIter, int seed, float alfa) {
		
		CPPSolution best;
		for(int i=0; i<maxIter; i++) {
			
		    CPPSolution incumbent;
		    ArrayList<ServerStub> stubs = new ArrayList<ServerStub>();
			
			for(Pod p: dc.getPods()) {
				for(Rack r: p.getRacks()) {
					for(Server s: r.getHosts()) {
						stubs.add(new ServerStub(s));
					}
				}
			}
			
			incumbent = this.greedy_rand_construction(seed, alfa, stubs);
			
			if(!(checkFeasibility(incumbent, stubs))) {
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





	private boolean checkFeasibility(CPPSolution incumbent, ArrayList<ServerStub> stubs) {
		// TODO Auto-generated method stub
		
		int [] usedBDWout = new int[stubs.size()];
		int [] usedBDWin = new int[stubs.size()];
		
		// TODO calcolare i contraints, serve hashmap x_old e hashmap INCUMBENT
		
		
		
		return false;
	}





	private CPPSolution greedy_rand_construction(int seed, float alfa, ArrayList<ServerStub> stubs) {
		// TODO Auto-generated method stub
		
		SecureRandom rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CPPSolution sol = new CPPSolution();
		ArrayList<Server> E = new ArrayList<Server>();
		for(Pod p:dc.getPods()) {
			for(Rack r: p.getRacks()) {
				E.addAll(r.getHosts());
			}
		}
		
		ArrayList<Container> vms = new ArrayList<Container>();
		for(Customer cust: req) {
			vms.addAll(cust.getNewContainers());
		}
		ArrayList<Float> costs = new ArrayList<Float>();
		
		while(!(vms.size() == 0)){
			costs.clear();
			
			for(Server e:E) {
				costs.add(this.incrementalCost(vms.get(0),e));
			}
			
			float c_min =  Float.POSITIVE_INFINITY;
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
			for(int i = 0; i<costs.size(); i++) {
				if(costs.get(i).floatValue() <= c_min + alfa*(c_max - c_min)) {
					RCL.add(E.get(i));
				}
			}
			
			sol.add(vms.get(0),RCL.get(rng.nextInt(RCL.size())));
			vms.remove(0);
			
		}
		
		return sol;
	}





	private Float incrementalCost(Container vm, Server e) {
		// TODO Auto-generated method stub
		
	}
}
