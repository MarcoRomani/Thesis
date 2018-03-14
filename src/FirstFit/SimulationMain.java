package FirstFit;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;

import cmp_heuristics.CMPSolution;
import cpp_heuristics.CPPSolution;
import general.Business;
import general.CMPDataCenter;
import general.Catalog;
import general.Container;
import general.Customer;
import general.DataCenter;

public class SimulationMain {

	static SecureRandom rng;
	static int max_requests = 3600;
	static int min_requests = 400;

	public static void main(String[] args) {

		int my_seed = 0;
		int pod = 6;
		int batch = 100;
		int total = 1000;
		String algorithm = "grasp";

		boolean migration = true;
		int migr_window = 3600;

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
			CPPSolution sol = solveProblem(algorithm);
			postprocessing(dc,sol);
			if ((new Date().getTime() - d2.getTime()) / 1000 > migr_window && migration) {
				CMPSolution solcmp = solveCMP();
				postprocessing(solcmp);
			}

		}
	}

	private static int generateRequests(int batch) {

		int count = 0;
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

	private static CMPSolution solveCMP() {
		// TODO Auto-generated method stub
		return null;
	}

	private static void postprocessing(DataCenter dc, CPPSolution solcpp) {
		
		for(Customer cm: Customer.custList) {
			cm.transformIntoOld();
		}
		
		for(Container vm: solcpp.getTable().keySet()) {
			
		}

	}

	private static void postprocessing(CMPSolution solcmp) {
		// TODO Auto-generated method stub

	}

	private static CPPSolution solveProblem(String algorithm) {
		// TODO Auto-generated method stub
		return null;
	}

}
