package ltCMP;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import cmp_heuristics.*;
import cmp_heuristics.GRASP_CMP_Type1;
import cmp_heuristics.Input;

import cpp_heuristics.CPPSolution;

import cpp_heuristics.ContainerBDWComparator;
import cpp_heuristics.ContainerCPUComparator;
import cpp_heuristics.ContainerDISKComparator;
import cpp_heuristics.ContainerRAMComparator;
import cpp_heuristics.PathRel_manager;
import cpp_heuristics.SolutionWrapper;
import general.*;
import stCPP.PopulateException;
import writeFiles.CMPtoAMPL;

public class CMPMain {
	 public static boolean writedat = false;
	public static boolean display = false;
	public static String option = "time";
	public static int iter_param = 10;
	public static double time_minutes = 60;
	public static double alfa_grasp = 0.15;
	public static double filler_thresh = 0.85;
	public static int max_requests = 3600;
	public static int min_requests = 400;
	
	public static boolean opt_empty = true;
	public static boolean opt = true;
	public static int opt_probability =2 ;

	public static void main(String[] args) {

		int iter = 1;
		int my_seed = 9760;
		int n_cust = 5000;
		int n_pods = 30;

		if (args.length >= 1)
			my_seed = Integer.parseInt(args[0]);

		if (args.length >= 2)
			n_pods = Integer.parseInt(args[1]);

		if (args.length >= 3)
			n_cust = Integer.parseInt(args[2]);

		if (args.length >= 4) {
			if ("time".equals(args[3])) {
				option = args[3];
				time_minutes = Double.parseDouble(args[4]);
			}
			if ("maxIter".equals(args[3])) {
				option = args[3];
				iter_param = Integer.parseInt(args[4]);
			}
		}
		if (args.length >= 6 && "display".equals(args[5])) {
			int disp = Integer.parseInt(args[6]);
			if (disp == 0) {
				display = false;
			} else {
				display = true;
			}
		}

		 readConfig();

		for (int i = my_seed; i < my_seed + iter; i++) {
			System.out.println("seed= " + i);
			System.out.println("pods= " + n_pods);

			System.out.println("oR= " + n_cust);

			doStuff(i, n_pods, n_cust, "FatTree");
		}
		System.out.println("-- END --");

	}

