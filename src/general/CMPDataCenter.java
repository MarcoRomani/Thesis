package general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;



public class CMPDataCenter extends DataCenter {

	public static double inv_offset = 0.000001;
	public Node s_0 = new Node();
	public Node t_0 = new Node();
	
	protected DefaultDirectedWeightedGraph<Node,Link> network;
	
	protected HashMap<S_Couple,List<Link>> paths;
	protected HashMap<Server, List<Link>> to_wan;
	protected HashMap<Server, List<Link>> from_wan;
	
	public CMPDataCenter(String topology, int size) {     
		super(topology, size);
		
	//	int serv = (size*size*size)/4;
		paths = new HashMap<S_Couple, List<Link>>();
		to_wan = new HashMap<Server, List<Link>>();
		from_wan = new HashMap<Server, List<Link>>();
		buildGraph();
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
		
		List<Server> servers = new ArrayList<Server>();
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
					servers.add(s);
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
					network.setEdgeWeight(l, 1/(l.getResidCapacity()+ inv_offset));
					
					Link k = new Link(p.getCore().get(i),ag,10);
					network.addEdge(p.getCore().get(i), ag, k);
					network.setEdgeWeight(k, 1/(k.getResidCapacity()+ inv_offset));
				}
				tmp += pods.size()/2;
			}
			for(Switch sw: p.getEdge()) {
				for(Switch sw2: p.getAggregation()) {
					Link l = new Link(sw,sw2,10);
					network.addEdge(sw, sw2, l);
					network.setEdgeWeight(l, 1/(l.getResidCapacity()+ inv_offset));
					
					Link k = new Link(sw2,sw,10);
					network.addEdge(sw2, sw, k);
					network.setEdgeWeight(k, 1/(k.getResidCapacity()+ inv_offset));
					
				}
			}
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					Link l =  new Link(s,r.getSwitches().get(0),10);
					network.addEdge(s, r.getSwitches().get(0), l);
					network.setEdgeWeight(l, 1/(l.getResidCapacity()+ inv_offset));
					
					Link k = new Link(r.getSwitches().get(0),s,10);
					network.addEdge(r.getSwitches().get(0),s, k);
					network.setEdgeWeight(k, 1/(k.getResidCapacity()+ inv_offset));

				}
			}
		}
		
		DijkstraShortestPath<Node,Link> alg = new DijkstraShortestPath<Node,Link>(network);
		for(Server s1 : servers) {
			SingleSourcePaths<Node, Link> lpaths =alg.getPaths(s1);
			for(Server s2 : servers) {
				if(s1 == s2) {
					paths.put(new S_Couple(s1,s2), new ArrayList<Link>());
					costs[s1.getId()][s2.getId()] = 0;
					continue;
				}
				GraphPath<Node,Link> path = lpaths.getPath(s2);
				this.paths.put(new S_Couple(s1,s2), path.getEdgeList());
				this.costs[s1.getId()][s2.getId()] = path.getEdgeList().size()-1;
			}
			
			GraphPath<Node,Link> path_0 = lpaths.getPath(t_0);
			this.to_wan.put(s1, path_0.getEdgeList());
			
			
		}
		
		SingleSourcePaths<Node, Link> paths =alg.getPaths(s_0);
		for(Server s : servers) {
			GraphPath<Node,Link> path = paths.getPath(s);
			this.from_wan.put(s, path.getEdgeList());
		}
		
		
	}

	public HashMap<Server, List<Link>> getTo_wan() {
		return to_wan;
	}

	public HashMap<Server, List<Link>> getFrom_wan() {
		return from_wan;
	}
}
