package ltCMP;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cmp_heuristics.*;
import cmp_heuristics.GRASP_CMP_Type1;
import cmp_heuristics.Input;

import cpp_heuristics.CPPSolution;

import cpp_heuristics.ContainerBDWComparator;
import cpp_heuristics.ContainerCPUComparator;
import cpp_heuristics.ContainerDISKComparator;

import cpp_heuristics.SolutionWrapper;
import general.*;

import general.Server;
import writeFiles.CMPtoAMPL;


public class CMPMain {
	public static boolean display = false;
	public static String option = "time";
	public static int iter_param = 10;
	public static double time_minutes = 0.5;
	public static double alfa_grasp = 0.3;
	public static double filler_thresh = 0.99;
	
	
	public static void main(String[] args) {
	

		int iter = 1;
		int my_seed = 50;
		int n_cust = 40;
		int n_pods =6;
		
		
		
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
			CMPDataCenter dc = new CMPDataCenter("FatTree", n_pods);
			List<Customer> customers = new ArrayList<Customer>();

			for (int i = 0; i < n_cust; i++) {

				customers.add(new Customer((double) (rng.nextInt(3600) + 400), Business.values()[rng.nextInt(2)], rng));
			}

			System.out.println("-- GENERATE INITIAL PLACEMENT --");
			// FILL THE DATACENTER
			CMPtoAMPL writer = new CMPtoAMPL();
			CMPFiller filler = new DefaultCMPFiller(rng);
			
			filler.populate(dc, customers, (float) filler_thresh);
			System.out.println("PATHS "+dc.getPaths().size());
		//	writer.writeCMPdat_phase1(dc, Customer.custList, my_seed);
			
			Input input = preprocess(dc);
		//	writer.writeCMPdat_phase2(dc, Customer.custList, my_seed, input);
			int count_obl =0;			
			for(List<Container> ls : input.getClustersOBL()) {
				count_obl += ls.size();
			}
			int count_opt =0;			
			for(List<Container> ls : input.getClustersOPT()) {
				count_opt += ls.size();
			}
			System.out.println("OBL: \t"+count_obl+"\t"+input.getSinglesOBL().size());
			System.out.println("OPT: \t"+count_opt+"\t"+input.getSinglesOPT().size());
			
			// --------- HEURISTICS ----------

			int grasp_iter = iter_param;
			int grasp_seed = my_seed;
			double grasp_alfa =  alfa_grasp;
			int grasp_time =Math.max(1, (int) (time_minutes * 60));
			
			// ---- CREATE ALGORITHMS ---------

			ArrayList<GRASP_CMP_Scheme> algs_v0 = new ArrayList<GRASP_CMP_Scheme>();
			ArrayList<GRASP_CMP_Scheme> algs_v1 = new ArrayList<GRASP_CMP_Scheme>();
			ArrayList<GRASP_CMP_Scheme> algs_v2 = new ArrayList<GRASP_CMP_Scheme>();
			ArrayList<GRASP_CMP_Scheme> algs_v3 = new ArrayList<GRASP_CMP_Scheme>();
			
			algs_v0.add(new GRASP_CMP_Type1(dc,input));
			algs_v0.add(new GRASP_CMP_Type1b(dc,input));
			algs_v0.add(new GRASP_CMP_Type2(dc,input));
			algs_v0.add(new GRASP_CMP_Type2b(dc,input));
		
			algs_v1.add(new GRASP_CMP_Type1(dc,input));
			algs_v1.add(new GRASP_CMP_Type1b(dc,input));
			algs_v1.add(new GRASP_CMP_Type2(dc,input));
			algs_v1.add(new GRASP_CMP_Type2b(dc,input));
			for (GRASP_CMP_Scheme gs : algs_v1) {
				gs.setComparator(new ContainerBDWComparator());
			}
			algs_v2.add(new GRASP_CMP_Type1(dc,input));
			algs_v2.add(new GRASP_CMP_Type1b(dc,input));
			algs_v2.add(new GRASP_CMP_Type2(dc,input));
			algs_v2.add(new GRASP_CMP_Type2b(dc,input));
			for (GRASP_CMP_Scheme gs : algs_v2) {
				gs.setComparator(new ContainerCPUComparator());
			}
			algs_v3.add(new GRASP_CMP_Type1(dc,input));
			algs_v3.add(new GRASP_CMP_Type1b(dc,input));
			algs_v3.add(new GRASP_CMP_Type2(dc,input));
			algs_v3.add(new GRASP_CMP_Type2b(dc,input));
			for (GRASP_CMP_Scheme gs : algs_v3) {
				gs.setComparator(new ContainerDISKComparator());
			}
			
			// ---- SET NEIGHBORHOODS, WRAPPER and THREADS--------
			SolutionWrapper wrapper = new SolutionWrapper();
			ArrayList<CMPThread> threads = new ArrayList<CMPThread>();

			ArrayList<GRASP_CMP_Scheme> algs_all = new ArrayList<GRASP_CMP_Scheme>();
			algs_all.addAll(algs_v0);
			algs_all.addAll(algs_v1);
			algs_all.addAll(algs_v2);
			algs_all.addAll(algs_v3);
			
			for (GRASP_CMP_Scheme gs : algs_all) {
				 List<CMPNeighborhood> neighs = new ArrayList<CMPNeighborhood>();
				    neighs.add(new CMPOneSwitchSmallIter());
				    neighs.add(new CMPOneSwitchMediumIter());
				    neighs.add(new CMPOneSwapSmallIter());
			//	    neighs.add(new CMPOneSwapIter());
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
			
		

	}
			
			
	
	
	
	private static Input preprocess(CMPDataCenter dc) {
		
		GreedyInverseKnapsack knaps = new GKS2();
		List<List<Container>> clusters_obl = new ArrayList<List<Container>>();
		List<List<Container>> clusters_opt = new ArrayList<List<Container>>();
		List<Container> singles_obl = new ArrayList<Container>();
		List<Container> singles_opt = new ArrayList<Container>();
		
		for(Pod p: dc.getPods()) {
			for(Rack r:p.getRacks()) {
				for(Server s: r.getHosts()) {
					
					knaps.setServer(s);
					knaps.process();
					List<Container> my_obl = knaps.getMandatory();
					List<Container> my_opt = knaps.getOptional();
				
					for(Container v: my_obl) {
						s.deallocateContainer(v);							
						Customer cust = Customer.custList.get(v.getMy_customer());
						cust.getContainers().remove(v);
						cust.getNewContainers().add(v);
						
						Double to_t0 = cust.getTraffic().get(new C_Couple(v,Container.c_0));
						Double from_s0 =cust.getTraffic().get(new C_Couple(Container.c_0, v));
						
						if(to_t0 != null) {
							List<Link> path_0 = dc.getTo_wan().get(s);
							for(Link l : path_0) {
								l.setResCapacity(l.getResCapacity() + to_t0.doubleValue());
							}
						}
						if(from_s0 != null) {
							List<Link> path_0 = dc.getFrom_wan().get(s);
							for(Link l : path_0) {
								l.setResCapacity(l.getResCapacity() + from_s0.doubleValue());
							}
						}
						
						for(Container v2: cust.getContainers()) {
							Double t1 = cust.getTraffic().get(new C_Couple(v,v2));
							Double t2 = cust.getTraffic().get(new C_Couple(v2,v));
							if(t1 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if(s2 == s) continue;
								List<Link> p1 = dc.getPaths().get(new S_Couple(s,s2));							
								for(Link l:p1) {
									l.setResCapacity(l.getResCapacity() + t1.doubleValue());
								}
							}
							if(t2 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if(s2 == s) continue;
								List<Link> p2 = dc.getPaths().get(new S_Couple(s2,s));
								for(Link l:p2) {
									l.setResCapacity(l.getResCapacity() + t2.doubleValue());
								}
							}
						}
						
					}
					for(Container v: my_opt) {
						s.deallocateContainer(v);							
						Customer cust = Customer.custList.get(v.getMy_customer());
						cust.getContainers().remove(v);
						cust.getNewContainers().add(v);
						for(Container v2: cust.getContainers()) {
							Double t1 = cust.getTraffic().get(new C_Couple(v,v2));
							Double t2 = cust.getTraffic().get(new C_Couple(v2,v));
							if(t1 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if(s2 == s) continue;
								List<Link> p1 = dc.getPaths().get(new S_Couple(s,s2));
								for(Link l:p1) {
									l.setResCapacity(l.getResCapacity() + t1.doubleValue());
								}
							}
							if(t2 != null) {
								Server s2 = dc.getPlacement().get(v2);
								if(s2 == s) continue;
								List<Link> p2 = dc.getPaths().get(new S_Couple(s2,s));
								for(Link l:p2) {
									l.setResCapacity(l.getResCapacity() + t2.doubleValue());
								}
							}
						}
					
					
					
					
				}
					

				
					
					clustering(my_obl, singles_obl, clusters_obl);
					clustering(my_opt, singles_opt, clusters_opt);
					
			}
		}
	}
		
		for(Pod p: dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s : r.getHosts()) {
					s.updateBandwidth();
				}
			}
		}
	
		return new Input(clusters_obl, singles_obl, clusters_opt, singles_opt);
		
		
		
  }
	
	private static void clustering(List<Container> input, List<Container> singles, List<List<Container>> clusters) {
		
		while(!input.isEmpty()) {
			Container repr = input.remove(0);
			List<Container> my_cluster = new ArrayList<Container>();
			for(Container v: input) {
				if(repr.getMy_customer() == v.getMy_customer()) {
					my_cluster.add(v);					
				}
			}
			if(!my_cluster.isEmpty()) {
				input.removeAll(my_cluster);
				my_cluster.add(repr);
				clusters.add(my_cluster);
			}else {
				singles.add(repr);
			}
		}
	}
}
