package stCPP;

import java.util.ArrayList;
import java.util.List;

import general.Container;
import general.Customer;
import general.DataCenter;
import general.Pod;
import general.Server;
import general.Rack;

public class FirstFit implements DC_filler {

	
	
	
	@Override
	public void populate(DataCenter dc, List<Customer> req, float tolerance) {

		ArrayList<Server> machines = new ArrayList<Server>();
		for(Pod p : dc.getPods()) {
			for(Rack r : p.getRacks()) {
				machines.addAll(r.getHosts());
			}
		}
		
	   ArrayList<Container> vms = new ArrayList<Container>();
	   for(Customer c : req) {
		   vms.addAll(c.getWeb_servers());
		   vms.addAll(c.getApp_servers());
		   vms.addAll(c.getDbms());
	   }
	
	int index = 0;
	for(Container v : vms) {
		if(machines.get(index).canBePlaced(v, tolerance)) {
			machines.get(index).allocateContainer(v);			
		}else {
			index += 1;
			machines.get(index).allocateContainer(v);
		}
		dc.getPlacement().put(v, machines.get(index));
		machines.get(index).updateBandwidth();
	}

	for(Server s: machines) {
		s.updateBandwidth();
	}
	}
}
