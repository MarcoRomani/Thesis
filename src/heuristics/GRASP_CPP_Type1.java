package heuristics;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import general.*;


/**
 * 
 * @author Marco
 * grasp configuration:
 *  - no repair
 *  - incremental cost is = partial obj funct. if feasible, infinity o.w.
 *  - greedy rand constr. by 1) placing first all the new customers 2) placing other requests
 *  - new customers placement is chosen by racks' residual memory, the ones in excess (if any) go with other requests
 *  - local search only with 1-switch neighbourhood : looks for a vm-switch in a server of pods(customer) intersez. s_u
 */

public class GRASP_CPP_Type1 extends GRASP_CPP_Scheme{

	

	
	public GRASP_CPP_Type1(DataCenter dc, Iterator<CPPSolution> iter) {
		
		this.neighborhood_explorer = iter;
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
		
		Result result = allnew_constr(alfa,stubs_u,stubs,sol);
		System.out.println("new cust done \n");
		sol = result.getSol();
		ArrayList<Container> toPlace = result.getRest();
		for(Customer c: Customer.custList) {
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
		
		
		ArrayList<Float> costs = new ArrayList<Float>();
		
		while(!(vms.size() == 0)){
			costs.clear();
			
			for(ServerStub e: E) {
				costs.add(this.incrementalCost(vms.get(0), e, incumbent));
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

	protected Result allnew_constr(float alfa, ArrayList<ServerStub> stubs_u, ArrayList<ServerStub> stubs, CPPSolution incumbent) {
		
		//SecureRandom rng = new SecureRandom();
		
		CPPSolution sol = incumbent;
		ArrayList<Container> rest = new ArrayList<Container>();
		
		ArrayList<Rack> racks = new ArrayList<Rack>();
		for(Pod p: dc.getPods()) {
			racks.addAll(p.getRacks());
		}
		
		// for each rack calculate its residual memory
		ArrayList<Float> estCap = new ArrayList<Float>();
		int count = 0;
		for(int i=0;i < racks.size(); i++) {
			estCap.add(new Float(0));
			for(Server s: racks.get(i).getHosts()) {
				if(stubs_u.get(count) == stubs.get(s.getId())) {
					estCap.set(i, new Float(estCap.get(i).floatValue() + stubs_u.get(count).getRes_mem()));
					count +=1;
				}
				
			}
		}

		ArrayList<Container> vms = new ArrayList<Container>();
		ArrayList<Float> costs = new ArrayList<Float>();
		ArrayList<Rack> RCL = new ArrayList<Rack>();
		float c_min = Float.POSITIVE_INFINITY;
		float c_max = 0;
		
		for(Customer c: newcust) {
			costs.clear();
			RCL.clear();
			vms = c.getNewContainers();
			c_min = Float.POSITIVE_INFINITY;
			c_max = 0;
			
			// ram requirement of the new customer
			float mem = 0;
			for(Container ct: vms) {
				mem += ct.getMem();
			}
			
			// incremental costs of racks, based on residual ram
			for(int i=0; i<racks.size(); i++) {
	            if(estCap.get(i).floatValue() < mem) {
	            	costs.add(new Float(10000 + (mem-estCap.get(i))));  // big M
	            } else costs.add(new Float(estCap.get(i)-mem));
	            if (costs.get(i).floatValue() < c_min) c_min = costs.get(i).floatValue();
	            if (costs.get(i).floatValue() > c_max) c_max = costs.get(i).floatValue();
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
			
			// prepare servers in descending ram order
		    for(ServerStub s_st: stubs_u) {
				if (r.getHosts().contains(s_st.getRealServ())) {
					substub.add(s_st);
				}
			}
			Comparator<ServerStub> comp = new RamComparator();
			substub.sort(comp); // descending
			ArrayList<Container> ws = c.getNewWS();
			ArrayList<Container> as = c.getNewAS();
			ArrayList<Container> dbms = c.getNewDBMS();
			int j= 0;
			int k= 0;
			
			// fill the servers with ws,as and dbms alternated
			while(ws.size()+as.size()+dbms.size() > 0 && j < substub.size() ) {
				if(ws.size() > 0 && substub.get(j).allocate(ws.get(0),stubs,sol,dc,true)) {
					
					sol.getTable().put(ws.remove(0),new Integer(substub.get(j).getId()));
					
				}else { k+=1; }
				if(as.size() > 0 && substub.get(j).allocate(as.get(0),stubs,sol,dc,true)) {

					sol.getTable().put(as.remove(0),new Integer(substub.get(j).getId()));

				}else { k+= 1;}
				if(dbms.size() > 0 && substub.get(j).allocate(dbms.get(0),stubs,sol,dc,true)) {
					
					sol.getTable().put(dbms.remove(0),new Integer(substub.get(j).getId()));

				}else { k+=1; }
				
				if(k > 2) {
					j+=1;
					k=0;
				}
				
			}
			
			rest.addAll(ws);
			rest.addAll(as);
			rest.addAll(dbms);
			
		
			
			
		}
		return new Result(sol,rest);
	}


	@Override
	protected Float incrementalCost(Container vm, ServerStub e, CPPSolution incumbent) {
		float cost =0;
		
		if(!(e.allocate(vm, stubs, incumbent, dc, false))) {
			cost = Float.POSITIVE_INFINITY;
			return cost;
		}
		
		Customer r = Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();
		
		for(Container c: conts) {
			Server s = dc.getPlacement().get(c);
			Float t1 = r.getTraffic().get(new C_Couple(vm,c));
			Float t2 = r.getTraffic().get(new C_Couple(c,vm));
			if(!(t1 == null)) cost += dc.getCosts()[e.getId()][s.getId()]*t1.floatValue();
			if(!(t2 == null)) cost += dc.getCosts()[s.getId()][e.getId()]*t2.floatValue();
		}
		conts = r.getNewContainers();
		
		for(Container c: conts) {
			Integer s= incumbent.getTable().get(c);
			if(!(s == null)) {
				Float t1 = r.getTraffic().get(new C_Couple(vm,c));
				Float t2 = r.getTraffic().get(new C_Couple(c,vm));
				if(!(t1 == null)) cost += dc.getCosts()[e.getId()][s.intValue()]*t1.floatValue();
				if(!(t2 == null)) cost += dc.getCosts()[s.intValue()][e.getId()]*t2.floatValue();
			}
		}
		
		return cost;
	}


}
