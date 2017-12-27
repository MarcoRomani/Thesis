package heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import general.*;

/**
 * 
 * @author Marco
 *
 * REQUIRES setstubs before starting exploration
 */
public class CPPOneSwapIter implements Iterator<CPPSolution>, My_Neighborhood {

	private DataCenter dc;
	private CPPSolution sol;
	private int index_one = 0;
	private int index_two = 0;
	private ArrayList<ServerStub> stubs;
	private ArrayList<Container> conts;
	
	public CPPOneSwapIter() {}
	
	@Override
	public boolean hasNext() {
		if(index_one + index_two >= 2*conts.size() -2) {
		   return false;
		}
		return true;
	}

	@Override
	public CPPSolution next() {
		
		index_two += 1;
		if(index_two >= conts.size()) { 
			index_two = 0;
			index_one += 1;
			if(index_one >= conts.size()) { throw new NoSuchElementException(); }
		}
		
		
		CPPSolution nextsol = (CPPSolution)sol.clone();
		substitute(conts.get(index_one),conts.get(index_two),nextsol);
		return nextsol;
		
	}

	
	private boolean substitute(Container c1, Container c2, CPPSolution incumbent) {
		ServerStub s1 = stubs.get(incumbent.getTable().get(c1).intValue());
		ServerStub s2 = stubs.get(incumbent.getTable().get(c2).intValue());
		if(s1.getId() == s2.getId()) { return true; }  // trivial case, no need to compute
		s1.remove(c1, stubs, incumbent, dc);
		incumbent.getTable().remove(c1);
		s2.remove(c2, stubs, incumbent, dc);
		incumbent.getTable().remove(c2);
		
		if(s1.allocate(c2, stubs, incumbent, dc, true)) {
			incumbent.getTable().put(c2, s1.getId());
			if(s2.allocate(c1, stubs, incumbent, dc, true)) {
				incumbent.getTable().put(c1, s1.getId());
				return true;
			}else {
				s1.remove(c2, stubs, incumbent, dc);
				incumbent.getTable().remove(c2);
				s1.allocate(c1, stubs, incumbent, dc, true);
				incumbent.getTable().put(c1, s1.getId());
				s2.allocate(c2, stubs, incumbent, dc, true);
				incumbent.getTable().put(c2, s2.getId());
				return false;
			}
		}else {
			s1.allocate(c1, stubs, incumbent, dc, true);
			incumbent.getTable().put(c1, s1.getId());
			s2.allocate(c2, stubs, incumbent, dc, true);
			incumbent.getTable().put(c2, s2.getId());
			return false;
		}
		
	}
	
	@Override
	public void setUp(DataCenter dc ,ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
		this.dc = dc;
        this.sol = (CPPSolution)sol.clone();		
		this.stubs = stubs;
	
	}

	
}
