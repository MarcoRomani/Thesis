package heuristics;

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
			if(c.getNewContainers().size() != 0) { custs.add(c);}
		}
	}
	
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
		stubs.get(tmp2.intValue()).remove(conts.get(cont_index), stubs, sol, dc); // da rollbackare poco dopo
		sol.getTable().remove(conts.get(cont_index));
		
		if(stubs.get(tmp.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, false)) {
			CPPSolution nextSol = (CPPSolution)sol.clone();
			nextSol.getTable().put(conts.get(cont_index), tmp);
			stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback    
		    sol.getTable().put(conts.get(cont_index), tmp2);
		    return nextSol;
		}else {
		stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, sol, dc, true); // rollback       
	    sol.getTable().put(conts.get(cont_index), tmp2);
		//return (CPPSolution)sol.clone();
		// System.out.println("end next");

	    return sol;
		}
	}

	protected void updateCust() {
		servs.clear();
		conts = custs.get(cust_index).getNewContainers();
		ArrayList<Integer> c_serv = new ArrayList<Integer>();
		
		for(Container ct: conts) {
			c_serv.add(this.sol.getTable().get(ct));
		}
		for(Container ct: custs.get(cust_index).getContainers()) {
			c_serv.add(new Integer(this.dc.getPlacement().get(ct).getId()));
		}
		
	    Set<Pod> c_pods = new TreeSet<Pod>();
	    boolean flag = false; 
		for(Pod p:dc.getPods()) {      // breaks make sure that duplicate integers c_serv dont cause problems
			flag = false;
			for(Rack r: p.getRacks()) {
				if (flag == true) break;
				for(Server s:r.getHosts()) {
					if (flag == true) break;
					for(Integer i: c_serv) {
						if (flag == true) break;
						if(s.getId() == i.intValue()) {
							c_pods.add(p);
							flag = true;
						}
					}
				}
			}
		}
		
		for(Pod p: c_pods) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					for(ServerStub s_st: stubs_u) {
						if(s == s_st.getRealServ()) {
							servs.add(s_st);
							break;
						}
					}
				}
			}
		}
		// System.out.println(servs.size());
		//servs = stubs_u;
	}

	@Override
	public void setUp(DataCenter dc, ArrayList<ServerStub> stubs, ArrayList<ServerStub> stubs_u, CPPSolution sol) {
		this.dc = dc;
		this.stubs = stubs;
		this.stubs_u = stubs_u;
		
		cust_index = 0;
		cont_index = 0;
		serv_index = 0;
		ArrayList<Container> toSwitch = new ArrayList<Container>();
		for(Container vm: this.sol.getTable().keySet()) {
			if(this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
				toSwitch.add(vm);
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
		// TODO Auto-generated method stub
		conts = new ArrayList<Container>();
		servs = new ArrayList<ServerStub>();
		this.sol.getTable().clear();
		this.sol.setValue(Double.POSITIVE_INFINITY);
	}

	
	
}
