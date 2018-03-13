package FirstFit;
import java.util.ArrayList;
import java.util.List;

import cpp_heuristics.*;
import general.CPUcalculator;
import general.Container;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;
public class FirstFitHeur {

	protected DataCenter dc;
	protected List<ServerStub> stubs = new ArrayList<ServerStub>();
	
	public FirstFitHeur(DataCenter dc) {
		
		this.dc = dc;
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ServerStub tmp = new ServerStub(s);
					stubs.add(tmp);
					
				}
			}
		}
	}
	
	public CPPSolution findSolution(List<Container> toPlace) {
		CPPSolution sol = new CPPSolution();
		
		for(Container c: toPlace) {
			for(ServerStub st : stubs) {
				if(st.getRes_mem() >= c.getMem() && st.getRes_disk() >= c.getDisk() && st.getRes_cpu() >= CPUcalculator.utilization(c, st.getRealServ()) ) {
					st.forceAllocation(c, stubs, sol, dc);
					sol.getTable().put(c, new Integer(st.getId()));
					break;
				}
			}
		}
		
		
		
		return sol;
	}
}
