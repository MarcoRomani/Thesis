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
	protected static double inv_offset = 0.0001;
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
			double to_t0 = 0;
			double from_s0 = 0;
			
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
				
				Double c_c0 = r.getTraffic().get(new C_Couple(v,Container.c_0));
				to_t0 += (c_c0 == null)? 0 : c_c0.doubleValue();
				Double c0_c = r.getTraffic().get(new C_Couple(Container.c_0, v));
				from_s0 += (c0_c == null)? 0 : c0_c.doubleValue(); 
			}
			
			
			// UPDATE LINKS AND PATHS
			
			// TRAFFIC WITH C_0
			GraphPath<Node,Link> path_0 =alg.getPath(s, dc.t_0);
			for(Link l : path_0.getEdgeList()) {
				if(l.getResidCapacity() == Double.POSITIVE_INFINITY) continue;
				l.setResidCapacity(l.getResidCapacity() - to_t0);
		//		System.out.println(l.getResCapacity());
				g.setEdgeWeight(l, 1/(l.getResidCapacity() + inv_offset ));
			}
			dc.getTo_wan().remove(s);
			dc.getTo_wan().put(s, path_0.getEdgeList());
			
			path_0 = alg.getPath(dc.s_0, s);
			for(Link l : path_0.getEdgeList()) {
				if(l.getResidCapacity() == Double.POSITIVE_INFINITY) continue;
				l.setResidCapacity(l.getResidCapacity() - from_s0);
			//	System.out.println(l.getResCapacity());
				g.setEdgeWeight(l, 1/(l.getResidCapacity() + inv_offset ));
				
			}
			dc.getFrom_wan().remove(s);
			dc.getFrom_wan().put(s, path_0.getEdgeList());
			
			// OTHER TRAFFIC
		    for(Server t : set.keySet()) {
		    	
		    		GraphPath<Node,Link> path =alg.getPath(s, t);
		    		for(Link l : path.getEdgeList()) {
		    			l.setResidCapacity(l.getResidCapacity() - set.get(t).doubleValue());
		    			g.setEdgeWeight(l, 1/(l.getResidCapacity() + inv_offset ));
		    		}
		    		dc.getPaths().remove(new S_Couple(s,t));
		    		dc.getPaths().put(new S_Couple(s,t), path.getEdgeList());
		    		dc.getCosts()[s.getId()][t.getId()] = path.getEdgeList().size()-1;
		    	
		    }
		    
		    
		}/*
		for(Link l : dc.getNetwork().edgeSet()) {
			System.out.println(l.getResCapacity());
		}*/
		
	}

}
