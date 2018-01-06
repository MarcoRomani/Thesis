package cpp_heuristics;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import general.C_Couple;
import general.Container;
import general.Customer;
import general.Server;

public class EfficientOneSwitchStrictIter extends CPPOneSwitchStrictIter {

	@Override
	public CPPSolution next() {
		// System.out.println("start next");
				serv_index += 1;
				if(serv_index >= servs.size()) { serv_index = 0; cont_index += 1; }
				if(cont_index >= conts.size()) { 
					cont_index = 0; 
					cust_index += 1;
					if(cust_index >= custs.size()) { throw new NoSuchElementException(); }
					updateCust();
				}
				
				if(serv_index >= servs.size()) return sol;
				
				Integer tmp = new Integer(servs.get(serv_index).getId());
				Integer tmp2=  sol.getTable().get(conts.get(cont_index));
				
				if(tmp.intValue() == tmp2.intValue()) return sol; //return (CPPSolution)sol.clone();
				
				double value = sol.getValue();
				stubs.get(tmp2.intValue()).remove(conts.get(cont_index), stubs, sol, dc); // da rollbackare poco dopo
				sol.getTable().remove(conts.get(cont_index));
				
				Double deltacurrent = deltaObj(conts.get(cont_index),stubs.get(tmp2.intValue()),sol,false);
				Double deltanext = deltaObj(conts.get(cont_index),stubs.get(tmp.intValue()),sol,true);
				
				if(deltanext.doubleValue() < deltacurrent.doubleValue()) {
					CPPSolution nextSol = (CPPSolution) sol.clone();
					nextSol.getTable().put(conts.get(cont_index), tmp);
					nextSol.setValue(value - deltacurrent + deltanext);
					
					stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback       
				    sol.getTable().put(conts.get(cont_index), tmp2);
					
					return nextSol;
					
				}
				stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback       
			    sol.getTable().put(conts.get(cont_index), tmp2);
				
			    return sol;
	}

	
	
	
	
	
	protected Double deltaObj(Container vm, ServerStub e, CPPSolution incumbent, boolean b) {
		double cost =0;
		
		if(b && !(e.allocate(vm, stubs, incumbent, dc, false))) {
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
}
