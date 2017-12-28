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
	private CPPSolution sol = new CPPSolution();
	private int index_one = 0;
	private int index_two = 0;
	private ArrayList<ServerStub> stubs;
	private ArrayList<Container> conts = new ArrayList<Container>();;
	
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
		
		
		//CPPSolution nextsol = (CPPSolution)sol.clone();
		swap(conts.get(index_one),conts.get(index_two),sol);
		CPPSolution nextSol = (CPPSolution)sol.clone();
		swap(conts.get(index_two),conts.get(index_one),sol);
		return nextSol;
		
	}

	
	private boolean swap(Container c1, Container c2, CPPSolution incumbent) {
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
	//	System.out.println(this.sol.toString());
	//	System.out.println(sol.toString());
		ArrayList<Container> toSwap = new ArrayList<Container>();
		for(Container vm: conts) {
       		if(this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
       			/*System.out.println("correggere: "+vm.getId()+" - "+this.sol.getTable().get(vm).intValue()+" - "+sol.getTable().get(vm).intValue());
       			this.stubs.get(this.sol.getTable().get(vm).intValue()).remove(vm, stubs, this.sol, dc);
       			this.sol.getTable().remove(vm);
       			this.stubs.get(sol.getTable().get(vm).intValue()).allocate(vm, stubs, this.sol, dc, true);*/
       			toSwap.add(vm);
       		}
       	}
		if(toSwap.size() == 2) swap(toSwap.get(0),toSwap.get(1),this.sol);
		
		this.sol = (CPPSolution)sol.clone();	
		
		if(conts.size() != sol.getTable().size()) {
        	conts = new ArrayList<Container>();
		    conts.addAll(sol.getTable().keySet());
       	}
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		conts = new ArrayList<Container>();
	}

	
}
