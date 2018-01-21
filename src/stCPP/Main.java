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

		int iter = 25;
		int my_seed =26;
		int n_newcust = 1;
		int n_cust = 5;
		int n_newcont = 20;
		int n_pods = 4;

		for (int i = my_seed; i < my_seed + iter; i++) {
			System.out.print("seed="+i);
			doStuff(i, n_pods, n_cust, n_newcust, n_newcont, "FatTree");
		}
		System.out.print("End batch");
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
		filler.populate(dc, customers, (float) 0.7);


		int count_s_u = 0;


	
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					System.out.println(s.toString());
				
					if (s.isUnderUtilized())
						count_s_u++;
				}
			}
		}

		CPPtoAMPL writer = new CPPtoAMPL();
		//writer.writeCPPdat(dc, customers, new_customers, my_seed);

		// --------- HEURISTICS ----------

		int grasp_iter = 10;
		int grasp_seed = my_seed;
		float grasp_alfa = (float) 0.15;
		int grasp_time = 3*60;

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
		//	gs.setIndexing(tree);
			gs.setWrapper(wrapper);
			threads.add(new CPPThread("time",grasp_time, grasp_seed, grasp_alfa, gs));
		}

		// -------- EXECUTE IN PARALLEL ----------
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

		// ------- DISPLAY RESULTS ----------
		for (CPPSolution s : wrapper.getSolutions()) {
			System.out.println(s.getValue());
		}
		System.out.println(wrapper.getBest().toString());
		System.out.println("time = " + (d2.getTime() - d1.getTime()));

		int tot = 0;
		for (Customer c : Customer.custList) {
			tot += c.getContainers().size();
		}
		System.out.println("|C_bar| = " + tot);

		System.out.println("s_u " + count_s_u);
		
		writer.writeResults(my_seed, n_pods, n_newcont, n_newcust, n_cust, wrapper.getBest().getValue(),d2.getTime()-d1.getTime());
	}
}
