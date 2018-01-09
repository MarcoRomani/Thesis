package cpp_heuristics;

import java.util.Set;
import java.util.TreeSet;

import general.Container;

public class CPPOneSwitchSmallIter extends CPPOneSwitchIter {

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
		
		for(Integer sv: c_serv) {
			if(stubs.get(sv.intValue()).getRealServ().isUnderUtilized()) {
				servs.add(stubs.get(sv.intValue()));
			}
		}
		
	}

	
}
