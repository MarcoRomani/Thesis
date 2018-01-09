package stCPP;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import cpp_heuristics.*;
import general.*;
import writeFiles.CPPtoAMPL;
import java.util.Date;

public class Main {

	public static void main(String[] args) {

		int iter = 1;
		int my_seed = 264;
		int n_newcust = 4;
		int n_cust = 10000;
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
			// customers.add(new
			// Customer(((rng.nextDouble()/500)+(double)0.001),Business.values()[rng.nextInt(2)],rng));
			customers.add(new Customer(((double) (rng.nextInt(3500) + 400) / 1000000) * 8,
					Business.values()[rng.nextInt(2)], rng));
		}

		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for (int i = 0; i < n_newcust; i++) {
			// new_customers.add(new
			// Customer(((rng.nextDouble()/500)+(double)0.001),Business.values()[rng.nextInt(2)],rng));
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

		// passare roba al filler
		DC_filler filler = new FirstFit();
		filler = new RackFiller(rng);
		filler.populate(dc, customers, (float) 0.7);

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					System.out.println(s.toString());
				}
			}
		}

		CPPtoAMPL writer = new CPPtoAMPL();
		// writer.writeDAT(dc, customers, new_customers, my_seed);

		Date d4 = new Date();
		ArrayList<CPPNeighborhood> iters4 = new ArrayList<CPPNeighborhood>();
		iters4.add(new CPPOneSwitchSmallIter());
		iters4.add(new CPPOneSwitchMediumIter());
		iters4.add(new CPPOneSwapSmallIter());
		iters4.add(new CPPOneSwapIter());

		GRASP_CPP_Scheme heur4 = new GRASP_CPP_Type4(dc, iters4);
		CPPSolution sol4 = heur4.grasp(10, my_seed, (float) 0.2);

		Date d5 = new Date();
		ArrayList<CPPNeighborhood> iters5 = new ArrayList<CPPNeighborhood>();
		iters5.add(new CPPOneSwitchMediumIter());
		iters5.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur5 = new GRASP_CPP_Type4(dc, iters5);
		CPPSolution sol5 = heur5.grasp(10, my_seed, (float) 0.2);

		Date d6 = new Date();

		System.out.println("solution value: " + sol4.getValue() + " size =" + sol4.getTable().size() + " time ="
				+ (d5.getTime() - d4.getTime()));
		System.out.println("solution value: " + sol5.getValue() + " size =" + sol5.getTable().size() + " time ="
				+ (d6.getTime() - d5.getTime()));

		int tot = 0;
		for (Customer c : Customer.custList) {
			tot += c.getContainers().size();
		}
		System.out.println(tot);
	}
}
