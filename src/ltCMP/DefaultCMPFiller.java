package ltCMP;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import general.*;
import general.Customer;
import stCPP.DC_filler;
import stCPP.RackFiller;

public class DefaultCMPFiller implements CMPFiller{
	protected SecureRandom rng;

	public DefaultCMPFiller(SecureRandom rng) {
		this.rng = rng;
	}

	@Override
	public void populate(CMPDataCenter dc, List<Customer> app, float tolerance) {
		DC_filler fil = new RackFiller(rng);
		fil.populate(dc, app, tolerance);
		DefaultDirectedWeightedGraph<Node,Link> g = dc.getNetwork();
		DijkstraShortestPath<Node,Link> alg = new DijkstraShortestPath<Node,Link>(g);
		ArrayList<Server> servs = new ArrayList<Server>();
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					servs.add(s);
				}
			}
		}
		
		for(Server s: servs) {
			List<Container> vms = s.getContainers();
		    for(Container v : vms) {
		    	Customer r = Customer.custList.get(v.getMy_customer());
		    	for(Container c: r.getContainers()) {
		    		r.getTraffic().get(new C_Couple(v,c));
		    		GraphPath<Node,Link> path =alg.getPath(s, dc.getPlacement().get(c));
		    		dc.getPaths()
		    		// TREESET OF COMMUNICATING SERVERS, ASSOCIATE EACH ITS TO GLOBAL TRAFFIC, DO SHORTEST PATH ONE BY ONE 
		    	}
		    }
		}
		
	}

}
