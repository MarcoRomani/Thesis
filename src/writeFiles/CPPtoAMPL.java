package writeFiles;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import general.*;

public class CPPtoAMPL {

	public void writeDAT(DataCenter dc, ArrayList<Customer> cust, ArrayList<Customer> newcust, int seed) {
		
		Charset utf8 = StandardCharsets.UTF_8;
		
		
		ArrayList<String> lines = new ArrayList<String>();
		String ln = "";
		// write insieme C_bar
		ln = "";
		for(Customer c: cust) {
			for(Container vm: c.getContainers()) {
				ln = ln+vm.getId()+" ";
			}
		}
		lines.add(ln);
		
		// write insieme C
		ln = "";
		int count = 0;
		for(Customer c: newcust) {
			for(Container vm: c.getNewContainers()) {
				ln = ln+vm.getId()+" ";
				count+=1;
			}
		}
		for(Customer c: cust) {
			for(Container vm: c.getNewContainers()) {
				ln = ln+vm.getId()+" ";
				count+=1;
			}
		}
		lines.add(ln);
		
		// write insieme R
		ln = "";
		for(Customer c: cust) {
			ln = ln+c.getId()+" ";
		}
		for(Customer c: newcust) {
			ln = ln+c.getId()+" ";
		}
		lines.add(ln);
		
		// write insieme R_new
		ln = "";
		for(Customer c: newcust) {
			ln = ln+c.getId()+" ";
		}
		lines.add(ln);
		
		// write insiemi C_r_bar
		ln = "";
		for(Customer c: cust) {
			lines.add(c.getId()+"");
			ln = "";
			for(Container vm: c.getContainers()) {
				ln= ln+vm.getId()+" ";
			}
			lines.add(ln);
		}
		
		// write insiemi C_r
		ln = "";
		for(Customer c: cust) {
			lines.add(c.getId()+"");
			ln = "";
			for(Container vm : c.getNewContainers()) {
				ln = ln+vm.getId()+" ";
			}
			lines.add(ln);
		}
		for(Customer c: newcust) {
			lines.add(c.getId()+"");
			ln = "";
			for(Container vm : c.getNewContainers()) {
				ln = ln+vm.getId()+" ";
			}
			lines.add(ln);
			
		}
		
		// write insieme S
		ln = "";
		for(Pod p : dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					ln = ln+s.getId()+" ";
				}
			}
		}
		lines.add(ln);
		
		// write insieme S_u
		ln = "";
		for(Pod p : dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					if(s.getResidual_cpu() >= 0.4*s.getCpu()) {
					   ln = ln+s.getId()+" ";
					}
				}
			}
		}
		lines.add(ln);
		
		// write insieme Rack
		ln = "";
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				ln = ln+r.getId()+" ";
			}
		}
		lines.add(ln);
		
		// write COST
		ln = "";
		for(int i=0; i< (Math.pow(dc.getDim(),3)/4); i++) {
			ln = "";
			for(int j=0; j< (Math.pow(dc.getDim(),3)/4); j++) {			
				ln = ln+dc.getCosts()[i][j]+" ";				
			}
			lines.add(ln);
		}
		
		// write insiemi S_rack
		ln = "";
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				lines.add(r.getId()+"");
				ln = "";
				for(Server s : r.getHosts()) {
					ln = ln+s.getId()+" ";
				}
				lines.add(ln);
			}
		}
		
		// write parametri S
		ln = "";
		for(Pod p: dc.getPods()) {
			for(Rack r : p.getRacks()) {
				for(Server s: r.getHosts()) {
					lines.add(s.getId()+"");
					ln = "";
					ln = ln+ s.getCpu()+/*" "+s.getMem()+" "+s.getDisk()+" "+s.getBdw_out()+" "+s.getBdw_in()+*/" "+s.getResidual_cpu()+" "+s.getResidual_mem()
					+" "+s.getResidual_disk()+" "+s.getResidual_bdw_out()+" "+s.getResidual_bdw_in()+" "+s.getFrequency();
					lines.add(ln);
				}
			}
		}
		
		// write parametri C
		ln = "";
		for(Customer c: cust) {
			for(Container vm: c.getContainers() ) {
				lines.add(vm.getId()+"");
				ln = "";
				ln = ln+vm.getCpu()+" "+vm.getMem()+" "+vm.getDisk();
				lines.add(ln);
			}
			for(Container vm: c.getNewContainers() ) {
				lines.add(vm.getId()+"");
				ln = "";
				ln = ln+vm.getCpu()+" "+vm.getMem()+" "+vm.getDisk();
				lines.add(ln);
			}
		}
		for(Customer c: newcust) {
			for(Container vm: c.getNewContainers() ) {
				lines.add(vm.getId()+"");
				ln = "";
				ln = ln+vm.getCpu()+" "+vm.getMem()+" "+vm.getDisk();
				lines.add(ln);
			}
		}
		
		// write c_0
		
		// write traffici
		ln = "";
		ArrayList<Customer> all_cust = new ArrayList<Customer>();
		all_cust.addAll(cust);
		all_cust.addAll(newcust);
		
		for(Customer c: all_cust) {
			lines.add(c.getId()+"");
			ln = "";
			ArrayList<Container> all_vm = new ArrayList<Container>();
			all_vm.add(Container.c_0);
			all_vm.addAll(c.getContainers());
			all_vm.addAll(c.getNewContainers());
			for(Container vm1: all_vm) {
				for(Container vm2: all_vm ) {
					
					if(!(c.getTraffic().get(new C_Couple(vm1,vm2))== null)) {
						ln = ln+c.getTraffic().get(new C_Couple(vm1,vm2))+" ";
					} else { ln = ln+0+" "; }
					
				}
			}
			lines.add(ln);
		}
		
		// write x_bar
		ln = "";
		ArrayList<Server> machines = new ArrayList<Server>();
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				machines.addAll(r.getHosts());
			}
		}
		
		ArrayList<Container> all_cont = new ArrayList<Container>();
		for(Customer c: all_cust) {
			all_cont.addAll(c.getContainers());
			all_cont.addAll(c.getNewContainers());
		}
		
		for(Container vm: all_cont) {
		      for(Server s: machines) {
			
				if(s.isIn(vm)) {
					ln = ln+1+" ";
				}else {
					ln = ln+0+" ";
				}
			}
		}
		lines.add(ln);
		
		
		// actual write
		
		
		try {
			Files.write(Paths.get("CPP_seed"+seed+"pod"+dc.getDim()+"_newC"+count+".txt"), lines, utf8,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
