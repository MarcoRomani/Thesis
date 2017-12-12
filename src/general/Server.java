package general;

import java.util.*;

public class Server extends Node {
	private static int server_id = 0;
	private int id;
	private Server_model type;
    private float cpu;
    private float residual_cpu;
    private float mem;
    private float residual_mem;
    private float disk;
    private float residual_disk;
    private float bdw_out;
    private float residual_bdw_out;
    private float bdw_in;
    private float residual_bdw_in;
    private float frequency;
    
    private Set<Container> containers = new TreeSet<Container>();
    
  
    
    public Server(Server_model sm) {
    	this.type = sm;
    	float [] specs = Catalog.getServSpecs(sm);
    	cpu = specs[2];
    	residual_cpu = cpu;
    	mem = specs[4];
    	residual_mem = mem;
    	disk = specs[5];
    	residual_disk = disk;
    	bdw_out = specs[6];
    	bdw_in = specs[6];
    	residual_bdw_out = bdw_out;
    	residual_bdw_in = bdw_in;
    	frequency = specs[3];
    	
    	this.id = server_id;
    	server_id += 1; 
    }
    
    public boolean canBePlaced(Container c, float tol) {
 
    	if(tol < 0 || tol > 1) return false;
    	float t = 1 - tol;
    		
        return(this.residual_cpu - c.getCpu() >= t*this.cpu && this.residual_mem - c.getMem() >= t*this.mem && this.residual_disk -
    				c.getDisk() >= t*this.disk && this.residual_bdw_out - c.getBdw_out() >= t*this.bdw_out &&
    				this.residual_bdw_in - c.getBdw_in() >= t*this.bdw_in);
    	
    }
    
    public void allocateContainer(Container c) {
    	this.containers.add(c);
    	this.residual_cpu -= c.getCpu()*(float)(2500/frequency);
    	this.residual_mem -= c.getMem();
    	this.residual_disk -= c.getDisk();
    	this.residual_bdw_out -= c.getBdw_out();
    	this.residual_bdw_in -= c.getBdw_in();
    }
    
    public void deallocateContainer(Container c) {
    	this.containers.remove(c);
      	this.residual_cpu += c.getCpu()*(float)(2500/frequency);
    	this.residual_mem += c.getMem();
    	this.residual_disk += c.getDisk();
    	this.residual_bdw_out += c.getBdw_out();
    	this.residual_bdw_in += c.getBdw_in();
    }

    public void updateBandwidth() {
    	
    	float usedBDWin = 0;
    	float usedBDWout = 0;
    	for(Container c1 : containers) {
    		HashMap<C_Couple,Float> tr = Customer.custList.get(c1.getCust()).getTraffic();
    		ArrayList<Container> list = Customer.custList.get(c1.getCust()).getContainers();
    		
    		for(Container c2 : list) {
    			if(!(this.isIn(c2)) && !(tr.get(new C_Couple(c1,c2)) == null) ) {
     				usedBDWout += tr.get(new C_Couple(c1,c2)).floatValue();
    				if(!(tr.get(new C_Couple(c2,c1)) == null)) {
    				    usedBDWin += tr.get(new C_Couple(c2,c1)).floatValue();
    				}
    			}
    		}
    	}
    	
    	this.residual_bdw_in = this.bdw_in - usedBDWin;
    	this.residual_bdw_out = this.bdw_out - usedBDWout;
    }
    
    // GETTERS
    
	public Server_model getType() {
		return type;
	}

	public float getCpu() {
		return cpu;
	}

	public float getResidual_cpu() {
		return Math.max(0, residual_cpu);
	}

	public float getMem() {
		return mem;
	}
	
	public float getFrequency() {
		return frequency;
	}


	public float getResidual_mem() {
		return Math.max(0, residual_mem);
	}

	public float getDisk() {
		return disk;
	}

	public float getResidual_disk() {
		return Math.max(0, residual_disk);
	}

	public float getBdw_out() {
		return bdw_out;
	}

	public float getResidual_bdw_out() {
		return Math.max(0, residual_bdw_out);
	}

	public float getBdw_in() {
		return bdw_in;
	}

	public float getResidual_bdw_in() {
		return Math.max(0, residual_bdw_in);
	}

	public Set<Container> getContainers() {
		return containers;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isIn(Container c) {
		return containers.contains(c);
	}
	@Override
	public String toString() {
		return Integer.toString(this.id)+": [modello ="+type.toString()+"res_cpu ="+residual_cpu+", res_mem ="+residual_mem+", res_disk ="+residual_disk+",res_out ="+residual_bdw_out+", res_in ="+residual_bdw_in+"]";
	}
    
}
