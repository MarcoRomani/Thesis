package cpp_heuristics;

import java.util.Set;
import java.util.TreeSet;

import general.Container;
import general.Pod;
import general.Rack;
import general.Server;

public class CPPOneSwitchStrictIter extends EfficientOneSwitchIter {

	@Override
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
	
		  Set<Rack> c_racks = new TreeSet<Rack>();
		  boolean flag = false;
		  for(Integer sv: c_serv) {
			  flag = false;
			  for(Pod p: dc.getPods()) {
				  if (flag == true) break;
				  if(p.containsServer(sv.intValue())) {
					  for(Rack r:p.getRacks()) {
						  if (flag == true) break;
						  if(r.containsServer(sv.intValue())) {
							  c_racks.add(r);
							  flag = true;
						  }
					  }
				  }
			  }
		  }
		  
		
		  for(Rack r: c_racks) {
				for(Server s: r.getHosts()) {
				
					if(s.isUnderUtilized()) {
						servs.add(stubs.get(s.getId()));
					}
				}
			}
		 // System.out.println(servs.size());
	}

	
}