	private static void readConfig() {
		Scanner sc = null;
		try {
			sc = new Scanner(new File("CMPconfig.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		ArrayList<String> lines = new ArrayList<String>();
		while (sc.hasNext()) {
			lines.add(sc.next());
		}

		// FILLER TRESHOLD
		filler_thresh = Double.parseDouble(findValue(lines, "filler_threshold"));
		// ALFA GRASP
		alfa_grasp = Double.parseDouble(findValue(lines, "alpha_grasp"));
		// ALFA PATH
				PathRel_manager.alfa = Double.parseDouble(findValue(lines,"alpha_pathrel"));
				// BETA PATH
				PathRel_manager.beta = Double.parseDouble(findValue(lines,"beta_pathrel"));
			
				// PARALL PR
				PathRel_manager.parallelism = Integer.parseInt(findValue(lines,"pathrel_threads"));
				// PR INNER ITER
				PathRel_manager.inner_iter =Integer.parseInt(findValue(lines,"pathrel_innerIter"));
				// PR N_MOVES
				PathRel_manager.n_moves =Integer.parseInt(findValue(lines,"pathrel_moves"));
				// PR MAX TIME
				PathRel_manager.maxTime =Long.parseLong(findValue(lines,"pathrel_maxTime"));
				// PR MAX ITER
				PathRel_manager.maxIter= Integer.parseInt(findValue(lines,"pathrel_maxIter"));
				// TABOO MAXSIZE
				PathRel_manager.maxTaboo = Integer.parseInt(findValue(lines,"pathrel_maxTaboo"));
				
				GRASP_CMP_Scheme.MIGR_TIME = Double.parseDouble(findValue(lines,"migr_time"));
				GRASP_CMP_Scheme.pow_coeff = Double.parseDouble(findValue(lines,"p_coeff"));
				GRASP_CMP_Scheme.traff_coeff = Double.parseDouble(findValue(lines,"t_coeff"));
				GRASP_CMP_Scheme.migr_coeff = Double.parseDouble(findValue(lines,"m_coeff"));
				GRASP_CMP_Scheme.min_delta = Double.parseDouble(findValue(lines,"min_delta"));
				GRASP_CMP_Type1.k_paths = Integer.parseInt((findValue(lines,"k_paths")));
				GRASP_CMP_Type1.inner_cpp_iter = Integer.parseInt(findValue(lines,"inner_cpp_iter"));
				CMPMain.opt_empty = Boolean.parseBoolean((findValue(lines,"opt_empty")));
				CMPMain.opt = Boolean.parseBoolean(findValue(lines,"opt"));
				CMPMain.opt_probability = Integer.parseInt((findValue(lines,"opt_probability")));
				

				max_requests =Integer.parseInt(findValue(lines,"max_requests"));
				min_requests = Integer.parseInt(findValue(lines,"min_requests"));
				
				GRASP_CMP_Scheme.SAMPLING = Integer.parseInt(findValue(lines,"sampling"));
				GRASP_CMP_Scheme.SAMPLING_GREEDY = Integer.parseInt(findValue(lines,"sampling_greedy"));
				GRASP_CMP_Scheme.DISCARD_FACTOR = Double.parseDouble(findValue(lines,"discard"));
				GRASP_CMP_Scheme.DISCARD_FACTOR_2 = Double.parseDouble(findValue(lines,"discard_firstsol"));
				
	}

	private static String findValue(ArrayList<String> list, String key) {
		int i = 0;
		for (i = 0; i < list.size() - 1; i++) {
			if (list.get(i).equals(key)) {
				break;
			}
		}

		return list.get(i + 1);
	}

	private static void doStuff(int my_seed, int n_pods, int n_cust, String dctype) {

		Customer.cust_id = 0;
		Customer.custList.clear();
		Container.container_id = 1;

		byte[] seed = BigInteger.valueOf(my_seed).toByteArray();
		SecureRandom rng = null;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		rng.setSeed(seed);
		System.out.println("RNG: " + rng.getAlgorithm());
		Catalog.setRNG(rng);

		System.out.println("-- GENERATE DATACENTER AND REQUESTS --");
		CMPDataCenter dc = new CMPDataCenter(dctype, n_pods);
		List<Customer> customers = new ArrayList<Customer>();

		int max_app = 0;
		for (int i = 0; i < n_cust; i++) {

			customers.add(new Customer((double) (rng.nextInt(max_requests) + min_requests), Business.values()[rng.nextInt(2)], rng));
			if(customers.get(i).getContainers().size() > max_app) {
				max_app = customers.get(i).getContainers().size();
			}
		}

		System.out.println("LARGEST APP: "+max_app);
		System.out.println("-- GENERATE INITIAL PLACEMENT --");
		// FILL THE DATACENTER
		CMPtoAMPL writer = new CMPtoAMPL();
		CMPFiller filler = new DefaultCMPFiller(rng);

		try {
			filler.populate(dc, customers, (float) filler_thresh);
		} catch (PopulateException e1) {
			System.out.println("FAILED TO POPULATE - ABORT INSTANCE");
			return;
		}
		Set<Integer> set_accesi = new TreeSet<Integer>();
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					if(s.isStateON())set_accesi.add(new Integer(s.getId()));
				}
			}
		}
		System.out.println("PATHS " + dc.getPaths().size());
		if(writedat) writer.writeCMPdat_phase1(dc, Customer.custList, my_seed); // WRITE FASE 1

		int tot = 0;
		for (Customer c : Customer.custList) {
			tot += c.getContainers().size();
		}
		System.out.println("|C_bar| = " + tot);

		double totram = 0;
		double totcpu = 0;
		double res_cpu = 0;
		double res_ram = 0;

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					totram += s.getMem();
					res_ram += s.getResidual_mem();
					totcpu += s.getCpu();
					res_cpu += s.getResidual_cpu();

				}

			}
		}

		System.out.println("CPU LOAD= " + (100 - (res_cpu / totcpu) * 100) + " %");
		System.out.println("RAM LOAD= " + (100 - (res_ram / totram) * 100) + " %");

		// PREPROCESS
		Input input = preprocess(dc,rng);
		if(writedat) {
			writer.writeCMPdat_phase2(dc, Customer.custList, my_seed, input); // WRITE
			return;
		}
		// FASE 2

		int count_obl = 0;
		for (List<Container> ls : input.getClustersOBL()) {
			count_obl += ls.size();
		}
		int count_opt = 0;
		for (List<Container> ls : input.getClustersOPT()) {
			count_opt += ls.size();
		}
		System.out.println("OBL: \t" + count_obl + "\t" + input.getSinglesOBL().size());
		System.out.println("OPT: \t" + count_opt + "\t" + input.getSinglesOPT().size());

		// --------- HEURISTICS ----------

		int grasp_iter = iter_param;
		int grasp_seed = my_seed;
		double grasp_alfa = alfa_grasp;
		int grasp_time = Math.max(1, (int) (time_minutes * 60));

		// ---- CREATE ALGORITHMS ---------

		ArrayList<GRASP_CMP_Scheme> algs_v0 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v1 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v2 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v3 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v4 = new ArrayList<GRASP_CMP_Scheme>();

		algs_v0.add(new GRASP_CMP_Type2(dc, input));
		algs_v0.add(new GRASP_CMP_Type2b(dc, input));

	//	algs_v1.add(new GRASP_CMP_Type1(dc, input));
		algs_v1.add(new GRASP_CMP_Type1ALT(dc, input));
 		algs_v1.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v1) {
			gs.setComparator(new ContainerBDWComparator());
		}

	//	algs_v2.add(new GRASP_CMP_Type1(dc, input));
		algs_v2.add(new GRASP_CMP_Type1ALT(dc, input));
		algs_v2.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v2) {
			gs.setComparator(new ContainerCPUComparator());
		}

	//	algs_v3.add(new GRASP_CMP_Type1(dc, input));
		algs_v3.add(new GRASP_CMP_Type1ALT(dc, input));
		algs_v3.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v3) {
			gs.setComparator(new ContainerDISKComparator());
		}

	//	algs_v4.add(new GRASP_CMP_Type1(dc, input));
		algs_v4.add(new GRASP_CMP_Type1ALT(dc, input));
		algs_v4.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v4) {
			gs.setComparator(new ContainerRAMComparator());
		}

		// ---- SET NEIGHBORHOODS, WRAPPER and THREADS--------
		SolutionWrapper wrapper = new SolutionWrapper();
		ArrayList<CMPThread> threads = new ArrayList<CMPThread>();

		ArrayList<GRASP_CMP_Scheme> algs_all = new ArrayList<GRASP_CMP_Scheme>();
		algs_all.addAll(algs_v0);
		algs_all.addAll(algs_v1);
		algs_all.addAll(algs_v2);
		algs_all.addAll(algs_v3);
		algs_all.addAll(algs_v4);

		for (GRASP_CMP_Scheme gs : algs_all) {
			List<CMPNeighborhood> neighs = new ArrayList<CMPNeighborhood>();
			neighs.add(new CMPOneSwitchSmallIter());
			neighs.add(new CMPOneSwitchMediumIter());
		//	neighs.add(new CMPOneSwapSmallIter()); 
			        neighs.add(new CMPOneSwapSmallIterALT());
	
			gs.setNeighborhoods(neighs);

			gs.setWrapper(wrapper);
			if ("time".equals(option)) {
				threads.add(new CMPThread("time", grasp_time, grasp_seed, grasp_alfa, gs));
			} else {
				threads.add(new CMPThread("maxIter", grasp_iter, grasp_seed, grasp_alfa, gs));
			}
		}

		// -------- EXECUTE IN PARALLEL ----------
		System.out.println("-- START GRASP + LOCAL SEARCH --");
		Date d1 = new Date();
		for (CMPThread thread : threads) {
			thread.start();
		}

		try {
			synchronized (wrapper) {
				while (wrapper.getCount() < threads.size()) {

					wrapper.wait();

				}
			}
		} catch (InterruptedException e) {

		}

		Date d2 = new Date();

		System.out.println("-- END OF GRASP + LOCAL SEARCH --");
		// ------- DISPLAY RESULTS ----------
		if (display) {
			System.out.println("time = " + (d2.getTime() - d1.getTime()));
			System.out.println(" \n SOLUTIONS: \n");
			for (CPPSolution s : wrapper.getSolutions()) {
				System.out.println(s.getValue());
			}
		}

		System.out.println("BEST SOLUTION: \t" + wrapper.getBest().getValue());

		writer.writeResultsCMP(my_seed, n_pods, n_cust,(count_obl+input.getSinglesOBL().size()), (count_opt+input.getSinglesOPT().size()),wrapper, "CMPjava_results");

		if (wrapper.getBest().getValue() == Double.POSITIVE_INFINITY) {
			System.out.println("INFEASIBLE?");
			return;
		}

		System.out.println("-- START PATH RELINKING --");
		ArrayList<CPPSolution> grasp_solutions = new ArrayList<CPPSolution>();
		grasp_solutions.addAll(wrapper.getSolutions());
		Date d3 = new Date();
		CMPPath_Manager pathrel = new CMPPath_Manager(dc,Math.max(1,  grasp_solutions.size() / 2), grasp_solutions, input, rng);
		CPPSolution final_sol = pathrel.path_relinking();
		Date d4 = new Date();
		System.out.println("-- END OF PATH RELINKING --");
		if(display )System.out.println("timePR = "+(d4.getTime()-d3.getTime()));
		System.out.println("FINAL SOLUTION VALUE: \t" + final_sol.getValue());
		writer.writeResultsCMP(my_seed, n_pods, n_cust, (count_obl+input.getSinglesOBL().size()), (count_opt+input.getSinglesOPT().size()), pathrel, "CMPjava_resultsPR");

		Set<Integer> set = new TreeSet<Integer>();
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					if(s.isStateON())set.add(new Integer(s.getId()));
				}
			}
		}
		for(Container vm : final_sol.getTable().keySet()) {
			Integer s = final_sol.getTable().get(vm);
			set.add(s);
		}
		
		System.out.println("SERVER ACCESI PRE - POST: \t"+set_accesi.size()+ "\t"+set.size());
		/*
		 * CMPSolution fi_sol = (CMPSolution) final_sol;
		 * for(Container m :fi_sol.getTable().keySet()) {
		 * System.out.println(m.getId()+": \t"+(m.getState()/GRASP_CMP_Scheme.MIGR_TIME)
		 * +"GB \t from "+dc.getPlacement().get(m).getId()+" to "+fi_sol.getTable().get(
		 * m).intValue());
		 * System.out.println("Path: \t"+fi_sol.getFlows().get(m).toString()); }
		 */
	}

	public static Input preprocess(CMPDataCenter dc, SecureRandom rng) {

		GreedyInverseKnapsack knaps = new GKS2();
		knaps.setRng(rng);
		List<List<Container>> clusters_obl = new ArrayList<List<Container>>();
		List<List<Container>> clusters_opt = new ArrayList<List<Container>>();
		List<Container> singles_obl = new ArrayList<Container>();
		List<Container> singles_opt = new ArrayList<Container>();

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					knaps.setServer(s);
					knaps.process();
					List<Container> my_obl = knaps.getMandatory();
					List<Container> my_opt = knaps.getOptional();

					for (Container v : my_obl) {
						s.deallocateContainer(v);
						Customer cust = Customer.custList.get(v.getMy_customer());
						cust.getContainers().remove(v);
						cust.getNewContainers().add(v);

						Double to_t0 = cust.getTraffic().get(new C_Couple(v, Container.c_0));
						Double from_s0 = cust.getTraffic().get(new C_Couple(Container.c_0, v));

						if (to_t0 != null) {
							List<Link> path_0 = dc.getTo_wan().get(s);
							for (Link l : path_0) {
								l.setResidCapacity(l.getResidCapacity() + to_t0.doubleValue());
							}
						}
						if (from_s0 != null) {
							List<Link> path_0 = dc.getFrom_wan().get(s);
							for (Link l : path_0) {
								l.setResidCapacity(l.getResidCapacity() + from_s0.doubleValue());
							}
						}

						for (Container v2 : cust.getContainers()) {
							Double t1 = cust.getTraffic().get(new C_Couple(v, v2));
							Double t2 = cust.getTraffic().get(new C_Couple(v2, v));
							if (t1 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if (s2 == s)
									continue;
					//			List<Link> p1 = dc.getPaths().get(new S_Couple(s, s2));
								List<Link> p1 = dc.getPath(s, s2);
								for (Link l : p1) {
									l.setResidCapacity(l.getResidCapacity() + t1.doubleValue());
								}
							}
							if (t2 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if (s2 == s)
									continue;
						//		List<Link> p2 = dc.getPaths().get(new S_Couple(s2, s));
								List<Link> p2 = dc.getPath(s2, s);
								for (Link l : p2) {
									l.setResidCapacity(l.getResidCapacity() + t2.doubleValue());
								}
							}
						}

					}
					for (Container v : my_opt) {
						s.deallocateContainer(v);
						Customer cust = Customer.custList.get(v.getMy_customer());
						cust.getContainers().remove(v);
						cust.getNewContainers().add(v);
						for (Container v2 : cust.getContainers()) {
							Double t1 = cust.getTraffic().get(new C_Couple(v, v2));
							Double t2 = cust.getTraffic().get(new C_Couple(v2, v));
							if (t1 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if (s2 == s)
									continue;
						//		List<Link> p1 = dc.getPaths().get(new S_Couple(s, s2));
								List<Link> p1 = dc.getPath(s, s2);
								for (Link l : p1) {
									l.setResidCapacity(l.getResidCapacity() + t1.doubleValue());
								}
							}
							if (t2 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if (s2 == s)
									continue;
						//		List<Link> p2 = dc.getPaths().get(new S_Couple(s2, s));
								List<Link> p2 = dc.getPath(s2, s);
								for (Link l : p2) {
									l.setResidCapacity(l.getResidCapacity() + t2.doubleValue());
								}
							}
						}

					}

					clustering(my_obl, singles_obl, clusters_obl);
					clustering(my_opt, singles_opt, clusters_opt);

				}
			}
		}

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					s.updateBandwidth();
				}
			}
		}

		return new Input(clusters_obl, singles_obl, clusters_opt, singles_opt);

	}

	private static void clustering(List<Container> input, List<Container> singles, List<List<Container>> clusters) {

		while (!input.isEmpty()) {
			Container repr = input.remove(0);
			List<Container> my_cluster = new ArrayList<Container>();
			for (Container v : input) {
				if (repr.getMy_customer() == v.getMy_customer()) {
					my_cluster.add(v);
				}
			}
			if (!my_cluster.isEmpty()) {
				input.removeAll(my_cluster);
				my_cluster.add(repr);
				clusters.add(my_cluster);
			} else {
				singles.add(repr);
			}
		}
	}
}
