package cpp_heuristics;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Server;

public class EfficientOneSwapIter extends CPPOneSwapIter {

	protected CPPSolution copy;
	protected Double deltacurrent;
	
	@Override
	protected CPPSolution generateSolution() {
	
		
		Container c1 = conts.get(index_one);
		Container c2 = conts.get(index_two);
		Integer s1 = sol.getTable().get(c1);
		Integer s2 = sol.getTable().get(c2);
		
		if (c1 == c2 || s1.intValue() == s2.intValue()){
			return sol;
		}
		
		stubs.get(s2.intValue()).remove(c2, stubs, copy, dc);
		copy.getTable().remove(c2);
		Double deltacurrent_2 = deltaObj(c2,stubs.get(s2.intValue()),copy,false);
		Double deltanext_2 = deltaObj(c2,stubs.get(s1.intValue()),copy,true);
		if(deltanext_2.doubleValue() < Double.POSITIVE_INFINITY) {
			stubs.get(s1.intValue()).allocate(c2, stubs, copy, dc, true);
			copy.getTable().put(c2, s1);
			Double deltanext = deltaObj(c1,stubs.get(s2.intValue()),copy,true);
			stubs.get(s1.intValue()).remove(c2, stubs, copy, dc);
			copy.getTable().remove(c2);
			stubs.get(s2.intValue()).allocate(c2, stubs, copy, dc, true);
			copy.getTable().put(c2, s2);
		    if(deltanext.doubleValue()+deltanext_2.doubleValue() < deltacurrent.doubleValue()+deltacurrent_2.doubleValue()) {
		    	CPPSolution nextSol = (CPPSolution) copy.clone();
		    	nextSol.getTable().remove(c2);
		    	nextSol.getTable().put(c1, s2);
		    	nextSol.getTable().put(c2, s1);
		    	nextSol.setValue(sol.getValue()-deltacurrent.doubleValue()-deltacurrent_2.doubleValue()+
		    			deltanext.doubleValue()+deltanext_2.doubleValue());
		    	return nextSol;
		    }
		}else {
			
			stubs.get(s2.intValue()).allocate(c2, stubs, copy, dc, true);
			copy.getTable().put(c2, s2);
			
			
		}
		
		return sol;
		
	}

	protected Double deltaObj(Container vm, ServerStub e, CPPSolution incumbent, boolean b) {
		double cost =0;
		
		if(b && !(e.allocate(vm, stubs, incumbent, dc, false))) {
			cost = Double.POSITIVE_INFINITY;
			return new Double(cost);
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
	public boolean hasNext() {
		if(index_one + index_two >= 2*conts.size() -3) {
				stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one), stubs, copy, dc, true);
				copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));
			    return false;
			}
			return true;
	}

	@Override
	public void setUp(DataCenter dc, ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
	
		super.setUp(dc, stubs, stubs_u, sol);
		copy = (CPPSolution)this.sol.clone();
		stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs, copy, dc);
        copy.getTable().remove(conts.get(index_one));
        deltacurrent = deltaObj(conts.get(index_one),stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()),copy,false);
	}

	@Override
	public CPPSolution next() {
		index_two += 1;
		if(index_two >= conts.size()) { 
			stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one), stubs, copy, dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));
			
			index_one += 1;
			index_two = index_one+1;
			
			if(index_one >= conts.size()) { throw new NoSuchElementException(); }
			
			stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs, copy, dc);
            copy.getTable().remove(conts.get(index_one));
            deltacurrent = deltaObj(conts.get(index_one),stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()),copy,false);

		}
		
		
		return generateSolution();
	}
}
