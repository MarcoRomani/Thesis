package writeFiles;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.TreeSet;

import general.*;

public class CPPtoAMPL {

	

	public void writeCPPdat(DataCenter dc, ArrayList<Customer> cust, ArrayList<Customer> newcust, int seed) {

		Charset utf8 = StandardCharsets.UTF_8;

		ArrayList<String> lines = new ArrayList<String>();
		lines.add("data;");
		String ln = "";
		// write insieme C_bar
		ln = "";
		ln = ln + "set C_bar :=  ";
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {
				ln = ln + vm.getId() + " ";
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme C
		ln = "";
		ln = ln + "set C :=  ";
		int count = 0;
		for (Customer c : newcust) {
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
				count += 1;
			}
		}
		for (Customer c : cust) {
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
				count += 1;
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme R
		ln = "";
		ln = ln + "set R := ";
		for (Customer c : cust) {
			ln = ln + c.getId() + " ";
		}
		for (Customer c : newcust) {
			ln = ln + c.getId() + " ";
		}
		ln = ln + ";";
		lines.add(ln);
		/*
		 * // write insieme R_new ln = ""; for(Customer c: newcust) { ln =
		 * ln+c.getId()+" "; } lines.add(ln);
		 */
		// write insiemi C_bar_r
		ln = "";
		for (Customer c : cust) {
			lines.add("set C_bar_r[" + c.getId() + "] :=  ");
			ln = "";
			for (Container vm : c.getContainers()) {
				ln = ln + vm.getId() + " ";
			}
			ln = ln + ";";
			lines.add(ln);
		}

		// write insiemi C_r
		ln = "";
		for (Customer c : cust) {
			lines.add("set C_r[" + c.getId() + "] :=  ");
			ln = "";
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
			}
			ln = ln + ";";
			lines.add(ln);
		}
		for (Customer c : newcust) {
			lines.add("set C_r[" + c.getId() + "] :=  ");
			ln = "";
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
			}
			ln = ln + ";";
			lines.add(ln);

		}

