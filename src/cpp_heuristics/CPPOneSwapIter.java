package cpp_heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import general.*;

/**
 * 
 * @author Marco
 *
 * REQUIRES setstubs before starting exploration
 */
public class CPPOneSwapIter implements Iterator<CPPSolution>, My_Neighborhood {

	protected DataCenter dc;
	protected CPPSolution sol = new CPPSolution();
	protected int index_one = 0;
	protected int index_two = 0;
	protected List<ServerStub> stubs;
	protected List<Container> conts = new ArrayList<Container>();
	
	public CPPOneSwapIter() {}
	
	@Override
	public boolean hasNext() {
		if(index_one + index_two >= 2*conts.size() -3) {
		   return false;
		}
		return true;
	}

	@Deprecated
	@Override
	public CPPSolution next() {
		
		index_two += 1;
		if(index_two >= conts.size()) { 
			
			index_one += 1;
			index_two = index_one+1;
			if(index_one >= conts.size()) { throw new NoSuchElementException(); }
		}
		
		
		return generateSolution();
	}

	@Deprecated
	protected CPPSolution generateSolution() {
		if(swap(conts.get(index_one),conts.get(index_two),sol)) {
			CPPSolution nextSol = (CPPSolution)sol.clone();
			nextSol.setValue(Double.POSITIVE_INFINITY);
			swap(conts.get(index_two),conts.get(index_one),sol);
			return nextSol;
		}
		
		swap(conts.get(index_two),conts.get(index_one),sol);
		return sol;
	}
	
	@Deprecated
	protected boolean swap(Container c1, Container c2, CPPSolution incumbent) {
		// System.out.println("swap "+c1.getId()+" - "+c2.getId());
		ServerStub s1 = stubs.get(incumbent.getTable().get(c1).intValue());
		ServerStub s2 = stubs.get(incumbent.getTable().get(c2).intValue());
		if(s1.getId() == s2.getId()) { return true; }  // trivial case, no need to compute
		s1.remove(c1, stubs, incumbent, dc);
		incumbent.getTable().remove(c1);
		s2.remove(c2, stubs, incumbent, dc);
		incumbent.getTable().remove(c2);
		
		if(s1.allocate(c2, stubs, incumbent, dc, true)) {
			incumbent.getTable().put(c2, new Integer(s1.getId()));
			if(s2.allocate(c1, stubs, incumbent, dc, true)) {
				incumbent.getTable().put(c1, new Integer(s2.getId()));
				return true;
			}else {
				s1.remove(c2, stubs, incumbent, dc);
				incumbent.getTable().remove(c2);
				s1.allocate(c1, stubs, incumbent, dc, true);
				incumbent.getTable().put(c1, new Integer(s1.getId()));
				s2.allocate(c2, stubs, incumbent, dc, true);
				incumbent.getTable().put(c2, new Integer(s2.getId()));
				return false;
			}
		}else {
			s1.allocate(c1, stubs, incumbent, dc, true);
			incumbent.getTable().put(c1, new Integer(s1.getId()));
			s2.allocate(c2, stubs, incumbent, dc, true);
			incumbent.getTable().put(c2, new Integer(s2.getId()));
			return false;
		}
		
	}
	
	@Override
	public void setUp(DataCenter dc ,ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
		//System.out.println("set up");
		this.dc = dc;
       	this.stubs = stubs;
		
       	
		index_one = 0;
		index_two = 0;
	
		ArrayList<Container> toSwap = new ArrayList<Container>();
		for(Container vm: conts) {
       		if(this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
       		
       			toSwap.add(vm);
       		}
       	}
		
		// remove phase
		for(Container v: toSwap) {
			stubs.get(this.sol.getTable().get(v).intValue()).remove(v, stubs, this.sol, dc);
			this.sol.getTable().remove(v);
		}// allocate phase
		for(Container v: toSwap) {
			int tmp =sol.getTable().get(v).intValue();
			stubs.get(tmp).allocate(v, stubs, this.sol, dc, true);
			this.sol.getTable().put(v, new Integer(tmp));
		}
		
		this.sol = (CPPSolution)sol.clone();	
		
		if(conts.size() != sol.getTable().size()) {
        	conts = new ArrayList<Container>();
		    conts.addAll(sol.getTable().keySet());
       	}
	}

	@Override
	public void clear() {
		conts = new ArrayList<Container>();
		
		// could reset solution but is not necessary
	}

	
}
