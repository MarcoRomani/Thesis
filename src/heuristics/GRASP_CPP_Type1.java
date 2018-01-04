package heuristics;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import general.*;


/**
 * 
 * @author Marco
 * grasp configuration:  
 *  - incremental cost is = partial obj funct. if feasible, +infinity o.w.
 *  - greedy rand constr. by 1) placing first all the new customers 2) placing other requests
 *  - new customers placement is chosen by racks' residual memory in grasp fashion, the ones in excess (if any) go with other requests
 *  - local search  with multi neighbourhood : they are applied in cycle following the order in which they are given
 */

public class GRASP_CPP_Type1 extends GRASP_CPP_Scheme{

	protected int neigh_index;

	
	public GRASP_CPP_Type1(DataCenter dc, List<Iterator<CPPSolution>> iters) {
		
		this.neighborhoods.addAll(iters);
		neigh_index = 0;
		this.neighborhood_explorer = iters.get(neigh_index);
		
		this.dc = dc;
		stubs = new ArrayList<ServerStub>();
		stubs_u = new ArrayList<ServerStub>();
		
		for(Customer c: Customer.custList) {
			if(c.getContainers().size() == 0) { newcust.add(c);}
			else { req.add(c);}
		}
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					ServerStub tmp = new ServerStub(s);
					stubs.add(tmp);
					if(s.getResidual_cpu() >= 0.4*s.getCpu()) {
						stubs_u.add(tmp);
					}
				}
			}
		}
		
		
	}
	
	
	
	protected void repair(CPPSolution incumbent) {
		// TODO Auto-generated method stub
		
	}





    @Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {
		
		CPPSolution sol = new CPPSolution();
		
		Result result = allnew_constr(alfa,sol);
		System.out.println("new cust done \n");
		sol = result.getSol();
		ArrayList<Container> toPlace = result.getRest();
		for(Customer c: req) {
			toPlace.addAll(c.getNewContainers());
		}
		
		sol = notnew_constr(toPlace,alfa,sol);
		System.out.println("other cust done");
		return sol;
		
		
	}

    /**
     * 
     * @param toPlace
     * @param alfa
     * @param stubs_u
     * @param stubs requires that these are all stubs ordere by id, with placement in list = id
     * @param incumbent
     * @return
     * @throws InfeasibilityException
     */
    
    protected CPPSolution notnew_constr(ArrayList<Container> toPlace, float alfa, CPPSolution incumbent) throws InfeasibilityException {
    	
    	ArrayList<ServerStub> E = stubs_u;
		
		
		ArrayList<Container> vms = toPlace;
		
		
		ArrayList<Double> costs = new ArrayList<Double>();
		
		while(!(vms.size() == 0)){
			costs.clear();
			
			for(ServerStub e: E) {
				costs.add(this.incrementalCost(vms.get(0), e, incumbent));
			}
			
			double c_min =  Float.POSITIVE_INFINITY;
			double c_max = 0;
			for(Double ce: costs) {
				if(ce.doubleValue() < c_min) {
					c_min = ce.doubleValue();
				}
				if(ce.doubleValue() > c_max) {
					c_max = ce.doubleValue();
				}
			}
			
			if(c_min == Float.POSITIVE_INFINITY) 
				throw new InfeasibilityException(); 
			
			// if many infeasibility slows tha alg, we could tune alfa or remove here all the infeasible options and build RCL on the result
			// at the price of slowing down each iteration
			
			ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();
			for(int i = 0; i<costs.size(); i++) {
				if(costs.get(i).floatValue() <= c_min + alfa*(c_max - c_min)) {
					RCL.add(E.get(i));
				}
			}
		//	System.out.println("RCL size = "+RCL.size());
			
			ServerStub candidate = RCL.get(rng.nextInt(RCL.size()));
			if(!(candidate.allocate(vms.get(0), stubs, incumbent, dc, true))) { continue; }
			incumbent.getTable().put(vms.get(0), candidate.getId()); // 
			vms.remove(0);
			
		}
		
		return incumbent;
    }

	/**
	 * 
	 * @param seed
	 * @param alfa
	 * @param stubs_u
	 * @param stubs   requires that these are all the stubs orderer by id, id must correspond to the position in the list
	 * @return
	 */

	protected Result allnew_constr(float alfa, CPPSolution incumbent) {
		
		//SecureRandom rng = new SecureRandom();
		
		CPPSolution sol = incumbent;
		ArrayList<Container> rest = new ArrayList<Container>();
		
		ArrayList<Rack> racks = new ArrayList<Rack>();
		for(Pod p: dc.getPods()) {
			racks.addAll(p.getRacks());
		}
		
		// for each rack calculate its residual memory
		ArrayList<Double> estCap = new ArrayList<Double>();
		int count = 0;
		for(int i=0;i < racks.size(); i++) {
			estCap.add(new Double(0));
			for(Server s: racks.get(i).getHosts()) {
				if(stubs_u.get(count) == stubs.get(s.getId())) {
					estCap.set(i, new Double(estCap.get(i).doubleValue() + stubs_u.get(count).getRes_mem()));
					count +=1;
				}
				
			}
		}

		ArrayList<Container> vms = new ArrayList<Container>();
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<Rack> RCL = new ArrayList<Rack>();
		double c_min = Double.POSITIVE_INFINITY;
		double c_max = 0;
		
		for(Customer c: newcust) {
			costs.clear();
			RCL.clear();
			vms.clear();
			vms.addAll(c.getNewContainers());
			c_min = Float.POSITIVE_INFINITY;
			c_max = 0;
			
			// ram requirement of the new customer
			double mem = 0;
			for(Container ct: vms) {
				mem += ct.getMem();
			}
			
			// incremental costs of racks, based on residual ram
			for(int i=0; i<racks.size(); i++) {
	            if(estCap.get(i).floatValue() < mem) {
	            	costs.add(new Double(10000 + (mem-estCap.get(i))));  // big M
	            } else costs.add(new Double(estCap.get(i)-mem));
	            if (costs.get(i).doubleValue() < c_min) c_min = costs.get(i).doubleValue();
	            if (costs.get(i).doubleValue() > c_max) c_max = costs.get(i).doubleValue();
			}
			
			//build RCL of racks
			for(int i=0;i<racks.size();i++) {
				if (costs.get(i) <= c_min + alfa*(c_max - c_min)) {
					RCL.add(racks.get(i));
				}
			}
			
			// pick one rack at random from RCL
			Rack r = RCL.get(rng.nextInt(RCL.size()));
			ArrayList<ServerStub> substub = new ArrayList<ServerStub>();
			
			for(Server s: r.getHosts()) {
				if(s.getResidual_cpu() >= 0.4*s.getCpu()) {
					substub.add(stubs.get(s.getId()));
				}
			}
			if(substub.isEmpty()) {
				rest.addAll(vms);
				continue;   // skip to next customer
			}
			
			Comparator<ServerStub> comp = new RamComparator();
			substub.sort(comp); // descending
			
			 int n =0;
			 ArrayList<Double> profit = new ArrayList<Double>();
			 ArrayList<Container> here = new ArrayList<Container>();
			 
			// manually insert first vm (max ram) in first stub (max ram) to better guide the rest
			int ram_max = 0;
			for(int i=0; i<vms.size(); i++) {
				if(vms.get(i).getMem() > vms.get(ram_max).getMem()) {
					ram_max = i;
				}
			}
			if(substub.get(0).allocate(vms.get(ram_max), stubs, sol, dc, true)){
				sol.getTable().put(vms.get(ram_max), substub.get(n).getId());
				here.add(vms.get(ram_max));
				vms.remove(vms.get(ram_max));
			}
			
		  
		   while(vms.size() > 0 && n < substub.size()) {
			   profit.clear();
			   for(Container v: vms) {
				   if(!(substub.get(n).allocate(v, stubs, sol, dc,false))) {
					   profit.add(Double.NEGATIVE_INFINITY);
					   continue;
				   }
				   double pr = 0;
				   for(Container h: here) {
					   pr += (c.getTraffic().get(new C_Couple(v,h)) == null)? 0 : c.getTraffic().get(new C_Couple(v,h)).doubleValue();
					   pr += (c.getTraffic().get(new C_Couple(h,v)) == null)? 0 : c.getTraffic().get(new C_Couple(h,v)).doubleValue();
				   }
				   profit.add(new Double(pr));
			   }
			   
			   int max = 0;
			   for(int i=0; i<profit.size(); i++) {
				   if(profit.get(i).doubleValue() > profit.get(max).doubleValue()) {
					   max = i;
				   }
			   }
			   if(profit.get(max).doubleValue() >= 0) {
				   substub.get(n).allocate(vms.get(max), stubs, sol, dc, true);
				   sol.getTable().put(vms.get(max), new Integer(substub.get(n).getId()));
				   here.add(vms.get(max));
				   vms.remove(vms.get(max));
			   }else {
				   n++;
				   here.clear();
			   }
			   
		   }
			
		   rest.addAll(vms);
		
		}
		return new Result(sol,rest);
	}


	@Override
	protected Double incrementalCost(Container vm, ServerStub e, CPPSolution incumbent) {
		double cost =0;
		
		if(!(e.allocate(vm, stubs, incumbent, dc, false))) {
			cost = Float.POSITIVE_INFINITY;
			return cost;
		}
		
		Customer r = Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();
		
		for(Container c: conts) {
			Server s = dc.getPlacement().get(c);
			Double t1 = r.getTraffic().get(new C_Couple(vm,c));
			Double t2 = r.getTraffic().get(new C_Couple(c,vm));
			if(!(t1 == null)) cost += dc.getCosts()[e.getId()][s.getId()]*t1.doubleValue();
			if(!(t2 == null)) cost += dc.getCosts()[s.getId()][e.getId()]*t2.doubleValue();
		}
		conts = r.getNewContainers();
		
		for(Container c: conts) {
			Integer s= incumbent.getTable().get(c);
			if(!(s == null)) {
				Double t1 = r.getTraffic().get(new C_Couple(vm,c));
				Double t2 = r.getTraffic().get(new C_Couple(c,vm));
				if(!(t1 == null)) cost += dc.getCosts()[e.getId()][s.intValue()]*t1.doubleValue();
				if(!(t2 == null)) cost += dc.getCosts()[s.intValue()][e.getId()]*t2.doubleValue();
			}
		}
		
		return new Double(cost);
	}



	@Override
	protected void changeNeighborhood() {
		neigh_index ++;
		if(neigh_index >= neighborhoods.size()) {
			neigh_index = 0;
		}
		neighborhood_explorer = neighborhoods.get(neigh_index);
	}



	@Override
	protected void reset(CPPSolution solution) {
		
		super.reset(solution);
		
		// ---- RESET NEIGHBORHOODS ------
		neigh_index = 0;
		neighborhood_explorer = neighborhoods.get(neigh_index);
		for(Iterator<CPPSolution> n : neighborhoods) {
			((My_Neighborhood) n).clear();
		}
	}


}
