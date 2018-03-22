package FirstFit;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import cpp_heuristics.CPPSolution;
import cpp_heuristics.SolutionWrapper;
import general.Business;
import general.Catalog;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;
import stCPP.DC_filler;
import stCPP.FirstFit;
import stCPP.PopulateException;
import stCPP.RackFiller;
import writeFiles.CPPtoAMPL;

public class FirstFitMain {

	
	    public static boolean display = false;
		
		public static int max_requests = 3600;
		public static int min_requests = 400;
		
		public static double filler_thresh = 0.9;
	
		
		
		
		public static void main(String[] args) {

			System.out.println("-- START --");
			int iter =1;
			int my_seed = 9026;
			int n_newcust = 7;
			int n_cust = 20000;
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
			
			if (args.length >= 6 && "display".equals(args[5])) {
				int disp = Integer.parseInt(args[6]);
				if (disp == 0) {
					display = false;
				} else {
					display = true;
				}
			}
			
	//		readConfig();

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

				ArrayList<Container> toPlace = new ArrayList<Container>();
				for(Customer cust : Customer.custList) {
					toPlace.addAll(cust.getNewContainers());
				}
				Date d1 = new Date();
				SolutionWrapper wr = new SolutionWrapper();
				FirstFitHeur alg = new FirstFitHeur(dc);
				CPPSolution sol = alg.findSolution(toPlace);
				
				wr.updateInit(sol);
				wr.updateBests(sol);
				wr.updateSolutions(sol);
				Date d2 = new Date();
				wr.updateTime(d2.getTime()-d1.getTime());
				int viol = alg.getViolations();
				wr.updateIterations(viol);
				
				CPPtoAMPL writer = new CPPtoAMPL();
			//	 writer.writeResults(my_seed, n_pods, n_newcont, n_newcust, n_cust,	wr,"FF_results");
				System.out.println("SOLUTION: \t" + wr.getBest().getValue());
				System.out.println("VIOLATIONS: \t" + viol);

				
				

	}
		
		private static void readConfig() {
			Scanner sc = null;
			try {
				 sc = new Scanner(new File("FFconfig.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			ArrayList<String> lines = new ArrayList<String>();
			while(sc.hasNext()) {
				lines.add(sc.next());
			}
			
			// FILLER TRESHOLD
			filler_thresh = Double.parseDouble(findValue(lines,"filler_threshold"));
		
			max_requests =Integer.parseInt(findValue(lines,"max_requets"));
			min_requests = Integer.parseInt(findValue(lines,"min_requests"));
			
		
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
