package heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import general.*;
import general.Customer;
import general.DataCenter;

public class CPPOneSwitchIter implements Iterator<CPPSolution> {

	private CPPSolution sol;
	private DataCenter dc;
	private ArrayList<ServerStub> stubs;
	private int cust_index = 0;
	private int cont_index = 0;
	private int serv_index = 0;
	private ArrayList<Container> conts;
	private ArrayList<ServerStub> servs;
	
	
	public CPPOneSwitchIter(CPPSolution solution, DataCenter dc, ArrayList<ServerStub> stubs_u) {
		sol = (CPPSolution) solution.clone();
		this.dc = dc;
		this.stubs = stubs_u;
		updateCust();
		
	}
	
	@Override
	public boolean hasNext() {
		if(cust_index + cont_index + serv_index == Customer.custList.size() + conts.size() + servs.size() - 3) {
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
		
		  
		serv_index += 1;
		if(serv_index >= servs.size()) { serv_index = 0; cont_index += 1; }
		if(cont_index >= conts.size()) { 
			cont_index = 0; 
			cust_index += 1;
			if(cust_index == Customer.custList.size()) { throw new NoSuchElementException(); }
			updateCust();
		}
		
		CPPSolution nextSol = (CPPSolution)sol.clone();
		Integer tmp = new Integer(servs.get(serv_index).getId());
		Integer tmp2=  nextSol.getTable().get(conts.get(cont_index));
		
		if(tmp.intValue() == tmp2.intValue()) return nextSol;
		stubs.get(tmp2.intValue()).remove(conts.get(cont_index), stubs, nextSol, dc); // da rollbackare poco dopo
		if(stubs.get(tmp.intValue()).allocate(conts.get(cont_index), stubs, nextSol, dc, false)) {
			nextSol.getTable().replace(conts.get(cont_index), tmp);
		}else {
			// nothing
		}
		stubs.get(tmp2.intValue()).allocate(conts.get(cont_index), stubs, nextSol, dc, true); // rollback      STA ROBA SI PUO OTTIMIZZARE CON METODO A PARTE CHE CALCOLA IL DELTA
	  
		return nextSol;
	}

	private void updateCust() {
		conts = Customer.custList.get(cust_index).getNewContainers();
		ArrayList<Integer> c_serv = new ArrayList<Integer>();
		
		for(Container ct: conts) {
			c_serv.add(new Integer(this.sol.getTable().get(ct)));
		}
	    Set<Pod> c_pods = new TreeSet<Pod>();
	    boolean flag = false;
		for(Pod p:dc.getPods()) {
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
					for(ServerStub s_st: stubs) {
						if(s == s_st.getRealServ()) {
							servs.add(s_st);
							break;
						}
					}
				}
			}
		}
	}
}
