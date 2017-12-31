package heuristics;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import general.Container;
import general.Pod;
import general.Rack;
import general.Server;

public class CPPOneSwitchStrictIter extends CPPOneSwitchIter {

	@Override
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
	
		  Set<Rack> c_racks = new TreeSet<Rack>();
		  ArrayList<Rack> allracks = new ArrayList<Rack>();
		  for(Pod p : dc.getPods()) {
			  allracks.addAll(p.getRacks());
		  }
		  boolean flag = false; 
		  for(Rack r: allracks) {
				flag = false;
				for(Server s:r.getHosts()) {
					if (flag == true) break;
					for(Integer i: c_serv) {
						if (flag == true) break;
						if(s.getId() == i.intValue()) {
							c_racks.add(r);
							flag = true;
						}
					}
				}
			}
		  for(Rack r: c_racks) {
				for(Server s: r.getHosts()) {
					for(ServerStub s_st: stubs_u) {
						if(s == s_st.getRealServ()) {
							servs.add(s_st);
							break;
						}
					}
				}
			}
		 // System.out.println(servs.size());
	}

	
}
