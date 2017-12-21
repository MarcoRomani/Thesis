package heuristics;

import java.util.ArrayList;

import general.*;
import general.Customer;
import general.Server;

public class ServerStub {

	private int id;
	private float cpu;
	private float res_cpu;
	private float res_mem;
	private float res_disk;
	private float res_out;
	private float res_in;
	private float freq;
	private boolean flag = true;
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
		
		if(res_cpu - vm.getCpu()<0 || res_mem - vm.getMem()<0 || res_disk - vm.getDisk() < 0) {   // TODO adjust cpu
		//	System.out.println("\n 1- cant put vm "+vm.getId()+vm.getType()+" into server "+this.getId());
			return false;
	     }
		boolean flag = true;
		Customer r= Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();
		ArrayList<Container> n_conts = r.getNewContainers();
		float[] out = new float[stubs.size()];
		float[] in = new float[stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
			    Float tmp = r.getTraffic().get(new C_Couple(c,vm));
			    out[s] += (tmp == null) ?  0 :  tmp.floatValue();
			    tmp = r.getTraffic().get(new C_Couple(vm,c));
			    in[s] += (tmp == null) ? 0 : tmp.floatValue();
			  }
			}
		}
		for(Container c: conts) {
			int s= dc.getPlacement().get(c).getId();
			if(s != this.getRealServ().getId()) {
				 Float tmp = r.getTraffic().get(new C_Couple(c,vm));
				    out[s] += (tmp == null) ?  0 :  tmp.floatValue();
				    tmp = r.getTraffic().get(new C_Couple(vm,c));
				    in[s] += (tmp == null) ? 0 : tmp.floatValue();
			}
		}
	
		Float toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0));
		Float fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm));
		float this_out =0;
		float this_in = 0;

		for(int i=0; i<stubs.size(); i++) {
			if (stubs.get(i).getRes_out() < out[i]) { flag = false; }
			if ( stubs.get(i).getRes_in() < in[i]) { flag = false; }
			this_out += in[i];
			this_in += out[i];
		}
		if(!flag) { 
			System.out.println("\n 2- cant put vm "+vm.getId()+" into server "+this.getId());
			return false; }
		
		
		if(toWan != null) this_out += toWan.floatValue();
		if(fromWan != null) this_in += fromWan.floatValue();
		
		if(this.res_out < this_out || this.res_in < this_in) {
			
		System.out.println("\n 3- cant put vm "+vm.getId()+" into server "+this.getId());
		return false; 
		}
		if(!b) return true;
		// ALL IS GOOD AND WE CAN PROCEED
		
		for(int i=0; i<stubs.size(); i++) {
			stubs.get(i).setRes_out(stubs.get(i).getRes_out() - out[i]);
			stubs.get(i).setRes_in(stubs.get(i).getRes_in() - in[i]);
		}
		
		res_out -= this_out;
		res_in -= this_in;
	    res_cpu -= vm.getCpu();
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
		float[] out = new float[stubs.size()];
		float[] in = new float[stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
			    Float tmp = r.getTraffic().get(new C_Couple(c,vm));
			    out[s] += (tmp == null) ?  0 :  tmp.floatValue();
			    tmp = r.getTraffic().get(new C_Couple(vm,c));
			    in[s] += (tmp == null) ? 0 : tmp.floatValue();
			  }
			}
		}
		for(Container c: conts) {
			int s= dc.getPlacement().get(c).getId();
			if(s != this.getRealServ().getId()) {
				 Float tmp = r.getTraffic().get(new C_Couple(c,vm));
				    out[s] += (tmp == null) ?  0 :  tmp.floatValue();
				    tmp = r.getTraffic().get(new C_Couple(vm,c));
				    in[s] += (tmp == null) ? 0 : tmp.floatValue();
			}
		}
	
		float toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0)).floatValue();
		float fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm)).floatValue();
		float this_out =0;
		float this_in = 0;

		for(int i=0; i<stubs.size(); i++) {	
			stubs.get(i).setRes_out(stubs.get(i).getRes_out() + out[i]);
			stubs.get(i).setRes_in(stubs.get(i).getRes_in() + in[i]);
			this_out += in[i];
			this_in += out[i];
		}
		this_out += toWan;
		this_in += fromWan;
		
		res_out += this_out;
		res_in += this_in;
		res_cpu += vm.getCpu();
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

	public boolean isFlag() {
		return flag;
	}

	public Server getRealServ() {
		return serv;
	}

	public ArrayList<Container> getContainers() {
		return containers;
	}

}
