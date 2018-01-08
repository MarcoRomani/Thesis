package cpp_heuristics;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import general.DataCenter;

public class EfficientOneSwitchIter extends CPPOneSwitchIter {

	protected CPPSolution copy;
	protected Double deltacurrent;
	
	@Override
	public CPPSolution next() {
		// System.out.println("start next");
		serv_index += 1;
		if(serv_index >= servs.size()) { 
			stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()).allocate(conts.get(cont_index), stubs, copy, dc,true);
			copy.getTable().put(conts.get(cont_index),new Integer(sol.getTable().get(conts.get(cont_index)).intValue()));
			serv_index = 0; 
			cont_index += 1;
			if(cont_index < conts.size()) {
			  stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index), stubs, copy, dc);
			  copy.getTable().remove(conts.get(cont_index));
			  deltacurrent = deltaObj(conts.get(cont_index),stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()),copy,false);
			}
		}
		if(cont_index >= conts.size()) { 
			cont_index = 0; 
			cust_index += 1;
			if(cust_index >= custs.size()) { throw new NoSuchElementException(); }		
			
			updateCust();
			stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index), stubs, copy, dc);
			copy.getTable().remove(conts.get(cont_index));
			deltacurrent = deltaObj(conts.get(cont_index),stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()),copy,false);

		}
		
		if(serv_index >= servs.size()) return sol;
		
		Integer tmp = new Integer(servs.get(serv_index).getId());
		Integer tmp2=  sol.getTable().get(conts.get(cont_index));
		
		if(tmp.intValue() == tmp2.intValue()) return sol; //return (CPPSolution)sol.clone();
		
		double value = sol.getValue();
	//	stubs.get(tmp2.intValue()).remove(conts.get(cont_index), stubs, sol, dc); // da rollbackare poco dopo
	//	sol.getTable().remove(conts.get(cont_index));
		
	//	Double deltacurrent = deltaObj(conts.get(cont_index),stubs.get(tmp2.intValue()),copy,false);
		Double deltanext = deltaObj(conts.get(cont_index),stubs.get(tmp.intValue()),copy,true);
		
		if(deltanext.doubleValue() < deltacurrent.doubleValue()) {
			CPPSolution nextSol = (CPPSolution) copy.clone();
			nextSol.getTable().put(conts.get(cont_index), tmp);
			nextSol.setValue(value - deltacurrent.doubleValue() + deltanext.doubleValue());
			
		//	stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback       
		//    sol.getTable().put(conts.get(cont_index), tmp2);
			
			return nextSol;
			
		}
	//	stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback       
	 //   sol.getTable().put(conts.get(cont_index), tmp2);
		
	    return sol;
	}

	@Override
	public void setUp(DataCenter dc, ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
		super.setUp(dc, stubs, stubs_u, sol);
		copy =(CPPSolution) this.sol.clone();
		stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index), stubs, copy, dc);
		copy.getTable().remove(conts.get(cont_index));
		deltacurrent = deltaObj(conts.get(cont_index),stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()),copy,false);

	}

	

	@Override
	public boolean hasNext() {
		if(cust_index + cont_index + serv_index >= custs.size() + conts.size() + servs.size() - 3) {
			stubs.get(sol.getTable().get(conts.get(cont_index)).intValue()).allocate(conts.get(cont_index), stubs, copy, dc,true);
			copy.getTable().put(conts.get(cont_index),new Integer(sol.getTable().get(conts.get(cont_index)).intValue()));
			return false;
		}
		return true;
	}

}
