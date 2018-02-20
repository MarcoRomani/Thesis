package general;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.*;



public class CMPDataCenter extends DataCenter {

	
	protected DefaultDirectedWeightedGraph<Node,Link> network;
	
	protected HashMap<S_Couple,List<Link>> paths;
	
	public CMPDataCenter(String topology, int size) {     // TODO GESTIONE DEI COST, SHOULD BE INPUT
		super(topology, size);
		buildGraph();
		int serv = (size*size*size)/4;
		paths = new HashMap<S_Couple, List<Link>>();
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
		
		for(Pod p: pods) {
			int tmp =0;
			for(Switch ag: p.getAggregation()) {
				for(int i=tmp; i<tmp+ pods.size()/2;i++) {
					network.addEdge(ag, p.getCore().get(i), new Link(ag,p.getCore().get(i),10));
					network.addEdge(p.getCore().get(i), ag, new Link(p.getCore().get(i),ag,10));
				}
				tmp += pods.size()/2;
			}
			for(Switch sw: p.getEdge()) {
				for(Switch sw2: p.getAggregation()) {
					network.addEdge(sw, sw2, new Link(sw,sw2,10));
					network.addEdge(sw2, sw, new Link(sw2,sw,10));
				}
			}
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					network.addEdge(s, r.getSwitches().get(0), new Link(s,r.getSwitches().get(0),10));
					network.addEdge(r.getSwitches().get(0),s, new Link(r.getSwitches().get(0),s,10));

				}
			}
		}
	}
}
