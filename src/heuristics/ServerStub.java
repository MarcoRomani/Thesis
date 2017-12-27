package heuristics;

import java.util.ArrayList;
import java.util.TreeSet;

import general.*;

public class ServerStub {

	private int id;
	private final float cpu;
	private float res_cpu;
	private float res_mem;
	private float res_disk;
	private float res_out;
	private float res_in;
	private final float freq;
	private Server serv;
	
	private ArrayList<Container> containers = new ArrayList<Container>();
	
	public ServerStub(Server s) {
		
		serv = s;
		id = s.getId();
		cpu = s.getCpu();
		res_cpu = s.getResidual_cpu();
		res_mem = s.getResidual_mem();
		res_disk = s.getResidual_disk();
		res_out = s.getResidual_bdw_out();
		res_in = s.getBdw_in();
		freq = s.getFrequency();
	}
	
	/**
	 * check if the container can be added to the stub:
	 * feasible -> return true and update all resources (bandwidth of other stubs too)
	 * infeasible -> return false without changing anything
	*  requires that stubs contains stubs of all servers with id matching the position in the list
	*/
	public boolean allocate(Container vm, ArrayList<ServerStub> stubs, CPPSolution sol, DataCenter dc, boolean b) {
		
		if(res_cpu - vm.getCpu()*((float)(2500/this.freq))<0 || res_mem - vm.getMem()<0 || res_disk - vm.getDisk() < 0) {   
		//	System.out.println("\n 1- cant put vm "+vm.getId()+vm.getType()+" into server "+this.getId());
			return false;
	     }
		boolean flag = true;
		Customer r= Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();
		ArrayList<Container> n_conts = r.getNewContainers();
		
		  TreeSet<Integer> set = new TreeSet<Integer>();
		  for(Container ct: conts) {
			  set.add( new Integer(dc.getPlacement().get(ct).getId()));
		  }
		  for(Container ct: n_conts) {
			  if(sol.getTable().get(ct) != null) {
			     set.add(sol.getTable().get(ct));
			  }
		  }
		  ArrayList<ServerStub> local_stubs = new ArrayList<ServerStub>();
		  for(Integer i: set) {
			  local_stubs.add(stubs.get(i.intValue()));
		  }
		  
		float[] out = new float[local_stubs.size()];
		float[] in = new float[local_stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
				  for(int i=0; i < local_stubs.size(); i++) { 
						if(local_stubs.get(i).getId() == s) {
							Float tmp = r.getTraffic().get(new C_Couple(c,vm));
							out[i] += (tmp == null) ?  0 :  tmp.floatValue();
							tmp = r.getTraffic().get(new C_Couple(vm,c));
							in[i] += (tmp == null) ? 0 : tmp.floatValue();
						}
				  }
			  }
			}
		}
		for(Container c: conts) {
			int s= dc.getPlacement().get(c).getId();
			if(s != this.getRealServ().getId()) {
				for(int i =0; i < local_stubs.size(); i++) {
					if(local_stubs.get(i).getId() == s) {
						Float tmp = r.getTraffic().get(new C_Couple(c,vm));
						out[i] += (tmp == null) ?  0 :  tmp.floatValue();
				    	tmp = r.getTraffic().get(new C_Couple(vm,c));
				    	in[i] += (tmp == null) ? 0 : tmp.floatValue();
					}
				}
			}
		}
	
		Float toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0));
		Float fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm));
		float this_out =0;
		float this_in = 0;

		for(int i=0; i<local_stubs.size(); i++) {
			if (local_stubs.get(i).getRes_out() < out[i]) { flag = false; }
			if ( local_stubs.get(i).getRes_in() < in[i]) { flag = false; }
			this_out += in[i];
			this_in += out[i];
		}
		if(!flag) { 
			System.out.println("\n 2- cant put vm "+vm.getId()+" into server "+this.getId());
			return false; }
		
		
		 this_out += (toWan == null) ? 0 : toWan.floatValue();
		 this_in += (fromWan == null)? 0 : fromWan.floatValue();
		
		if(this.res_out < this_out || this.res_in < this_in) {
			
		System.out.println("\n 3- cant put vm "+vm.getId()+" into server "+this.getId());
		return false; 
		}
		if(!b) return true;
		// ALL IS GOOD AND WE CAN PROCEED with the allocation
		
		for(int i=0; i<local_stubs.size(); i++) {
			local_stubs.get(i).setRes_out(local_stubs.get(i).getRes_out() - out[i]);
			local_stubs.get(i).setRes_in(local_stubs.get(i).getRes_in() - in[i]);
		}
		
		res_out -= this_out;
		res_in -= this_in;
	    res_cpu -= vm.getCpu()*((float)(2500/this.freq));
	    res_mem -= vm.getMem();
	    res_disk -= vm.getDisk();
	    containers.add(vm);
	    return true;
	     
	}
	
	/**
	 * removes a container from the stub and updates all the resources (bandwidth of other stubs too)
	*  requires that stubs contains stubs of all servers with id matching the position in the list
	*/
	
	public void remove(Container vm, ArrayList<ServerStub> stubs, CPPSolution sol, DataCenter dc) {
		
		Customer r= Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();
		ArrayList<Container> n_conts = r.getNewContainers();
		
		  TreeSet<Integer> set = new TreeSet<Integer>();
		  for(Container ct: conts) {
			  set.add( new Integer(dc.getPlacement().get(ct).getId()));
		  }
		  for(Container ct: n_conts) {
			  if(sol.getTable().get(ct) != null) {
			     set.add(sol.getTable().get(ct));
			  }
		  }
		  
		  
		  ArrayList<ServerStub> local_stubs = new ArrayList<ServerStub>();
		  for(Integer i: set) {
			  local_stubs.add(stubs.get(i.intValue()));
		  }
		  
		float[] out = new float[local_stubs.size()];
		float[] in = new float[local_stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
				  for(int i=0; i < local_stubs.size(); i++) { 
					if(local_stubs.get(i).getId() == s) {
					 Float tmp = r.getTraffic().get(new C_Couple(c,vm));
					 out[i] += (tmp == null) ?  0 :  tmp.floatValue();
					 tmp = r.getTraffic().get(new C_Couple(vm,c));
					 in[i] += (tmp == null) ? 0 : tmp.floatValue();
					 break;
				   }
				  }
			  }
			}
		}
		for(Container c: conts) {
			int s= dc.getPlacement().get(c).getId();
			if(s != this.getRealServ().getId()) {
				for(int i =0; i < local_stubs.size(); i++) {
					if(local_stubs.get(i).getId() == s) {
						Float tmp = r.getTraffic().get(new C_Couple(c,vm));
						out[i] += (tmp == null) ?  0 :  tmp.floatValue();
						tmp = r.getTraffic().get(new C_Couple(vm,c));
						in[i] += (tmp == null) ? 0 : tmp.floatValue();
					}
				}
			}
		}
	
		Float toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0));
		Float fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm));
		float this_out =0;
		float this_in = 0;

		for(int i=0; i<local_stubs.size(); i++) {	
			local_stubs.get(i).setRes_out(local_stubs.get(i).getRes_out() + out[i]);
			local_stubs.get(i).setRes_in(local_stubs.get(i).getRes_in() + in[i]);
			this_out += in[i];
			this_in += out[i];
		}
		this_out += (toWan == null)? 0 : toWan.floatValue();
		this_in += (fromWan == null)? 0 : fromWan.floatValue();
		
		res_out += this_out;
		res_in += this_in;
		res_cpu += vm.getCpu()*((float)(2500/this.freq));
		res_mem += vm.getMem();
		res_disk += vm.getDisk();
		containers.remove(vm);
	}

	public int getId() {
		return id;
	}

	public float getCpu() {
		return cpu;
	}

	public float getRes_cpu() {
		return res_cpu;
	}

	public float getRes_mem() {
		return res_mem;
	}

	public float getRes_disk() {
		return res_disk;
	}

	public float getRes_out() {
		return res_out;
	}

	public float getRes_in() {
		return res_in;
	}
	
	protected void setRes_out(float f) {
		res_out = f;
	}
	
	protected void setRes_in(float f) {
		res_in = f;
	}

	public float getFreq() {
		return freq;
	}

	

	public Server getRealServ() {
		return serv;
	}

	public ArrayList<Container> getContainers() {
		return containers;
	}
	
	public void reset() {
		res_cpu = serv.getResidual_cpu();
		res_mem = serv.getResidual_mem();
		res_disk = serv.getResidual_disk();
		res_out = serv.getResidual_bdw_out();
		res_in = serv.getBdw_in();
		containers.clear();
	}

}
