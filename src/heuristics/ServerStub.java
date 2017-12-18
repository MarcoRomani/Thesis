package heuristics;

import java.util.ArrayList;

import general.Container;
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
		// TODO Auto-generated constructor stub
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
	
	
	public boolean allocate(Container vm) {
		
		if(res_cpu - vm.getCpu()<0 || res_mem - vm.getMem()<0 || res_disk - vm.getDisk() < 0) {
			return false;
	     }
	     res_cpu -= vm.getCpu();
	     res_mem -= vm.getMem();
	     res_disk -= vm.getDisk();
	     containers.add(vm);
	     return true;
	     
	}
	
	public void remove(Container vm) {
	//	if(containers.contains(vm)) {
		res_cpu += vm.getCpu();
		res_mem += vm.getMem();
		res_disk += vm.getDisk();
		containers.remove(vm);
	//	}
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
