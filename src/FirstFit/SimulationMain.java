package FirstFit;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cmp_heuristics.CMPNeighborhood;
import cmp_heuristics.CMPOneSwapSmallIter;
import cmp_heuristics.CMPOneSwitchMediumIter;
import cmp_heuristics.CMPOneSwitchSmallIter;
import cmp_heuristics.CMPPath_Manager;
import cmp_heuristics.CMPSolution;
import cmp_heuristics.CMPThread;
import cmp_heuristics.GRASP_CMP_Scheme;
import cmp_heuristics.GRASP_CMP_Type1;
import cmp_heuristics.GRASP_CMP_Type1b;
import cmp_heuristics.GRASP_CMP_Type2;
import cmp_heuristics.GRASP_CMP_Type2b;
import cmp_heuristics.Input;
import cpp_heuristics.CPPNeighborhood;
import cpp_heuristics.CPPOneSwapIter;
import cpp_heuristics.CPPOneSwapSmallIter;
import cpp_heuristics.CPPOneSwitchMediumIter;
import cpp_heuristics.CPPOneSwitchSmallIter;
import cpp_heuristics.CPPSolution;
import cpp_heuristics.CPPThread;
import cpp_heuristics.ContainerBDWComparator;
import cpp_heuristics.ContainerCPUComparator;
import cpp_heuristics.ContainerDISKComparator;
import cpp_heuristics.ContainerRAMComparator;
import cpp_heuristics.ContainerTimeStampComparator;
import cpp_heuristics.GRASP_CPP_Scheme;
import cpp_heuristics.GRASP_CPP_Type1Indexing;
import cpp_heuristics.GRASP_CPP_Type1b;
import cpp_heuristics.GRASP_CPP_Type1c;
import cpp_heuristics.GRASP_CPP_Type2;
import cpp_heuristics.GRASP_CPP_Type2b;
import cpp_heuristics.GRASP_CPP_Type2c;
import cpp_heuristics.GRASP_CPP_Type3;
import cpp_heuristics.GRASP_CPP_Type3b;
import cpp_heuristics.GRASP_CPP_Type3c;
import cpp_heuristics.GRASP_CPP_Type4;
import cpp_heuristics.GRASP_CPP_Type4b;
import cpp_heuristics.GRASP_CPP_Type4c;
import cpp_heuristics.InfeasibilityException;
import cpp_heuristics.SolutionWrapper;
import cpp_heuristics.TreeIndex;
import cpp_heuristics.TreeIndexUtilities;
import general.Business;
import general.CMPDataCenter;
import general.C_Couple;
import general.Catalog;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Link;
import general.Node;
import general.Pod;
import general.Rack;
import general.S_Couple;
import general.Server;
import ltCMP.CMPMain;

public class SimulationMain {

	static SecureRandom rng;
	static int max_requests = 3600;
	static int min_requests = 400;
	static boolean display = false;
	private static int iter_param_cmp;
	private static int iter_param_cpp;
	private static double alfa_grasp;
	private static int time_minutes_cmp;
	private static int time_minutes_cpp;
	private static Object option;
	static int migr_window = 3600;
	
	static HashMap<Customer, List<Container>> frozen = new HashMap<Customer, List<Container>>();
	
