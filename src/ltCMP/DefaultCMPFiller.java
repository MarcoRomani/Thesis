package ltCMP;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import general.*;
import general.Customer;
import stCPP.DC_filler;
import stCPP.RackFiller;

public class DefaultCMPFiller implements CMPFiller{
	protected static double inv_offset = 0.01;
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
			HashMap <Server, Double> set = new HashMap<Server, Double>();
			for(Container v:vms) {
				Customer r = Customer.custList.get(v.getMy_customer());
				for(Container c:r.getContainers()) {
					Double t = r.getTraffic().get(new C_Couple(v,c));
					if(t != null) {
						Double tmp = set.get(dc.getPlacement().get(c));
						double _tmp = (tmp == null)? 0 : tmp.doubleValue();
						set.put(dc.getPlacement().get(c),_tmp + t.doubleValue() );
					}
				}
			}
			
		    for(Server t : set.keySet()) {
		    	
		    		GraphPath<Node,Link> path =alg.getPath(s, t);
		    		for(Link l : path.getEdgeList()) {
		    			l.setResCapacity(l.getResCapacity() - set.get(t).doubleValue());
		    			g.setEdgeWeight(l, 1/(l.getResCapacity() + inv_offset ));
		    		}
		    		dc.getPaths().replace(new S_Couple(s,t), path.getEdgeList());
		    		dc.getCosts()[s.getId()][t.getId()] = path.getEdgeList().size()-1;
		    	
		    }
		}
		
	}

}
