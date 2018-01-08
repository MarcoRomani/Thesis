package cpp_heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import general.*;

/**
 * 
 * @author Marco
 *
 * REQUIRES setstubs before starting exploration
 */

public class CPPOneSwitchIter implements Iterator<CPPSolution>, My_Neighborhood{

	protected CPPSolution sol = new CPPSolution();
	protected DataCenter dc;
	protected ArrayList<ServerStub> stubs;
	protected ArrayList<ServerStub> stubs_u;
	protected int cust_index = 0;
	protected int cont_index = 0;
	protected int serv_index = 0;
	protected ArrayList<Container> conts = new ArrayList<Container>();
	protected ArrayList<ServerStub> servs = new ArrayList<ServerStub>();
	protected ArrayList<Customer> custs= new ArrayList<Customer>();
	
	public CPPOneSwitchIter() {
		for(Customer c: Customer.custList) {
			if(c.getNewContainers().size() > 0) { custs.add(c);}
		}
	}
	@Deprecated
	@Override
	public boolean hasNext() {
		if(cust_index + cont_index + serv_index >= custs.size() + conts.size() + servs.size() - 3) {
			return false;
		}
		return true;
	}

	/**
	 *  if next solution is feasible, return it
	 *  o.w. return the current solution
	 * 
	 */
	@Deprecated
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
		
		
		
		CPPSolution nextSol = (CPPSolution) sol.clone();
		double value = nextSol.getValue();
		stubs.get(tmp2.intValue()).remove(conts.get(cont_index), stubs, nextSol, dc); // da rollbackare poco dopo
		nextSol.getTable().remove(conts.get(cont_index));
		
		Double deltacurrent = deltaObj(conts.get(cont_index),stubs.get(tmp2.intValue()),nextSol,false);
		Double deltanext = deltaObj(conts.get(cont_index),stubs.get(tmp.intValue()),nextSol,true);
		
		stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, nextSol, dc,true); // rollback
		nextSol.getTable().put(conts.get(cont_index), tmp);
		nextSol.setValue(value - deltacurrent.doubleValue() + deltanext.doubleValue());
		
		
		return nextSol;
			
	}
	

	protected void updateCust() {
		servs.clear();
		conts = custs.get(cust_index).getNewContainers();
		Set<Integer> c_serv = new TreeSet<Integer>();
		
		for(Container ct: conts) {
			c_serv.add(this.sol.getTable().get(ct));
		}
		for(Container ct: custs.get(cust_index).getContainers()) {
			c_serv.add(new Integer(this.dc.getPlacement().get(ct).getId()));
		}
		
	    Set<Pod> c_pods = new TreeSet<Pod>();
	    boolean flag = false;
		  for(Integer sv: c_serv) {
			  flag = false;
			  for(Pod p: dc.getPods()) {
				  if (flag == true) break;
				  if(p.containsServer(sv.intValue())) {
					  c_pods.add(p);
					  flag = true;
				  }
			  }
		  }
		
		for(Pod p: c_pods) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					
					if(s.isUnderUtilized()) {
						servs.add(stubs.get(s.getId()));
					}
				}
			}
		}
		// System.out.println(servs.size());
		
	}

	@Deprecated
	@Override
	public void setUp(DataCenter dc, ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
		this.dc = dc;
		this.stubs = stubs;
		this.stubs_u = stubs_u;
		
		cust_index = 0;
		cont_index = 0;
		serv_index = -1;
		ArrayList<Container> toSwitch = new ArrayList<Container>();
		for(Container vm: this.sol.getTable().keySet()) {
			if(this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
				toSwitch.add(vm);
				//System.out.println("da correggere");
			}
		}
		
		// remove phase
		for(Container v: toSwitch) {
			stubs.get(this.sol.getTable().get(v).intValue()).remove(v, stubs, this.sol, dc);
			this.sol.getTable().remove(v);
		}// allocate phase
		for(Container v: toSwitch) {
			int tmp =sol.getTable().get(v).intValue();
			stubs.get(tmp).allocate(v, stubs, this.sol, dc, true);
			this.sol.getTable().put(v, new Integer(tmp));
		}
		this.sol =(CPPSolution) sol.clone();
		updateCust();
		
	}

	@Override
	public void clear() {
		conts = new ArrayList<Container>();
		servs = new ArrayList<ServerStub>();
		this.sol.getTable().clear();
		this.sol.setValue(Double.POSITIVE_INFINITY);
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
	
	
}