	public static void main(String[] args) {

		int my_seed = 0;
		int pod = 6;
		int batch = 100;
		int total = 1000;
		String algorithm = "grasp";

		boolean migration = true;
		if(algorithm.equals("firstfit")) migration = false;
		
		
		readConfig();

		byte[] seed = BigInteger.valueOf(my_seed).toByteArray();
		// System.out.println("byteseed: "+seed);
		rng = null;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		rng.setSeed(seed);
		System.out.println("RNG: " + rng.getAlgorithm());
		Catalog.setRNG(rng);
		DataCenter dc = null;
		if (migration) {
			dc = new CMPDataCenter("Fattree", pod);
		} else {
			dc = new DataCenter("Fattree", pod);
		}

		
		
		
		int partial = 0;
		Date d1 = new Date();
		Date d2 = new Date();
		
		
		
		while (partial < total) {

			partial += generateRequests(batch);
			CPPSolution sol = solveProblem(dc,algorithm,my_seed);
			
			if(sol.getValue() == Double.POSITIVE_INFINITY) {
				
				System.out.println("CMP TRIGGERED BY INFEASIBILITY");
				for(Customer cm : Customer.custList) {
					List<Container> ls = cm.getNewContainers();
					if(!ls.isEmpty()) {
						List<Container> tmp = new ArrayList<Container>(ls);
						frozen.put(cm, tmp);
						partial -= ls.size();
						ls.clear();
					}
				}
				
				
				CMPSolution solcmp;
				
				try {
					solcmp = solveCMP((CMPDataCenter)dc,my_seed);
				} catch (InfeasibilityException e) {
					
					e.printStackTrace();
					System.out.println("ABORT- CMP FAILED");
					return;
				}
				postprocessingCMP(dc,solcmp);
				d2 = new Date();
			
			}else {
			     postprocessingCPP(dc,sol);
			}
			if ((new Date().getTime() - d2.getTime()) / 1000 > migr_window && migration) {
				
				System.out.println("CMP TRIGGERED BY TIME");
				CMPSolution solcmp;
				try {
					solcmp = solveCMP((CMPDataCenter)dc,my_seed);
				} catch (InfeasibilityException e) {
					
					e.printStackTrace();
					System.out.println("ABORT- CMP FAILED");
					return;
				}
				postprocessingCMP(dc,solcmp);
				d2 = new Date();
			}

		}
		
		
		evaluateFinalConfiguration();
	}

	private static int generateRequests(int batch) {

		int count = 0;
		
		if(!frozen.isEmpty()) {
			for(Customer cm : frozen.keySet()) {
				cm.getNewContainers().addAll(frozen.get(cm));
				count += frozen.get(cm).size();
			}
				
			frozen.clear();
			return count;	
		}
		
	
		if (!Customer.custList.isEmpty()) {
			int rnd = rng.nextInt(Customer.custList.size());
			for (int i = 0; i < batch / 2; i++) {
				if (rng.nextInt(3) <= 1) {
					// dont update rnd - customer
				} else {
					rnd = rng.nextInt(Customer.custList.size());
				}

				int tmp = rng.nextInt(3);

				switch (tmp) {

				case 0:
					Customer.custList.get(rnd).addWS();
					break;
				case 1:
					Customer.custList.get(rnd).addAS();
					break;
				case 2:
					Customer.custList.get(rnd).addDBMS();
					break;
				}

				count+=1;
			}
		}
		
		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		while(count < batch) {

			Customer n = new Customer((double) (rng.nextInt(max_requests) + min_requests),
					Business.values()[rng.nextInt(2)], rng);
			new_customers.add(n);
			n.transformIntoNew();
			count += n.getNewContainers().size();
		}
		
		return count;
	}

	private static CMPSolution solveCMP(CMPDataCenter dc, int my_seed) throws InfeasibilityException {


		Input input = CMPMain.preprocess(dc, rng);
		
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

		int grasp_iter = iter_param_cmp;
		int grasp_seed = my_seed;
		double grasp_alfa = alfa_grasp;
		int grasp_time = Math.max(1, (int) (time_minutes_cmp * 60));

		// ---- CREATE ALGORITHMS ---------

		ArrayList<GRASP_CMP_Scheme> algs_v0 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v1 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v2 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v3 = new ArrayList<GRASP_CMP_Scheme>();
		ArrayList<GRASP_CMP_Scheme> algs_v4 = new ArrayList<GRASP_CMP_Scheme>();

		algs_v0.add(new GRASP_CMP_Type2(dc, input));
		algs_v0.add(new GRASP_CMP_Type2b(dc, input));

		algs_v1.add(new GRASP_CMP_Type1(dc, input));
     	algs_v1.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v1) {
			gs.setComparator(new ContainerBDWComparator());
		}

