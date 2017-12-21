package heuristics;

import java.util.ArrayList;

import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Server;

public abstract class GRASP_CPP_Scheme {

	protected DataCenter dc;
	protected ArrayList<Customer> req = new ArrayList<Customer>();
	protected ArrayList<Customer> newcust = new ArrayList<Customer>();
	protected ArrayList<Server> servers;
	protected ArrayList<Server> s_u;
	protected ArrayList<Server> s_u_compl;
	
	
	public abstract CPPSolution grasp(int maxIter, int seed, float alfa); 
	
	protected abstract CPPSolution greedy_rand_construction(float alfa, ArrayList<ServerStub> stubs_u, ArrayList<ServerStub> stubs) throws InfeasibilityException;
	
	protected abstract Float incrementalCost(Container vm, ServerStub e, ArrayList<ServerStub> stubs, CPPSolution incumbent);
	
	protected abstract CPPSolution localSearch(CPPSolution init_sol, ArrayList<ServerStub> stubs_u, ArrayList<ServerStub> stubs);
	
	protected abstract void repair(CPPSolution incumbent);
	
	
	
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
