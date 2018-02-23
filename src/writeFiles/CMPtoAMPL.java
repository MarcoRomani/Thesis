package writeFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import cmp_heuristics.GRASP_CMP_Scheme;
import cmp_heuristics.Input;
import general.CMPDataCenter;
import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;
import general.Link;
import general.Node;
import general.Pod;
import general.Rack;
import general.S_Couple;
import general.Server;

public class CMPtoAMPL {

	List<String> lines;

	public void writeCMPdat_phase1(CMPDataCenter dc, ArrayList<Customer> cust, int seed) {

	
		lines = new ArrayList<String>();
		lines.add("data;");
		String ln = "";

		// write insieme N
		ln = "";
		ln = ln + "set N :=  ";
		for (Node n : dc.getNetwork().vertexSet()) {
			ln += n.getId() + " ";
		}
		ln += dc.s_0.getId() + " " + dc.t_0.getId();

		ln = ln + ";";
		lines.add(ln);

		// write insieme E
		ln = "";
		ln = ln + "set E :=  ";
		for (Link l : dc.getNetwork().edgeSet()) {
			ln += "(" + l.getMySource().getId() + "," + l.getMyTarget().getId() + ") ";
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme C
		ln = "";
		ln = ln + "set C :=  ";
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {
				ln = ln + vm.getId() + " ";
			}
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
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

		ln = ln + ";";
		lines.add(ln);

		// write insiemi C_r
		ln = "";
		for (Customer c : cust) {
			lines.add("set C_r[" + c.getId() + "] :=  ");
			ln = "";
			for (Container vm : c.getContainers()) {
				ln = ln + vm.getId() + " ";
			}
			for (Container vm : c.getNewContainers()) {
				ln = ln + vm.getId() + " ";
			}
			ln = ln + ";";
			lines.add(ln);
		}

		// write insieme S
		ArrayList<Server> servers = new ArrayList<Server>();
		ln = "";
		ln = ln + "set S :=  ";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ln = ln + s.getId() + " ";
					servers.add(s);
				}
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme PATH
		ln = "";
		ln = ln + "set Path :  ";

		for (Server s : servers) {
			ln = ln + s.getId() + " ";
		}

		ln += ":=";
		lines.add(ln);

		ln = "";
		for (Server s1 : servers) {
			ln = "";
			ln += s1.getId() + " ";
			for (Server s2 : servers) {
				String myln = "{";
				for (Link l : dc.getPaths().get(new S_Couple(s1, s2))) {
					myln += "(" + l.getMySource().getId() + "," + l.getMyTarget().getId() + "),";
				}
				myln = myln.substring(0, ln.length() - 1);
				myln += "} ";
				ln += myln;
			}
			lines.add(ln);
		}
		lines.add(";");

		// INSIEMI path out e in
		ln = "";
		ln += "set Out_Path := ";
		lines.add(ln);
		for (Server s : servers) {
			ln = "";
			ln += s.getId() + " {";
			String myln = "";
			for (Link l : dc.getTo_wan().get(s)) {
				myln += "(" + l.getMySource().getId() + "," + l.getMyTarget().getId() + "),";
			}
			myln = myln.substring(0, ln.length() - 1);
			myln += "} ";
			ln += myln;

			lines.add(ln);
		}
		lines.add(";");

		ln = "";
		ln += "set In_Path := ";
		lines.add(ln);
		for (Server s : servers) {
			ln = "";
			ln += s.getId() + " {";
			String myln = "";
			for (Link l : dc.getFrom_wan().get(s)) {
				myln += "(" + l.getMySource().getId() + "," + l.getMyTarget().getId() + "),";
			}
			myln = myln.substring(0, ln.length() - 1);
			myln += "} ";
			ln += myln;

			lines.add(ln);
		}
		lines.add(";");

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

		// write parametri S

		lines.add("param b_old := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " ";
					ln += (s.isStateON()) ? 1 : 0;
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param CPUtot := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getCpu();
					lines.add(ln);
				}
			}
		}
		lines.add(";");

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

		lines.add("param P := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + (s.getP_max() - s.getP_idle());
					lines.add(ln);
				}
			}
		}
		lines.add(";");

		lines.add("param P_idle := ");
		ln = "";
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					ln = "";
					ln = ln + s.getId() + " " + s.getP_idle();
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

		lines.add(";");

		ln = "";
		lines.add("param Q := ");
		for (Customer c : cust) {
			for (Container vm : c.getContainers()) {

				ln = "";
				ln = ln + vm.getId() + " " + vm.getState();
				lines.add(ln);
			}
			for (Container vm : c.getNewContainers()) {
				ln = "";
				ln = ln + vm.getId() + " " + vm.getState();
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

							ln = ln + CPUcalculator.utilization(vm, s) + " "; // (vm.getCpu() * ((float) 2500 /
																				// s.getFrequency())) + " ";

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

							ln = ln + CPUcalculator.utilization(vm, s) + " ";// (vm.getCpu() * ((float) 2500 /
																				// s.getFrequency())) + " ";

						}
					}
				}
				lines.add(ln);
			}
		}

		lines.add(";");

		// write c_0
		lines.add("set c_0 := " + Container.c_0.getId() + ";");
		// write s_0
		lines.add("set s_0 := " + dc.s_0.getId() + ";");
		// write t_0
		lines.add("set t_0 := " + dc.t_0.getId() + ";");

		// write traffici
		ln = "";
		ArrayList<Customer> all_cust = new ArrayList<Customer>();
		all_cust.addAll(cust);

		lines.add("param d := ");

		for (Customer c : all_cust) {
			lines.add("[" + c.getId() + ",*,*] : ");
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

		// write x_old
		ln = "";
		ln = ln + "param x_old : ";

		ArrayList<Container> all_cont = new ArrayList<Container>();
		for (Customer c : all_cust) {
			all_cont.addAll(c.getContainers());

		}
		for (Server s : servers) {
			ln = ln + s.getId() + " ";
		}
		ln = ln + " := ";
		lines.add(ln);

		ln = "0 ";
		for (Server s : servers) {
			ln = ln + "0 ";
		}
		lines.add(ln);

		for (Container vm : all_cont) {
			ln = "";
			ln = ln + vm.getId() + " ";
			Server tmp = dc.getPlacement().get(vm);
			for (Server s : servers) {

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

		// write parametri LINKS
		lines.add("param K : ");
		ln = "";
		for (Node n : dc.getNetwork().vertexSet()) {
			ln += n.getId() + " ";
		}
		ln += ":=";
		lines.add(ln);
		ln = "";
		for (Node n1 : dc.getNetwork().vertexSet()) {
			ln = "";
			ln += n1.getId() + " ";
			for (Node n2 : dc.getNetwork().vertexSet()) {
				Link l = dc.getNetwork().getEdge(n1, n2);
				ln += (l == null) ? 0 : l.getResCapacity();
			}
			lines.add(ln);
		}
		lines.add(";");

		// write T_1
		lines.add("param T_1 := " + GRASP_CMP_Scheme.MIGR_TIME + ";");

		// write alpha
		lines.add("param alpha := " + GRASP_CMP_Scheme.traff_coeff + ";");

		// write beta
		lines.add("param beta := " + GRASP_CMP_Scheme.migr_coeff + ";");

		// write rho1
		lines.add("param rho1 := " + (1 - Server.almostEmpty_constant) + ";");

		// write rho2
		lines.add("param rho2 := " + (1 - Server.underUtilization_constant) + ";");

		// write rho3
		lines.add("param rho3 := " + (1 - Server.overUtilization_constant) + ";");

	}

	public void writeCMPdat_phase2(CMPDataCenter dc, ArrayList<Customer> cust, int seed, Input input) {

		Charset utf8 = StandardCharsets.UTF_8;

		int count_ob = 0;
		count_ob += input.getSinglesOBL().size();
		for (List<Container> vms : input.getClustersOBL()) {
			count_ob += vms.size();
		}

		int count_f = 0;
		count_f += input.getSinglesOPT().size();
		for (List<Container> vms : input.getClustersOPT()) {
			count_f += vms.size();
		}

		// write insieme C_ob
		String ln = "";
		ln = ln + "set C_ob :=  ";
		for (Container vm : input.getSinglesOBL()) {
			ln = ln + vm.getId() + " ";
		}
		for (List<Container> ls : input.getClustersOBL()) {
			for (Container vm : ls) {
				ln = ln + vm.getId() + " ";
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// write insieme C_f
		ln = "";
		ln = ln + "set C_f :=  ";
		for (Container vm : input.getSinglesOPT()) {
			ln = ln + vm.getId() + " ";
		}
		for (List<Container> ls : input.getClustersOPT()) {
			for (Container vm : ls) {
				ln = ln + vm.getId() + " ";
			}
		}
		ln = ln + ";";
		lines.add(ln);

		// actual write

		try {
			Files.write(
					Paths.get("istanzeCMP" + File.separator + "CMP_seed" + seed + "_pod" + dc.getDim() + "_Cob"
							+ count_ob + "_Cf" + count_f + "_R" + cust.size() + ".dat"),
					lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