		algs_v2.add(new GRASP_CMP_Type1(dc, input));
		algs_v2.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v2) {
			gs.setComparator(new ContainerCPUComparator());
		}

		algs_v3.add(new GRASP_CMP_Type1(dc, input));
		algs_v3.add(new GRASP_CMP_Type1b(dc, input));
		for (GRASP_CMP_Scheme gs : algs_v3) {
			gs.setComparator(new ContainerDISKComparator());
		}

		algs_v4.add(new GRASP_CMP_Type1(dc, input));
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
			neighs.add(new CMPOneSwapSmallIter());
			// neighs.add(new CMPOneSwapIter());
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


		if (wrapper.getBest().getValue() == Double.POSITIVE_INFINITY) {
			System.out.println("INFEASIBLE?");
			throw new InfeasibilityException(wrapper.getBest());
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
		return (CMPSolution)final_sol;
		
		
	}

	private static void postprocessingCPP(DataCenter dc, CPPSolution solcpp) {
		
		for(Customer cm: Customer.custList) {
			cm.transformIntoOld();
		}
		
		for(Container vm: solcpp.getTable().keySet()) {
			int tmp = solcpp.getTable().get(vm).intValue();
			Server s = dc.findServer(tmp);
			s.allocateContainer(vm);
			dc.getPlacement().put(vm, s);
		}
		
		for(Pod p: dc.getPods()) {
			for(Rack r:p.getRacks()) {
				for(Server s: r.getHosts()) {
					s.updateBandwidth();
				}
			}
		}
		
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

		

	}

	private static void postprocessingCMP(DataCenter mydc, CMPSolution solcmp) {
		postprocessingCPP(mydc,solcmp);
		CMPDataCenter dc = (CMPDataCenter) mydc;
	
		
		DefaultDirectedWeightedGraph<Node,Link> g = dc.getNetwork();
		DijkstraShortestPath<Node,Link> alg = new DijkstraShortestPath<Node,Link>(g);
		ArrayList<Server> servs = new ArrayList<Server>();
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					servs.add(s);
				}
			}
		}
		
		for(Server s: servs) {
			List<Container> vms = s.getContainers();
			HashMap <Server, Double> set = new HashMap<Server, Double>();
			double to_t0 = 0;
			double from_s0 = 0;
			
			for(Container v:vms) {
				Customer r = Customer.custList.get(v.getMy_customer());
				for(Container c:r.getContainers()) {
					Double t = r.getTraffic().get(new C_Couple(v,c));
					if(t != null) {
						Double tmp = set.get(dc.getPlacement().get(c));
						double _tmp = (tmp == null)? 0 : tmp.doubleValue();
						set.put(dc.getPlacement().get(c),_tmp + t.doubleValue() );
					}
				}
				
				Double c_c0 = r.getTraffic().get(new C_Couple(v,Container.c_0));
				to_t0 += (c_c0 == null)? 0 : c_c0.doubleValue();
				Double c0_c = r.getTraffic().get(new C_Couple(Container.c_0, v));
				from_s0 += (c0_c == null)? 0 : c0_c.doubleValue(); 
			}
			
			
			// UPDATE LINKS AND PATHS
			
			// TRAFFIC WITH C_0
			GraphPath<Node,Link> path_0 =alg.getPath(s, dc.t_0);
			
			for(Link l : path_0.getEdgeList()) {
				if(l.getResidCapacity() == Double.POSITIVE_INFINITY) continue;
				l.setResidCapacity(l.getResidCapacity() - to_t0);
				
		//		System.out.println(l.getResCapacity());
				g.setEdgeWeight(l, 1/(l.getResidCapacity() + CMPDataCenter.inv_offset  ));
			}
			dc.getTo_wan().remove(s);
			dc.getTo_wan().put(s, path_0.getEdgeList());
			
			path_0 = alg.getPath(dc.s_0, s);
			for(Link l : path_0.getEdgeList()) {
				if(l.getResidCapacity() == Double.POSITIVE_INFINITY) continue;
				l.setResidCapacity(l.getResidCapacity() - from_s0);
				
			//	System.out.println(l.getResCapacity());
				g.setEdgeWeight(l, 1/(l.getResidCapacity() + CMPDataCenter.inv_offset ));
				
			}
			dc.getFrom_wan().remove(s);
			dc.getFrom_wan().put(s, path_0.getEdgeList());
			
			// OTHER TRAFFIC
		    for(Server t : set.keySet()) {
		    	
		    		GraphPath<Node,Link> path =alg.getPath(s, t);
		    		for(Link l : path.getEdgeList()) {
		    			l.setResidCapacity(l.getResidCapacity() - set.get(t).doubleValue());
		    			
		    			g.setEdgeWeight(l, 1/(l.getResidCapacity() + CMPDataCenter.inv_offset ));
		    		}
		    		
		    		dc.getPaths().remove(new S_Couple(s,t));		    		
		    		dc.getPaths().put(new S_Couple(s,t), path.getEdgeList());		    		
		    		dc.getCosts()[s.getId()][t.getId()] = path.getEdgeList().size()-1;
		    	
		    }
		    
		    
		}
		
		
		

	}

	private static CPPSolution solveProblem(DataCenter dc, String algorithm, int my_seed) {

		if(algorithm.equals("firstfit")) {
			FirstFitHeur ff = new FirstFitHeur(dc);
			List<Container> toPlace = new ArrayList<Container>();
			for(Customer cust : Customer.custList) {
				toPlace.addAll(cust.getNewContainers());
			}
			return ff.findSolution(toPlace);
			
			
		}else {
			
			int grasp_iter = iter_param_cpp;
			int grasp_seed = my_seed;
			float grasp_alfa = (float) alfa_grasp;
			int grasp_time =Math.max(1, (int) (time_minutes_cpp * 60));

			// ---------CREATE INDEXING------------
			ArrayList<Server> machines = new ArrayList<Server>();
			for (Pod p : dc.getPods()) {
				for (Rack r : p.getRacks()) {
					for (Server s : r.getHosts()) {
						machines.add(s);
					}
				}
			}
			ArrayList<Container> requests = new ArrayList<Container>();
			for (Customer c : Customer.custList) {
				requests.addAll(c.getNewContainers());
			}
			List<Server> feasibles = TreeIndexUtilities.feasibleServersApprox(requests, machines);
			TreeIndex tree = TreeIndexUtilities.createRAMIndex(feasibles);

			// ---- CREATE ALGORITHMS ---------

			ArrayList<GRASP_CPP_Scheme> algs_v0 = new ArrayList<GRASP_CPP_Scheme>();
			ArrayList<GRASP_CPP_Scheme> algs_v1 = new ArrayList<GRASP_CPP_Scheme>();
			ArrayList<GRASP_CPP_Scheme> algs_v2 = new ArrayList<GRASP_CPP_Scheme>();
			ArrayList<GRASP_CPP_Scheme> algs_v3 = new ArrayList<GRASP_CPP_Scheme>();
			ArrayList<GRASP_CPP_Scheme> algs_v4 = new ArrayList<GRASP_CPP_Scheme>();
			ArrayList<GRASP_CPP_Scheme> algs_v5 = new ArrayList<GRASP_CPP_Scheme>();

			algs_v0.add(new GRASP_CPP_Type1Indexing(dc));
			algs_v0.add(new GRASP_CPP_Type1b(dc));
			algs_v0.add(new GRASP_CPP_Type1c(dc));
			algs_v0.add(new GRASP_CPP_Type2(dc));
			algs_v0.add(new GRASP_CPP_Type2b(dc));
			algs_v0.add(new GRASP_CPP_Type2c(dc));

			algs_v1.add(new GRASP_CPP_Type3(dc));
			algs_v1.add(new GRASP_CPP_Type4(dc));
			algs_v1.add(new GRASP_CPP_Type3b(dc));
			algs_v1.add(new GRASP_CPP_Type3c(dc));
			algs_v1.add(new GRASP_CPP_Type4b(dc));
			algs_v1.add(new GRASP_CPP_Type4c(dc));
			for (GRASP_CPP_Scheme gs : algs_v1) {
				gs.setComparator(new ContainerBDWComparator());
			}
			algs_v2.add(new GRASP_CPP_Type3(dc));
			algs_v2.add(new GRASP_CPP_Type4(dc));
			algs_v2.add(new GRASP_CPP_Type3b(dc));
			algs_v2.add(new GRASP_CPP_Type3c(dc));
			algs_v2.add(new GRASP_CPP_Type4b(dc));
			algs_v2.add(new GRASP_CPP_Type4c(dc));
			for (GRASP_CPP_Scheme gs : algs_v2) {
				gs.setComparator(new ContainerCPUComparator());
			}
			algs_v3.add(new GRASP_CPP_Type3(dc));
			algs_v3.add(new GRASP_CPP_Type4(dc));
			algs_v3.add(new GRASP_CPP_Type3b(dc));
			algs_v3.add(new GRASP_CPP_Type3c(dc));
			algs_v3.add(new GRASP_CPP_Type4b(dc));
			algs_v3.add(new GRASP_CPP_Type4c(dc));
			for (GRASP_CPP_Scheme gs : algs_v3) {
				gs.setComparator(new ContainerDISKComparator());
			}
			algs_v4.add(new GRASP_CPP_Type3(dc));
			algs_v4.add(new GRASP_CPP_Type4(dc));
			algs_v4.add(new GRASP_CPP_Type3b(dc));
			algs_v4.add(new GRASP_CPP_Type3c(dc));
			algs_v4.add(new GRASP_CPP_Type4b(dc));
			algs_v4.add(new GRASP_CPP_Type4c(dc));
			for (GRASP_CPP_Scheme gs : algs_v4) {
				gs.setComparator(new ContainerRAMComparator());
			}
			algs_v5.add(new GRASP_CPP_Type3(dc));
			algs_v5.add(new GRASP_CPP_Type4(dc));
			algs_v5.add(new GRASP_CPP_Type3b(dc));
			algs_v5.add(new GRASP_CPP_Type3c(dc));
			algs_v5.add(new GRASP_CPP_Type4b(dc));
			algs_v5.add(new GRASP_CPP_Type4c(dc));
			for (GRASP_CPP_Scheme gs : algs_v5) {
				gs.setComparator(new ContainerTimeStampComparator());
			}
			// ---- SET NEIGHBORHOODS, WRAPPER and THREADS--------
			SolutionWrapper wrapper = new SolutionWrapper();
			ArrayList<CPPThread> threads = new ArrayList<CPPThread>();

			ArrayList<GRASP_CPP_Scheme> algs_all = new ArrayList<GRASP_CPP_Scheme>();
			algs_all.addAll(algs_v0);
			algs_all.addAll(algs_v1);
			algs_all.addAll(algs_v2);
			algs_all.addAll(algs_v3);
			algs_all.addAll(algs_v4);
			algs_all.addAll(algs_v5);

			for (GRASP_CPP_Scheme gs : algs_all) {
				ArrayList<CPPNeighborhood> neighs = new ArrayList<CPPNeighborhood>();
				neighs.add(new CPPOneSwitchSmallIter());
				neighs.add(new CPPOneSwitchMediumIter());
				neighs.add(new CPPOneSwapSmallIter());
				neighs.add(new CPPOneSwapIter());
				gs.setNeighborhoods(neighs);
				
				gs.setWrapper(wrapper);
				if ("time".equals(option)) {
					threads.add(new CPPThread("time", grasp_time, grasp_seed, grasp_alfa, gs));
				} else {
					threads.add(new CPPThread("maxIter", grasp_iter, grasp_seed, grasp_alfa, gs));
				}
			}

			// -------- EXECUTE IN PARALLEL ----------
			System.out.println("-- START GRASP + LOCAL SEARCH --");
			Date d1 = new Date();
			for (CPPThread thread : threads) {
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
			return wrapper.getBest();
			
			
			
		}
	}

}
