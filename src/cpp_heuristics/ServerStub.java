package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import general.*;
/**
 * 
 * @author Marco
 * Copy of a server, used in the heuristics in order to not modify the starting instance
 */
public class ServerStub {

	private int id;
	private final double cpu;
	private double res_cpu;
	private double res_mem;
	private double res_disk;
	private double res_out;
	private double res_in;
//	private final double freq;
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
	//	freq = s.getFrequency();
	}
	
	/**
	 * check if the container can be added to the stub:
	 * feasible -> return true and update all resources (bandwidth of other stubs too)
	 * infeasible -> return false without changing anything
	*  requires that stubs contains stubs of all servers with id matching the position in the list
	*/
	public boolean allocate(Container vm, List<ServerStub> stubs, CPPSolution sol, DataCenter dc, boolean b) {
		
		if(res_cpu - (CPUcalculator.utilization(vm, serv))<0 || res_mem - vm.getMem()<0 || res_disk - vm.getDisk() < 0) {   
			//System.out.println("\n 1- cant put vm "+vm.getId()+vm.getType()+" into server "+this.getId());
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
		  
		double[] out = new double[local_stubs.size()];
		double[] in = new double[local_stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
				  for(int i=0; i < local_stubs.size(); i++) { 
						if(local_stubs.get(i).getId() == s) {
							Double tmp = r.getTraffic().get(new C_Couple(c,vm));
							out[i] += (tmp == null) ?  0 :  tmp.doubleValue();
							tmp = r.getTraffic().get(new C_Couple(vm,c));
							in[i] += (tmp == null) ? 0 : tmp.doubleValue();
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
						Double tmp = r.getTraffic().get(new C_Couple(c,vm));
						out[i] += (tmp == null) ?  0 :  tmp.doubleValue();
				    	tmp = r.getTraffic().get(new C_Couple(vm,c));
				    	in[i] += (tmp == null) ? 0 : tmp.doubleValue();
				    	break;
					}
				}
			}
		}
	
		Double toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0));
		Double fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm));
		double this_out =0;
		double this_in = 0;

		for(int i=0; i<local_stubs.size(); i++) {
			if (local_stubs.get(i).getRes_out() < out[i]) { flag = false; }
			if ( local_stubs.get(i).getRes_in() < in[i]) { flag = false; }
			this_out += in[i];
			this_in += out[i];
		}
		if(!flag) { 
			//System.out.println("\n 2- cant put vm "+vm.getId()+" into server "+this.getId());
			return false; }
		
		
		 this_out += (toWan == null) ? 0 : toWan.doubleValue();
		 this_in += (fromWan == null)? 0 : fromWan.doubleValue();
		
		if(this.res_out < this_out || this.res_in < this_in) {
			
	//	System.out.println("\n 3- cant put vm "+vm.getId()+" into server "+this.getId());
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
	    res_cpu -= CPUcalculator.utilization(vm, serv); //vm.getCpu()*((double)(2500/this.freq));
	    res_mem -= vm.getMem();
	    res_disk -= vm.getDisk();
	//	System.out.println("Put vm "+vm.getId()+" into stub "+this.getId());

	    containers.add(vm);
	    return true;
	     
	}
	
	/**
	 * removes a container from the stub and updates all the resources (bandwidth of other stubs too)
	*  requires that stubs contains stubs of all servers with id matching the position in the list
	*/
	
	public void remove(Container vm, List<ServerStub> stubs, CPPSolution sol, DataCenter dc) {
		
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
		  
		double[] out = new double[local_stubs.size()];
		double[] in = new double[local_stubs.size()];
		
		for(Container c: n_conts) {
			if (vm != c && sol.getTable().get(c) != null) {
			  int s= sol.getTable().get(c).intValue();
			  if(s != this.getRealServ().getId()) {
				  for(int i=0; i < local_stubs.size(); i++) { 
					if(local_stubs.get(i).getId() == s) {
					 Double tmp = r.getTraffic().get(new C_Couple(c,vm));
					 out[i] += (tmp == null) ?  0 :  tmp.doubleValue();
					 tmp = r.getTraffic().get(new C_Couple(vm,c));
					 in[i] += (tmp == null) ? 0 : tmp.doubleValue();
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
						Double tmp = r.getTraffic().get(new C_Couple(c,vm));
						out[i] += (tmp == null) ?  0 :  tmp.doubleValue();
						tmp = r.getTraffic().get(new C_Couple(vm,c));
						in[i] += (tmp == null) ? 0 : tmp.doubleValue();
						break;
					}
				}
			}
		}
	
		Double toWan = r.getTraffic().get(new C_Couple(vm,Container.c_0));
		Double fromWan = r.getTraffic().get(new C_Couple(Container.c_0,vm));
		double this_out =0;
		double this_in = 0;

		for(int i=0; i<local_stubs.size(); i++) {	
			local_stubs.get(i).setRes_out(local_stubs.get(i).getRes_out() + out[i]);
			local_stubs.get(i).setRes_in(local_stubs.get(i).getRes_in() + in[i]);
			this_out += in[i];
			this_in += out[i];
		}
		this_out += (toWan == null)? 0 : toWan.doubleValue();
		this_in += (fromWan == null)? 0 : fromWan.doubleValue();
		
		res_out += this_out;
		res_in += this_in;
		res_cpu += CPUcalculator.utilization(vm, serv); //vm.getCpu()*((double)(2500/this.freq));
		res_mem += vm.getMem();
		res_disk += vm.getDisk();
		boolean bool = containers.remove(vm);
	//	System.out.println("Try to remove vm "+vm.getId()+" from stub "+this.getId());
		if(!bool) throw new NoSuchElementException();
	}

	public int getId() {
		return id;
	}

	public double getCpu() {
		return cpu;
	}

	public double getRes_cpu() {
		return res_cpu;
	}

	public double getRes_mem() {
		return res_mem;
	}

	public double getRes_disk() {
		return res_disk;
	}

	public double getRes_out() {
		return res_out;
	}

	public double getRes_in() {
		return res_in;
	}
	
	protected void setRes_out(double f) {
		res_out = f;
	}
	
	protected void setRes_in(double f) {
		res_in = f;
	}

	//public double getFreq() {
	//	return freq;
	//}

	

	public Server getRealServ() {
		return serv;
	}

	public ArrayList<Container> getContainers() {
		return containers;
	}
	
	/*
	public void reset() {
		res_cpu = serv.getResidual_cpu();
		res_mem = serv.getResidual_mem();
		res_disk = serv.getResidual_disk();
		res_out = serv.getResidual_bdw_out();
		res_in = serv.getBdw_in();
		containers.clear();
	}*/

}
