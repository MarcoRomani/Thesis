package general;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.*;



public class CMPDataCenter extends DataCenter {

	public static double inv_offset = 0.01;
	public Node s_0;
	public Node t_0;
	
	protected DefaultDirectedWeightedGraph<Node,Link> network;
	
	protected HashMap<S_Couple,List<Link>> paths;
	protected HashMap<Server, List<Link>> to_wan;
	protected HashMap<Server, List<Link>> from_wan;
	
	public CMPDataCenter(String topology, int size) {     // TODO GESTIONE DEI COST, SHOULD BE INPUT
		super(topology, size);
		buildGraph();
		int serv = (size*size*size)/4;
		paths = new HashMap<S_Couple, List<Link>>();
		to_wan = new HashMap<Server, List<Link>>();
		from_wan = new HashMap<Server, List<Link>>();
	}

	public Map<S_Couple, List<Link>> getPaths(){
		return paths;
	}
	
	public DefaultDirectedWeightedGraph<Node,Link> getNetwork() {
		return network;
	}
	
	public Server findServer(int id) {
		for(Pod p: pods) {
			if(id > p.getLower_index()) continue;
			for(Rack r:p.getRacks()) {
				if(id > r.getLower_index()) continue;
				for(Server s : r.getHosts()) {
					if(s.getId() == id) return s;
				}
			}
		}
		return null;
	}
	
	protected void buildGraph() {
		network = new DefaultDirectedWeightedGraph<Node,Link> (Link.class);
		network.addVertex(s_0);
		network.addVertex(t_0);
		
		for(Pod p: pods) {
			for(Switch sw: p.getCore()) {
				network.addVertex(sw);
			}
			for(Switch sw: p.getAggregation()) {
				network.addVertex(sw);
			}
			for(Switch sw: p.getEdge()) {
				network.addVertex(sw);
			}
			
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					network.addVertex(s);
				}
			}
		}
		
		for(Node n : this.core) {
			network.addEdge(n, t_0, new Link(n, t_0, Double.POSITIVE_INFINITY));
			network.addEdge(s_0, n, new Link(s_0, n, Double.POSITIVE_INFINITY));
		}
		
		for(Pod p: pods) {
			int tmp =0;
			for(Switch ag: p.getAggregation()) {
				for(int i=tmp; i<tmp+ pods.size()/2;i++) {
					Link l =new Link(ag,p.getCore().get(i),10);
					network.addEdge(ag, p.getCore().get(i),l );
					network.setEdgeWeight(l, 1/(l.getResCapacity()+ inv_offset));
					
					Link k = new Link(p.getCore().get(i),ag,10);
					network.addEdge(p.getCore().get(i), ag, k);
					network.setEdgeWeight(k, 1/(k.getResCapacity()+ inv_offset));
				}
				tmp += pods.size()/2;
			}
			for(Switch sw: p.getEdge()) {
				for(Switch sw2: p.getAggregation()) {
					Link l = new Link(sw,sw2,10);
					network.addEdge(sw, sw2, l);
					network.setEdgeWeight(l, 1/(l.getResCapacity()+ inv_offset));
					
					Link k = new Link(sw2,sw,10);
					network.addEdge(sw2, sw, k);
					network.setEdgeWeight(k, 1/(k.getResCapacity()+ inv_offset));
					
				}
			}
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					Link l =  new Link(s,r.getSwitches().get(0),10);
					network.addEdge(s, r.getSwitches().get(0), l);
					network.setEdgeWeight(l, 1/(l.getResCapacity()+ inv_offset));
					
					Link k = new Link(r.getSwitches().get(0),s,10);
					network.addEdge(r.getSwitches().get(0),s, k);
					network.setEdgeWeight(k, 1/(k.getResCapacity()+ inv_offset));

				}
			}
		}
	}

	public HashMap<Server, List<Link>> getTo_wan() {
		return to_wan;
	}

	public HashMap<Server, List<Link>> getFrom_wan() {
		return from_wan;
	}
}
