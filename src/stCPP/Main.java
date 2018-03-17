package stCPP;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

import cpp_heuristics.*;
import general.*;
import writeFiles.CPPtoAMPL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import cmp_heuristics.GRASP_CMP_Scheme;

public class Main {

   public static boolean writedat = false;
    public static boolean display = false;
	public static String option = "time";
	public static int iter_param = 10;
	public static double time_minutes = 0.5;
	public static int max_requests = 3600;
	public static int min_requests = 400;
	
	public static double filler_thresh = 0.9;
	public static double alfa_grasp = 0.15;
	public static boolean ram_indexing = false;
	
	public static void main(String[] args) {

		System.out.println("-- START --");
		int iter =1;
		int my_seed = 9004;
		int n_newcust = 7;
		int n_cust = 10000;
		int n_newcont = 100;
		int n_pods =34;

		if (args.length >= 1)
			my_seed = Integer.parseInt(args[0]);
		if (args.length >= 2)
			n_pods = Integer.parseInt(args[1]);
		if (args.length >= 3)
			n_newcont = Integer.parseInt(args[2]);
		if (args.length >= 4)
			n_newcust = Integer.parseInt(args[3]);
		if (args.length >= 5)
			n_cust = Integer.parseInt(args[4]);
		if (args.length >= 6) {
			if ("time".equals(args[5])) {
				option = args[5];
				time_minutes = Double.parseDouble(args[6]);
			}
			if ("maxIter".equals(args[5])) {
				option = args[5];
				iter_param = Integer.parseInt(args[6]);
			}
		}
		if (args.length >= 8 && "display".equals(args[7])) {
			int disp = Integer.parseInt(args[8]);
			if (disp == 0) {
				display = false;
			} else {
				display = true;
			}
		}
		
		readConfig();

		for (int i = my_seed; i < my_seed + iter; i++) {
			System.out.println("seed= " + i);
			System.out.println("pods= "+n_pods);
			System.out.println("C= " + n_newcont);
			System.out.println("nR= " + n_newcust);
			System.out.println("oR= " + n_cust);

			doStuff(i, n_pods, n_cust, n_newcust, n_newcont, "FatTree");
		}
		System.out.println("-- END --");
	}

