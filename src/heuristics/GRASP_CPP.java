package heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;

import general.*;

public class GRASP_CPP {

	
	private DataCenter dc;
	private ArrayList<Customer> req;
	private ArrayList<Customer> newcust;
	private ArrayList<Server> servers;
	private ArrayList<Server> s_u;
	private ArrayList<Server> s_u_compl;
	
	public GRASP_CPP(DataCenter dc, ArrayList<Customer> cust) {
		this.dc = dc;
		servers = new ArrayList<Server>();
		s_u = new ArrayList<Server>();
		s_u_compl = new ArrayList<Server>();
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					servers.add(s);
					if(s.getResidual_cpu() >= 0.4*s.getCpu()) {
						s_u.add(s);
					}
					else { s_u_compl.add(s); }
				}
			}
		}
		
		for(Customer c: cust) {
			if(c.getContainers().size() == 0) { newcust.add(c);}
			else { req.add(c);}
		}
		
		
	}
	
	public CPPSolution grasp(int maxIter, int seed, float alfa) {
		
		CPPSolution best = new CPPSolution();
		 

		for(int i=0; i<maxIter; i++) {
			
		    CPPSolution incumbent;
		    ArrayList<ServerStub> stubs = new ArrayList<ServerStub>();
		    ArrayList<ServerStub> stubs_u = new ArrayList<ServerStub>();
					for(Server s: s_u) {
						stubs_u.add(new ServerStub(s));
					}
					for(Server s: s_u_compl) {
						stubs.add(new ServerStub(s));
					}
			
			incumbent = this.greedy_rand_construction(seed, alfa, stubs_u, stubs);
			
			if(!(checkFeasibility(incumbent, stubs))) {
				this.repair(incumbent);
			}
			
			LocalSearch ls= new LocalSearch(Neighborhood n);
			incumbent = ls.search(incumbent);
			
			
			if(evaluate(incumbent) < best.getValue()) {
				best = incumbent;
			}
		}
		
		return best;
	}

	
	private float evaluate(CPPSolution sol) {
		
		float value = 0;
		ArrayList<Customer> custs = Customer.custList;
		
		for(Customer c: custs) {
			ArrayList<Container> conts = c.getContainers();
			ArrayList<Container> newconts = c.getNewContainers();
			for(Container c1: conts) {
				int s1 = dc.getPlacement().get(c1).getId();
				for(Container c2: newconts) {
					int s2 = sol.getTable().get(c2);
					if(c.getTraffic().get(new C_Couple(c1,c2)) != null) {
					    value += c.getTraffic().get(new C_Couple(c1,c2)).floatValue()*dc.getCosts()[s1][s2];
					}
					if(c.getTraffic().get(new C_Couple(c2,c1)) != null) {
				    	value += c.getTraffic().get(new C_Couple(c2,c1)).floatValue()*dc.getCosts()[s2][s1];
					}
				}
			}
			
			for(Container c1: newconts) {
				int s1 = sol.getTable().get(c1);
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
	
	
	
	
	private void repair(CPPSolution incumbent) {
		// TODO Auto-generated method stub
		
	}





	private boolean checkFeasibility(CPPSolution incumbent, ArrayList<ServerStub> stubs) {
		// TODO Auto-generated method stub
		
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





	private CPPSolution greedy_rand_construction(int seed, float alfa, ArrayList<ServerStub> stubs_u, ArrayList<ServerStub> stub) {
		// TODO Auto-generated method stub
		
		SecureRandom rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CPPSolution sol = new CPPSolution();
		ArrayList<Server> E = new ArrayList<Server>();
		for(Pod p:dc.getPods()) {
			for(Rack r: p.getRacks()) {
				E.addAll(r.getHosts()); // TO DO, stubs di s_u con abbastanza spazio
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
				costs.add(this.incrementalCost(vms.get(0), e, sol));
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
			
			sol.getTable().put(vms.get(0),RCL.get(rng.nextInt(RCL.size())).getId()); // NON VA BENE, FEASIBILITY CHECK
			vms.remove(0);
			
		}
		
		return sol;
	}


	/**
	 * 
	 * @param seed
	 * @param alfa
	 * @param stubs_u
	 * @param stubs   requires that these are all the stubs orderer by id, id must correspond to the position in the list
	 * @return
	 */

	private Result allnew_constr(int seed, float alfa, ArrayList<ServerStub> stubs_u, ArrayList<ServerStub> stubs) {
		
		SecureRandom rng = new SecureRandom();
		
		CPPSolution sol = new CPPSolution();
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
				if(s_u.contains(s)) {
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
	            	costs.add(new Float(1000 + (mem-estCap.get(i))));  // big M
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


	private Float incrementalCost(Container vm, Server e, CPPSolution incumbent) {
		float cost =0;
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
			Integer s= new Integer(incumbent.getTable().get(c));
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
