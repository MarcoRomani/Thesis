package heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;

import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;

public abstract class GRASP_CPP_Scheme {

	protected SecureRandom rng;
	protected Iterator<CPPSolution> neighborhood_explorer;
	protected DataCenter dc;
	protected ArrayList<Customer> req = new ArrayList<Customer>();
	protected ArrayList<Customer> newcust = new ArrayList<Customer>();
	
	protected ArrayList<ServerStub> stubs;
	protected ArrayList<ServerStub> stubs_u;
	
	
    protected abstract CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException;
	
	protected abstract Float incrementalCost(Container vm, ServerStub e, CPPSolution incumbent);
	
	protected abstract void repair(CPPSolution incumbent);
	
	
	
	
	public  CPPSolution grasp(int maxIter, int seed, float alfa) {
		
		rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CPPSolution best = new CPPSolution();
		 

		for(int i=0; i<maxIter; i++) {
			System.out.println("\n iter:"+i);
		    CPPSolution incumbent;
		     			
			try {
				incumbent = this.greedy_rand_construction(alfa);
			} catch (InfeasibilityException e) {
				System.out.println("infeasible");
				continue;
			}
			
			/*
			if(!(checkFeasibility(incumbent, stubs))) {
				this.repair(incumbent);
			}
			*/
			
			incumbent = localSearch(incumbent);
			
			
			if(incumbent.getValue() < best.getValue()) {
				best = incumbent;
			}
			
			// clear stubs for next iteration
			for(Customer c: req) {
				for(Container ct: c.getNewContainers()) {
					ServerStub tmp = stubs.get(incumbent.getTable().get(ct).intValue());
					tmp.reset();
				}
			}
			for(Customer c: newcust) {
				for(Container ct: c.getNewContainers()) {
					ServerStub tmp = stubs.get(incumbent.getTable().get(ct).intValue());
					tmp.reset();
				}
			}
		}
		
		return best;
	}
	
	
	
	
	
	
	protected CPPSolution localSearch(CPPSolution init_sol) {
		
		CPPSolution sol = (CPPSolution)init_sol.clone();
		evaluate(sol);
		((My_Neighborhood)(neighborhood_explorer)).setUp(dc,stubs, stubs_u,sol);

		CPPSolution best_neighbour = sol;
		System.out.println("start local search");
		while(sol.getValue() != best_neighbour.getValue()) {
			
			sol = best_neighbour;
			
			
			while(neighborhood_explorer.hasNext()) {
				System.out.println("Try new neighborhood");
				CPPSolution current = neighborhood_explorer.next();
				if(evaluate(current) < best_neighbour.getValue()) { best_neighbour = current; System.out.println("new best neighbour found"); }
				
			}

		}
		System.out.println("end local search");
		sol = best_neighbour;
		System.out.println(sol.toString());
		return sol;
	}
	
	
	
	
	
	
    protected float evaluate(CPPSolution sol) {
		
		float value = 0;
		ArrayList<Customer> custs = Customer.custList;
		
		for(Customer c: custs) {
			ArrayList<Container> conts = c.getContainers();
			ArrayList<Container> newconts = c.getNewContainers();
			for(Container c1: conts) {
				int s1 = dc.getPlacement().get(c1).getId();
				for(Container c2: newconts) {
					int s2 = sol.getTable().get(c2).intValue();
					if(c.getTraffic().get(new C_Couple(c1,c2)) != null) {
					    value += c.getTraffic().get(new C_Couple(c1,c2)).floatValue()*dc.getCosts()[s1][s2];
					}
					if(c.getTraffic().get(new C_Couple(c2,c1)) != null) {
				    	value += c.getTraffic().get(new C_Couple(c2,c1)).floatValue()*dc.getCosts()[s2][s1];
					}
				}
			}
			
			for(Container c1: newconts) {
				int s1 = sol.getTable().get(c1).intValue();
				for(Container c2: newconts) {
					if(c.getTraffic().get(new C_Couple(c1,c2)) != null) {
						value += c.getTraffic().get(new C_Couple(c1,c2)).floatValue()*dc.getCosts()[s1][sol.getTable().get(c2)];
					}
				}
			}
					
		}
		sol.setValue(value);
		return value;
	 }
    
    
    protected boolean checkFeasibility(CPPSolution incumbent, ArrayList<ServerStub> stubs) {
	
    	ArrayList<Server> servers = new ArrayList<Server>();
    	for(Pod p: dc.getPods()) {
    		for(Rack r: p.getRacks()) {
    			for(Server s: r.getHosts()) {
    				servers.add(s);
    			}
    		}
    	}
		
		int [] usedBDWout = new int[servers.size()];
		int [] usedBDWin = new int[servers.size()];
		
		for(int i=0; i<servers.size(); i++) {
			ArrayList<Container> tmp = servers.get(i).getContainers();
			for(Container c1: tmp) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				for(Container c2: r.getNewContainers()) {
					if(!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId()) ) {
						usedBDWout[i] += r.getTraffic().get(new C_Couple(c1,c2));
						usedBDWin[i] += r.getTraffic().get(new C_Couple(c2,c1));
					}
				}
			}
			
			tmp.clear();
			tmp = stubs.get(i).getContainers();
			for(Container c1: tmp) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				for(Container c2: r.getContainers()) {
					if(!(dc.getPlacement().get(c2).getId() == servers.get(i).getId())) {
						usedBDWout[i] += r.getTraffic().get(new C_Couple(c1,c2));
						usedBDWin[i] += r.getTraffic().get(new C_Couple(c2,c1));
					}
				}
				
				for(Container c2: r.getNewContainers()) {
					if(!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId()) ) {
						usedBDWout[i] += r.getTraffic().get(new C_Couple(c1,c2));
						usedBDWin[i] += r.getTraffic().get(new C_Couple(c2,c1));
					}
				}
			}
			
			
			
			
		}
		
		for(int i=0; i< servers.size(); i++) {
			if(servers.get(i).getResidual_bdw_out() - usedBDWout[i] < 0) return false;
			if(servers.get(i).getResidual_bdw_in() - usedBDWin[i] < 0) return false;
		}
		
		return true;
	}
    
    
    
    
    
 }