	private static void doStuff(int my_seed, int n_pods, int n_cust, int n_newcust, int n_newcont, String dctype)  {

		Customer.cust_id = 0;
		Customer.custList.clear();
		Container.container_id = 1;

		byte[] seed = BigInteger.valueOf(my_seed).toByteArray();
	//	System.out.println("byteseed: "+seed);
		SecureRandom rng = null;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		rng.setSeed(seed);
		System.out.println("RNG: "+rng.getAlgorithm());
		Catalog.setRNG(rng);

		System.out.println("-- GENERATE DATACENTER AND REQUESTS --");
		DataCenter dc = new DataCenter(dctype, n_pods);
		ArrayList<Customer> customers = new ArrayList<Customer>();

		for (int i = 0; i < n_cust; i++) {

			customers.add(new Customer((double) (rng.nextInt(max_requests) + min_requests), Business.values()[rng.nextInt(2)], rng));
		}

		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for (int i = 0; i < n_newcust; i++) {

			new_customers.add(new Customer((double) (rng.nextInt(max_requests) + min_requests), Business.values()[rng.nextInt(2)], rng));

			new_customers.get(i).transformIntoNew();
		}

		int howmanyalready = 0;
		for (Customer c : new_customers) {
			howmanyalready += c.getNewContainers().size();
		}

		int rnd = rng.nextInt(Customer.custList.size());

		for (int i = 0; i < n_newcont - howmanyalready; i++) {
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

		}

		int count = 0;
		int max_app = 0;
		for (Customer c : customers) {
			count += c.getNewContainers().size();
			if(c.getContainers().size()+c.getNewContainers().size() > max_app) {
				max_app = c.getContainers().size()+c.getNewContainers().size();
			}
		}
		for (Customer c : new_customers) {
			count += c.getNewContainers().size();
			if(c.getNewContainers().size() > max_app) {
				max_app = c.getNewContainers().size();
			}
		}
		System.out.println("LARGEST APP: "+max_app);
		if (display) {
			System.out.println("\n new containers: " + count);
		}

		System.out.println("-- GENERATE INITIAL PLACEMENT --");
		// FILL THE DATACENTER
		DC_filler filler = new FirstFit();
		filler = new RackFiller(rng);
		try {
			filler.populate(dc, customers, (float) filler_thresh);
		} catch (PopulateException e1) {
			System.out.println("FAILED TO POPULATE - ABORT INSTANCE");
			return;
		}

		int count_s_u = 0;
		
			for (Pod p : dc.getPods()) {
				for (Rack r : p.getRacks()) {
					for (Server s : r.getHosts()) {
					if(display)	System.out.println(s.toString());

						if (s.isUnderUtilized())
							count_s_u++;
					}
				}
			}

			int tot = 0;
			for (Customer c : Customer.custList) {
				tot += c.getContainers().size();
			}
			System.out.println("|C_bar| = " + tot);

			System.out.println("s_u " + count_s_u);

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

		
		CPPtoAMPL writer = new CPPtoAMPL();
		if(writedat) {
	    writer.writeCPPdat(dc, customers, new_customers, my_seed);
	    return;
		}

		System.out.println("-- END OF PRE-PROCESSING --");
		// --------- HEURISTICS ----------

		int grasp_iter = iter_param;
		int grasp_seed = my_seed;
		float grasp_alfa = (float) alfa_grasp;
		int grasp_time =Math.max(1, (int) (time_minutes * 60));

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
			if(ram_indexing) {
			   gs.setIndexing(tree);
			}
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
		 writer.writeResults(my_seed, n_pods, n_newcont, n_newcust, n_cust,
	wrapper,"java_results");
	
		 /*
		 System.out.println("-- START PATH RELINKING --");		 
		ArrayList<CPPSolution> grasp_solutions = new ArrayList<CPPSolution>();
		grasp_solutions.addAll(wrapper.getSolutions());
		Date d3 = new Date();
		PathRel_manager pathrel = new PathRel_manager(dc, grasp_solutions.size() * 2, grasp_solutions, rng);
		CPPSolution final_sol = pathrel.path_relinking();
		Date d4 = new Date();
		System.out.println("-- END OF PATH RELINKING --");
		System.out.println("FINAL SOLUTION VALUE: \t" + final_sol.getValue());
		 writer.writeResults(my_seed, n_pods, n_newcont, n_newcust, n_cust,
		final_sol.getValue(),0,d4.getTime()-d3.getTime(),"java_resultsPR");
		*/
	}
	
	
	
	private static void readConfig() {
		Scanner sc = null;
		try {
			 sc = new Scanner(new File("config.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		ArrayList<String> lines = new ArrayList<String>();
		while(sc.hasNext()) {
			lines.add(sc.next());
		}
		
		// FILLER TRESHOLD
		filler_thresh = Double.parseDouble(findValue(lines,"filler_threshold"));
		// ALFA GRASP
		alfa_grasp =  Double.parseDouble(findValue(lines,"alpha_grasp"));
		// ALFA PATH
		PathRel_manager.alfa = Double.parseDouble(findValue(lines,"alpha_pathrel"));
		// BETA PATH
		PathRel_manager.beta = Double.parseDouble(findValue(lines,"beta_pathrel"));
		// INDEXING
		ram_indexing = (Integer.parseInt(findValue(lines,"ram_index")) == 0)? false : true;
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
		
		max_requests =Integer.parseInt(findValue(lines,"max_requets"));
		min_requests = Integer.parseInt(findValue(lines,"min_requests"));
		
		GRASP_CPP_Scheme.min_delta = Double.parseDouble(findValue(lines,"min_delta"));
	}
	
	private static String findValue(List<String> list, String key) {
		int i = 0;
		for(i=0; i<list.size()-1;i++) {
			if(list.get(i).equals(key)) {
				break;
			}
		}
		
		return list.get(i+1);
	}
}
