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
	protected ArrayList<Iterator<CPPSolution>> neighborhoods = new ArrayList<Iterator<CPPSolution>>();
	protected DataCenter dc;
	protected ArrayList<Customer> req = new ArrayList<Customer>();
	protected ArrayList<Customer> newcust = new ArrayList<Customer>();
	
	protected ArrayList<ServerStub> stubs;
	protected ArrayList<ServerStub> stubs_u;
	
	
    protected abstract CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException;
	
	protected abstract Double incrementalCost(Container vm, ServerStub e, CPPSolution incumbent);
	
	protected abstract void repair(CPPSolution incumbent);
	
	protected abstract void changeNeighborhood();
	
	
	public  CPPSolution grasp(int maxIter, int seed, float alfa) {
		
		rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CPPSolution best = new CPPSolution();
		 

		for(int i=0; i<maxIter; i++) {
			System.out.println("\n iter:"+i);
		    CPPSolution incumbent = new CPPSolution();
		     			
			try {
				incumbent = this.greedy_rand_construction(alfa);
			} catch (InfeasibilityException e) {
				System.out.println("infeasible");
				reset(incumbent);
				continue;
			}
			
			
			
			
			evaluate(incumbent);
			System.out.println(incumbent.toString());
			
			
			
			//-------- LOCAL SEARCH WITH MULTI-NEIGHBORHOODS --------------
			
		    int count = 0;
		    do {
		    	CPPSolution newincumbent = localSearch(incumbent);
		    	if(!(newincumbent.getValue() < incumbent.getValue())) {
					count++;
		    	}else count = 0;
		    	incumbent = newincumbent;
		    	changeNeighborhood();
		    }while(count < neighborhoods.size() && neighborhoods.size() > 1);
		
			
		    //--------- UPDATE BEST SOLUTION AMONG ITERATIONS ------------
			if(incumbent.getValue() < best.getValue()) {
				best = (CPPSolution)incumbent.clone();
			}
			
			/*
			if(!(checkFeasibility(incumbent))) {
				System.out.println("SOMETHING's WRONG");
			}*/
			
			// --------- PREPARE FOR NEXT ITERATION ----------------------
			reset(incumbent);
			
		}
		
		return best;
	}
	
	
	
	protected void reset(CPPSolution solution) {
		CPPSolution my_sol = solution;
		
		ArrayList<Container> toRemove = new ArrayList<Container>();
		toRemove.addAll(my_sol.getTable().keySet());
		for(Container vm: toRemove) {
			ServerStub tmp = stubs.get(my_sol.getTable().get(vm).intValue());
			tmp.remove(vm, stubs, my_sol, dc);
			my_sol.getTable().remove(vm);
		}
		/*
		for(ServerStub s: stubs) {
			if(s.getRes_out() != s.getRealServ().getResidual_bdw_out()) {
				System.out.println("something's wrong: "+s.getRes_out()+" , "+s.getRealServ().getResidual_bdw_out()+" containers="+s.getContainers());
				
			}
		}*/
	}
	
	
	protected CPPSolution localSearch(CPPSolution init_sol)  {
		
		CPPSolution sol = (CPPSolution)init_sol.clone();
		evaluate(sol);


		CPPSolution best_neighbour = sol;
		
		System.out.println("start local search");
		
		 do {
		//	  System.out.println("Try new neighborhood");
			sol = (CPPSolution)best_neighbour.clone();
			((My_Neighborhood)(neighborhood_explorer)).setUp(dc,stubs, stubs_u,best_neighbour);
			
			  while(neighborhood_explorer.hasNext()) {
				
				   CPPSolution current = neighborhood_explorer.next();
				   if(evaluate(current) < best_neighbour.getValue()) { 
					   best_neighbour = (CPPSolution)current.clone(); 
				    //System.out.println("new best neighbour found"); 
				   }
				 
			  }

	    	}while(sol.getValue() != best_neighbour.getValue());
		 
		
		 
		
		 ((My_Neighborhood)(neighborhood_explorer)).clear();
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
    
    
    protected boolean checkFeasibility(CPPSolution incumbent) {
	
    	ArrayList<Container> tmp2 = new ArrayList<Container>();
		
		for(Customer c: req) {
			tmp2.addAll(c.getNewContainers());
		}
		for(Customer c: newcust) {
			tmp2.addAll(c.getNewContainers());
		}
		
		if(tmp2.size() != incumbent.getTable().size()) return false;
		
		
    	ArrayList<Server> servers = new ArrayList<Server>();
    	for(Pod p: dc.getPods()) {
    		for(Rack r: p.getRacks()) {
    			for(Server s: r.getHosts()) {
    				servers.add(s);
    			}
    		}
    	}
		
		float [] usedBDWout = new float[servers.size()];
		float [] usedBDWin = new float[servers.size()];
		float [] usedCPU = new float[servers.size()];
		float [] usedRAM = new float[servers.size()];
		float [] usedDISK = new float[servers.size()];
		
		for(int i=0; i<servers.size(); i++) {
			ArrayList<Container> tmp = servers.get(i).getContainers();
			for(Container c1: tmp) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				for(Container c2: r.getNewContainers()) {
					if(!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId()) ) {
						usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1,c2)) == null)? 0 : r.getTraffic().get(new C_Couple(c1,c2)).floatValue();
						usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2,c1))== null) ? 0 : r.getTraffic().get(new C_Couple(c2,c1)).floatValue();
					}
				}
			}
		}	
		
		
			
		for(Container c1: tmp2) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				int i = incumbent.getTable().get(c1).intValue();
				usedCPU[i] += c1.getCpu()*((float)2500/servers.get(i).getFrequency());
				usedRAM[i] += c1.getMem();
				usedDISK[i] += c1.getDisk();
				usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1,Container.c_0)) == null) ? 0 : r.getTraffic().get(new C_Couple(c1,Container.c_0)).floatValue();
				usedBDWin[i] += (r.getTraffic().get(new C_Couple(Container.c_0,c1))==null) ? 0: r.getTraffic().get(new C_Couple(c1,Container.c_0)).floatValue();
				for(Container c2: r.getContainers()) {
						if(!(dc.getPlacement().get(c2).getId() == servers.get(i).getId())) {
							usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1,c2)) == null)? 0 : r.getTraffic().get(new C_Couple(c1,c2)).floatValue();
							usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2,c1))== null) ? 0 : r.getTraffic().get(new C_Couple(c2,c1)).floatValue();
						}
				}
						
				for(Container c2: r.getNewContainers()) {
					if(!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId()) ) {
						usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1,c2)) == null)? 0 : r.getTraffic().get(new C_Couple(c1,c2)).floatValue();
						usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2,c1))== null) ? 0 : r.getTraffic().get(new C_Couple(c2,c1)).floatValue();
					}
				}
			}
			
			
			
			
		
		
		for(int i=0; i< servers.size(); i++) {
			if(servers.get(i).getResidual_bdw_out() - usedBDWout[i] < 0) return false;
			if(servers.get(i).getResidual_bdw_in() - usedBDWin[i] < 0) return false;
			if(servers.get(i).getResidual_cpu() - usedCPU[i] < 0) return false;
			if(servers.get(i).getResidual_mem() - usedRAM[i] < 0) return false;
			if(servers.get(i).getResidual_disk() - usedDISK[i] < 0) return false;
		}
		
		return true;
	}
    
    
    
    
    
 }
