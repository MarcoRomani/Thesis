package FirstFit;
import java.util.ArrayList;
import java.util.List;

import cpp_heuristics.*;
import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;
public class FirstFitHeur {

	protected DataCenter dc;
	protected List<ServerStub> stubs = new ArrayList<ServerStub>();
	protected int violations = 0;
	
	public FirstFitHeur(DataCenter dc) {
		
		this.dc = dc;
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ServerStub tmp = new ServerStub(s);
					stubs.add(tmp);
					
				}
			}
		}
	}
	
	public CPPSolution findSolution(List<Container> toPlace) {
		CPPSolution sol = new CPPSolution();
		
		for(Container c: toPlace) {
			for(ServerStub st : stubs) {
				if(st.getRes_mem() >= c.getMem() && st.getRes_disk() >= c.getDisk() && st.getRes_cpu() >= CPUcalculator.utilization(c, st.getRealServ()) ) {
					st.forceAllocation(c, stubs, sol, dc);
					sol.getTable().put(c, new Integer(st.getId()));
					break;
				}
			}
		}
		
		violations = countViolations();
		
		evaluate(sol);
		return sol;
	}
	
	private int countViolations() {
		int count = 0;
		for(ServerStub st : stubs) {
			if(st.getRes_cpu() < 0 || st.getRes_mem() <0 || st.getRes_disk() <0 || st.getRes_disk() < 0 || st.getRes_in() <0 || st.getRes_out() <0 ) {
				count += 1;
			}
		}
		return count;
	}

	protected double evaluate(CPPSolution sol) {

		if (sol.getValue() < Double.POSITIVE_INFINITY)
			return sol.getValue(); // lazy
		
		double value = 0;
		List<Customer> custs = Customer.custList;

		for (Customer c : custs) {
			List<Container> conts = c.getContainers();
			List<Container> newconts = c.getNewContainers();
			// old-new and new-old
			for (Container c1 : conts) {
				int s1 = dc.getPlacement().get(c1).getId();
				for (Container c2 : newconts) {
					int s2 = sol.getTable().get(c2).intValue();
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue() * dc.getCosts()[s1][s2];
					}
					if (c.getTraffic().get(new C_Couple(c2, c1)) != null) {
						value += c.getTraffic().get(new C_Couple(c2, c1)).doubleValue() * dc.getCosts()[s2][s1];
					}
				}
			}
			// new-new
			for (Container c1 : newconts) {
				int s1 = sol.getTable().get(c1).intValue();
				for (Container c2 : newconts) {
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue()
								* dc.getCosts()[s1][sol.getTable().get(c2).intValue()];
					}
				}
			}

		}
		sol.setValue(value);
		return value;
	}
	
	public int getViolations() {
		return violations;
		
	}
	
	
}
