package heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import general.*;

public class GRASP_CPP {

	
	private DataCenter dc;
	private ArrayList<Customer> req;
	private ArrayList<Customer> newcust;
	private ArrayList<Server> servers;
	private ArrayList<Server> s_u;
	
	public GRASP_CPP(DataCenter dc, ArrayList<Customer> cust) {
		this.dc = dc;
		servers = new ArrayList<Server>();
		s_u = new ArrayList<Server>();
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					servers.add(s);
					if(s.getResidual_cpu() >= 0.4*s.getCpu()) {
						s_u.add(s);
					}
				}
			}
		}
		
		for(Customer c: cust) {
			if(c.getContainers().size() == 0) { newcust.add(c);}
			else { req.add(c);}
		}
		
		
	}
	
	public CPPSolution grasp(int maxIter, int seed, float alfa) {
		
		CPPSolution best;
		 

		for(int i=0; i<maxIter; i++) {
			
		    CPPSolution incumbent;
		    ArrayList<ServerStub> stubs = new ArrayList<ServerStub>();
					for(Server s: s_u) {
						stubs.add(new ServerStub(s));
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





	private CPPSolution greedy_rand_construction(int seed, float alfa, ArrayList<ServerStub> stubs) {
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
			
			sol.getTable().put(vms.get(0),RCL.get(rng.nextInt(RCL.size())).getId());
			vms.remove(0);
			
		}
		
		return sol;
	}



	private CPPSolution allnew_constr(int seed, float alfa, ArrayList<ServerStub> stubs) {
		
		CPPSolution sol = new CPPSolution();
		ArrayList<Rack> racks = new ArrayList<Rack>();
		for(Pod p: dc.getPods()) {
			racks.addAll(p.getRacks());
		}
		
		ArrayList<Float> estCap = new ArrayList<Float>();
		int count = 0;
		for(int i=0;i < racks.size(); i++) {
			estCap.add(new Float(0));
			for(Server s: racks.get(i).getHosts()) {
				if(s_u.contains(s)) {
					estCap.set(i, new Float(estCap.get(i).floatValue() + stubs.get(count).getRes_mem()));
					count +=1;
				}
				
			}
		}

		ArrayList<Container> vms = new ArrayList<Container>();
		
		for(Customer c: newcust) {
			vms = c.getNewContainers();
			
			// TODO piazzare cont nei rack
			// pensare meglio a indicizzazione stub w.r.t server
			
		}
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