		// write insieme S
		ln = "";
		ln = ln + "set S :=  ";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ln = ln + s.getId() + " ";
				}
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme S_u
		ln = "";
		ln = ln + "set S_u :=  ";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					if (s.getResidual_cpu() >= 0.4 * s.getCpu()) {
						ln = ln + s.getId() + " ";
					}
				}
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insiemi S_r
		ln = "";
		for (Customer c : cust) {
			ln = "";
			lines.add("set S_r[" + c.getId() + "] := ");
			TreeSet<Server> set = new TreeSet<Server>();
			for (Container vm : c.getContainers()) {
				set.add(dc.getPlacement().get(vm));

			}

			for (Server s : set) {
				ln = ln + s.getId() + " ";
			}
			ln = ln + ";";
			lines.add(ln);

			set.clear();

		}

		// write insieme Rack
		ln = "";
		ln = ln + "set Rack :=  ";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				ln = ln + r.getId() + " ";
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write COST
		ln = "";
		ln = ln + "param COST : ";
		for (int i = 0; i < (Math.pow(dc.getDim(), 3) / 4); i++) {
			ln = ln + i + " ";
		}
		ln = ln + ":=";
		lines.add(ln);
		ln = "";
		for (int i = 0; i < (Math.pow(dc.getDim(), 3) / 4); i++) {
			ln = "";
			ln = ln + i + " ";
			for (int j = 0; j < (Math.pow(dc.getDim(), 3) / 4); j++) {
				ln = ln + dc.getCosts()[i][j] + " ";
			}
			lines.add(ln);
		}
		lines.add(";");

		// write insiemi S_rack
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				lines.add("set S_rack[" + r.getId() + "] :=  ");
				ln = "";
				for (Server s : r.getHosts()) {
					ln = ln + s.getId() + " ";
				}
				ln = ln + ";";
				lines.add(ln);
			}
		}

		// write parametri S

		lines.add("param CPU := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getResidual_cpu();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param MEM := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getResidual_mem();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param DISK := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getResidual_disk();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param BDW_out := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getResidual_bdw_out();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param BDW_in := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getResidual_bdw_in();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		// write parametri C
		ln = "";
		lines.add("param mem := ");
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {

				ln = "";
				ln = ln + vm.getId() + " " + vm.getMem();
				lines.add(ln);
			}
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " " + vm.getMem();
				lines.add(ln);
			}
		}
		for (Customer c : newcust) {
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " " + vm.getMem();
				lines.add(ln);
			}
		}
		lines.add(";");

		ln = "";
		lines.add("param disk := ");
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {

				ln = "";
				ln = ln + vm.getId() + " " + vm.getDisk();
				lines.add(ln);
			}
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " " + vm.getDisk();
				lines.add(ln);
			}
		}
		for (Customer c : newcust) {
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " " + vm.getDisk();
				lines.add(ln);
			}
		}
		lines.add(";");

		ln = "";
		lines.add("param cpu : ");
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ln = ln + s.getId() + " ";
				}
			}
		}
		ln = ln + ":= ";
		lines.add(ln);
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {
				ln = "";
				ln = ln + vm.getId() + " ";
				for (Pod p : dc.getPods()) {
					for (Rack r : p.getRacks()) {
						for (Server s : r.getHosts()) {

							ln = ln + (vm.getCpu() * ((float) 2500 / s.getFrequency())) + " ";

						}
					}
				}
				lines.add(ln);
			}
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " ";
				for (Pod p : dc.getPods()) {
					for (Rack r : p.getRacks()) {
						for (Server s : r.getHosts()) {

							ln = ln + (vm.getCpu() * ((float) 2500 / s.getFrequency())) + " ";

						}
					}
				}
				lines.add(ln);
			}
		}
		for (Customer c : newcust) {
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " ";
				for (Pod p : dc.getPods()) {
					for (Rack r : p.getRacks()) {
						for (Server s : r.getHosts()) {

							ln = ln + (vm.getCpu() * ((float) 2500 / s.getFrequency())) + " ";

						}
					}
				}
				lines.add(ln);
			}
		}
		lines.add(";");

		// write c_0
		lines.add("set c_0 := " + Container.c_0.getId() + ";");

		// write traffici
		ln = "";
		ArrayList<Customer> all_cust = new ArrayList<Customer>();
		all_cust.addAll(cust);
		all_cust.addAll(newcust);
		lines.add("param d := ");

		for (Customer c : all_cust) {
			lines.add("[" + c.getId() + ",*,*] : "); // TODO da rifare
			ln = "";
			ArrayList<Container> all_vm = new ArrayList<Container>();
			all_vm.add(Container.c_0);
			all_vm.addAll(c.getContainers());
			all_vm.addAll(c.getNewContainers());

			for (Container vm : all_vm) {
				ln = ln + vm.getId() + " ";
			}
			ln = ln + ":= ";
			lines.add(ln);
			for (Container vm1 : all_vm) {
				ln = "";
				ln = ln + vm1.getId() + " ";
				for (Container vm2 : all_vm) {

					if (!(c.getTraffic().get(new C_Couple(vm1, vm2)) == null)) {
						ln = ln + c.getTraffic().get(new C_Couple(vm1, vm2)) + " ";
					} else {
						ln = ln + 0 + " ";
					}

				}
				lines.add(ln);
			}

		}
		lines.add(";");

		// write x_bar
		ln = "";
		ln = ln + "param x_bar : ";
		ArrayList<Server> machines = new ArrayList<Server>();
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				machines.addAll(r.getHosts());
			}
		}

		ArrayList<Container> all_cont = new ArrayList<Container>();
		for (Customer c : all_cust) {
			all_cont.addAll(c.getContainers());

		}
		for (Server s : machines) {
			ln = ln + s.getId() + " ";
		}
		ln = ln + " := ";
		lines.add(ln);

		ln = "0 ";
		for (Server s : machines) {
			ln = ln + "0 ";
		}
		lines.add(ln);

		for (Container vm : all_cont) {
			ln = "";
			ln = ln + vm.getId() + " ";
			Server tmp = dc.getPlacement().get(vm);
			for (Server s : machines) {

				if (s.getId() == tmp.getId()) {
					ln = ln + 1 + " ";
				} else {
					ln = ln + 0 + " ";
				}
			}

			lines.add(ln);
		}
		lines.add(";");

		int oldR = 0;
		int newR = 0;
		for (Customer c : Customer.custList) {
			if (c.getContainers().size() == 0) {
				newR += 1;
			} else {
				oldR += 1;
			}
		}

		// actual write

		try {
			Files.write(Paths.get(
					"CPP_seed" + seed + "_pod" + dc.getDim() + "_C" + count + "_newR" + newR + "_oldR" + oldR + ".dat"),
					lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
