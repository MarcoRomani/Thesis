package cpp_heuristics;

import java.util.NoSuchElementException;

public class EfficientOneSwitchIter extends CPPOneSwitchIter {

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

}
