package general;

import java.util.ArrayList;

public class DataCenter {

	// TODO links switches, other topologies
	private int dim;
	private ArrayList<Pod> pods;
	private ArrayList<Switch> core;
	private int[][] costs;
	private static DataCenter myself = null;
	
	
	private DataCenter(String topology, int size) {
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

			case "VL2":
		    case "Bcube":
		}
		
		
	}
	
	
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
	
	public static DataCenter buyFatTreeDC(int size) {
		if(myself == null) {
			myself = new DataCenter("FatTree", size);
		}
		return myself;
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


	public int[][] getCosts() {
		return costs;
	}
}
	

