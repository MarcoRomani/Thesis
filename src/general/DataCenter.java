package general;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Representation of a data center with a hierarchy of servers and the distance measures between such servers
 * @author Marco
 *
 */
public class DataCenter {

	protected int dim;
	protected ArrayList<Pod> pods;
	protected ArrayList<Switch> core;
	protected int[][] costs;
	protected HashMap<Container,Server> placement;
	
	/**
	 * Generates a data center starting from a specified network topology and a size parameter (number of pods)
	 * @param topology
	 * @param size
	 */
	public DataCenter(String topology, int size) {
		this.dim = size;
		switch(topology) {
			case "FatTree":
				core = new ArrayList<Switch>();
				pods = new ArrayList<Pod>();
				for(int i=0; i< (int)(size*size)/4; i++) {
					core.add(new Switch());
				}
				for(int i=0; i<size; i++) {
					pods.add(new FatTreePod(core, size/2));
				}
				
				this.costs = new int[(int)(Math.pow(size, 3))/4][(int)(Math.pow(size, 3))/4];
				computeFatTreeCosts();
				placement = new HashMap<Container,Server>(10*(int)(Math.pow(size, 3))/4, 1);
				Pod.pod_id =0;
				Rack.rack_id = 0;
				
				Server.server_id = 0;

			case "VL2":
		    case "Bcube":
		}
		
		
	}
	
	/*
	 * default distances/costs in a fat tree structure
	 */
	private void computeFatTreeCosts() {
		for(int i=0; i<(int)(Math.pow(dim,3))/4; i++) {
			for(int j=0; j<(int)(Math.pow(dim,3))/4; j++) {
				costs[i][j] = 5;
			}
		}
		
		ArrayList<Server> servers = new ArrayList<Server>();
		for (Pod p : pods) {
		    for(Rack r: p.getRacks()) {
		    	servers.addAll(r.getHosts());
		    }
		    for(Server s: servers) {
		    	for(Server s2: servers) {
		    		costs[s.getId()][s2.getId()] = 3;
		    	}
		    }
		    
		    for(Rack r: p.getRacks()) {
		    	for(Server s: r.getHosts()) {
		    		for(Server s2: r.getHosts()) {
		    			costs[s.getId()][s2.getId()] = 1;
		    		}
		    	}
		    }
		  servers.clear();  
		}
		
		for(int i=0; i<(int)(Math.pow(dim,3))/4; i++) {
			costs[i][i] = 0;
		}
		
	}
	

	public int getDim() {
		return dim;
	}


	public ArrayList<Pod> getPods() {
		return pods;
	}


	public ArrayList<Switch> getCore() {
		return core;
	}


	public HashMap<Container, Server> getPlacement(){
		return placement;
	}
	
	public int[][] getCosts() {
		return costs;
	}
	
	/**
	 * Given a server identifier, finds the related server. Exploits the known ordering of the servers.
	 * @param id ID
	 * @return server
	 */
	public Server findServer(int id) {
		for(Pod p:pods) {
			if(p.containsServer(id)) {
				for(Rack r: p.getRacks()) {
					if(r.containsServer(id)) {
						for(Server s: r.getHosts()) {
							if(s.getId() == id) return s;
						}
					}
				}
			}
		}
		return null;
	}
}
	

