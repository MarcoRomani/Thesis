package stCPP;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import cpp_heuristics.*;
import general.*;
import writeFiles.CPPtoAMPL;
import java.util.Date;
import java.util.List;

public class Main {

	public static void main(String[] args) {

		int iter = 1;
		int my_seed = 264;
		int n_newcust = 4;
		int n_cust = 8000;
		int n_newcont = 100;
		int n_pods = 34;

		for (int i = my_seed; i < my_seed + iter; i++) {
			doStuff(i, n_pods, n_cust, n_newcust, n_newcont, "FatTree");
		}
	}

	private static void doStuff(int my_seed, int n_pods, int n_cust, int n_newcust, int n_newcont, String dctype) {

		Customer.cust_id = 0;
		Customer.custList.clear();
		Container.container_id = 1;

		byte[] seed = BigInteger.valueOf(my_seed).toByteArray();
		SecureRandom rng = new SecureRandom(seed); // SHA1PRNG
		Catalog.setRNG(rng);

		DataCenter dc = new DataCenter(dctype, n_pods);
		ArrayList<Customer> customers = new ArrayList<Customer>();

		for (int i = 0; i < n_cust; i++) {
		
			customers.add(new Customer(((double) (rng.nextInt(3500) + 400) / 1000000) * 8,
					Business.values()[rng.nextInt(2)], rng));
		}

		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for (int i = 0; i < n_newcust; i++) {
		
			new_customers.add(new Customer(((double) (rng.nextInt(3500) + 400) / 1000000) * 8,
					Business.values()[rng.nextInt(2)], rng));

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
		for (Customer c : customers) {
			count += c.getNewContainers().size();
		}
		for (Customer c : new_customers) {
			count += c.getNewContainers().size();
		}
		System.out.println("\n new containers: " + count);

		// FILL THE DATACENTER
		DC_filler filler = new FirstFit();
		filler = new RackFiller(rng);
		filler.populate(dc, customers, (float) 0.9);

		int countDiskFeas=0;
		int countRAMFeas=0;
		int countCPUFeas =0;
		int count_s_u = 0;
		double maxDisk = 0;
		double maxRAM = 0;
		double maxCPU = 0;
		
		for(Customer c: Customer.custList) {
			for(Container vm: c.getNewContainers()) {
				if(vm.getCpu() > maxCPU) maxCPU = vm.getCpu();
				if(vm.getMem() > maxRAM) maxRAM = vm.getMem();
				if(vm.getDisk() > maxDisk) maxDisk = vm.getDisk();
			}
		}
		
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					System.out.println(s.toString());
					if(s.getResidual_mem() > maxRAM) countRAMFeas++;
					if(s.getResidual_cpu() > maxCPU) countCPUFeas++;
					if(s.getResidual_disk() > maxDisk) countDiskFeas++;
					if(s.isUnderUtilized()) count_s_u++;
				}
			}
		}

		
		CPPtoAMPL writer = new CPPtoAMPL();
		// writer.writeCPPdat(dc, customers, new_customers, my_seed);

		// --------- HEURISTICS ----------

		int grasp_iter = 10;
		int grasp_seed = my_seed;
		float grasp_alfa = (float) 0.1;

		// ---------CREATE INDEXING------------
		ArrayList<Server> machines = new ArrayList<Server>();
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					machines.add(s);
				}
			}
		}
		ArrayList<Container> requests = new ArrayList<Container>();
		for(Customer c: Customer.custList) {
			requests.addAll(c.getNewContainers());
		}
		List<Server> feasibles = TreeIndexUtilities.feasibleServersApprox(requests, machines);
		TreeIndex tree = TreeIndexUtilities.createRAMIndex(feasibles);
		
		// ---- CREATE ALGORITHMS ---------

		ArrayList<GRASP_CPP_Scheme> algs_v1 = new ArrayList<GRASP_CPP_Scheme>();
		ArrayList<GRASP_CPP_Scheme> algs_v2 = new ArrayList<GRASP_CPP_Scheme>();

		algs_v1.add(new GRASP_CPP_Type1(dc));
		algs_v1.add(new GRASP_CPP_Type2(dc));
		algs_v1.add(new GRASP_CPP_Type3(dc));
		algs_v1.add(new GRASP_CPP_Type4(dc));

		algs_v2.add(new GRASP_CPP_Type1(dc));
		algs_v2.add(new GRASP_CPP_Type2(dc));
		algs_v2.add(new GRASP_CPP_Type3(dc));
		algs_v2.add(new GRASP_CPP_Type4(dc));

		// ---- SET NEIGHBORHOODS, WRAPPER and THREADS--------
		SolutionWrapper wrapper = new SolutionWrapper();
		ArrayList<CPPThread> threads = new ArrayList<CPPThread>();

		for (GRASP_CPP_Scheme gs : algs_v1) {
			ArrayList<CPPNeighborhood> neighs = new ArrayList<CPPNeighborhood>();
			neighs.add(new CPPOneSwitchSmallIter());
			neighs.add(new CPPOneSwitchMediumIter());
			neighs.add(new CPPOneSwapSmallIter());
			neighs.add(new CPPOneSwapIter());
			gs.setNeighborhoods(neighs);
			gs.setIndexing(tree);
			gs.setWrapper(wrapper);
			threads.add(new CPPThread(grasp_iter, grasp_seed, grasp_alfa, gs));
		}

		for (GRASP_CPP_Scheme gs : algs_v2) {
			ArrayList<CPPNeighborhood> neighs = new ArrayList<CPPNeighborhood>();
			neighs.add(new CPPOneSwitchMediumIter());
			neighs.add(new CPPOneSwapIter());
			gs.setNeighborhoods(neighs);
			gs.setIndexing(tree);
			gs.setWrapper(wrapper);
			threads.add(new CPPThread(grasp_iter, grasp_seed, grasp_alfa, gs));
		}

		// -------- EXECUTE IN PARALLEL ----------
		Date d1 = new Date();
		for (CPPThread thread : threads) {
			thread.start();
		}

		int thread_counter = 0;
		while (thread_counter < threads.size()) {
			try {
				synchronized (wrapper) {

					wrapper.wait();
					thread_counter++;

				}
			} catch (InterruptedException e) {

			}

		}

		Date d2 = new Date();
		
		//------- DISPLAY RESULTS ----------
		for (CPPSolution s : wrapper.getSolutions()) {
			System.out.println(s.getValue());
		}
		System.out.println(wrapper.getBest().toString());
		System.out.println("time = " + (d2.getTime() - d1.getTime()));

		/*
		 * Date d4 = new Date(); ArrayList<CPPNeighborhood> iters4 = new
		 * ArrayList<CPPNeighborhood>(); iters4.add(new CPPOneSwitchSmallIter());
		 * iters4.add(new CPPOneSwitchMediumIter()); iters4.add(new
		 * CPPOneSwapSmallIter()); iters4.add(new CPPOneSwapIter());
		 * 
		 * GRASP_CPP_Scheme heur4 = new GRASP_CPP_Type4(dc, iters4); CPPSolution sol4 =
		 * heur4.grasp(10, my_seed, (float) 0.2);
		 * 
		 * Date d5 = new Date(); ArrayList<CPPNeighborhood> iters5 = new
		 * ArrayList<CPPNeighborhood>(); iters5.add(new CPPOneSwitchMediumIter());
		 * iters5.add(new CPPOneSwapIter()); GRASP_CPP_Scheme heur5 = new
		 * GRASP_CPP_Type4(dc, iters5); CPPSolution sol5 = heur5.grasp(10, my_seed,
		 * (float) 0.2);
		 * 
		 * Date d6 = new Date();
		 * 
		 * System.out.println("solution value: " + sol4.getValue() + " size =" +
		 * sol4.getTable().size() + " time =" + (d5.getTime() - d4.getTime()));
		 * System.out.println("solution value: " + sol5.getValue() + " size =" +
		 * sol5.getTable().size() + " time =" + (d6.getTime() - d5.getTime()));
		 */
		int tot = 0;
		for (Customer c : Customer.custList) {
			tot += c.getContainers().size();
		}
		System.out.println("|C_bar| = " + tot);
		System.out.println("cpu feasible "+countCPUFeas);
		System.out.println("ram feasible "+countRAMFeas);
		System.out.println("disk feasible "+countDiskFeas);
		System.out.println("s_u "+count_s_u);
	}
}
