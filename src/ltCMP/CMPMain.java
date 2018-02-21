package ltCMP;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import cmp_heuristics.*;
import cmp_heuristics.GRASP_CMP_Type1;
import cmp_heuristics.Input;
import general.*;

import general.Server;


public class CMPMain {

	public static double filler_thresh = 0.99;
	public static void main(String[] args) {
	

		int iter = 1;
		int my_seed = 5;
		int n_cust = 150;
		int n_pods =8;
		
		
		
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
			CMPFiller filler = new DefaultCMPFiller(rng);
			
			filler.populate(dc, customers, (float) filler_thresh);
			
			System.out.println("PATHS "+dc.getPaths().size());
			Input input = preprocess(dc);
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
			
		    GRASP_CMP_Scheme heur = new GRASP_CMP_Type1(dc,input);
		    CMPSolution sol = heur.grasp(10, my_seed, 0.15);
			
		    System.out.println(sol.toString());
			

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
						cust.getMigrating().add(v);
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
						cust.getMigrating().add(v);
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
