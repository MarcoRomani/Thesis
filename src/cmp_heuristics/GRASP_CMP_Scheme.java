package cmp_heuristics;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.InfeasibilityException;
import cpp_heuristics.ServerStub;
import cpp_heuristics.SolutionWrapper;
import general.*;
import ltCMP.CMPMain;

public abstract class GRASP_CMP_Scheme {

	public static int SAMPLING = 10;
	public static int SAMPLING_GREEDY = 2;
	public static double DISCARD_FACTOR = 0.65;
	public static double DISCARD_FACTOR_2 = 0.9;
	public static double min_delta = 20;
	public static double MIGR_TIME = 240;
	public static int maxHops = 7;
	public static int k_paths = 3;
	public static double pow_coeff =1;
	public static double traff_coeff =50;
	public static double migr_coeff =1;
	protected static double inv_offset =CMPDataCenter.inv_offset;
	protected SecureRandom rng;
	protected CMPDataCenter dc;
	protected int neigh_index = 0;
	protected CMPNeighborhood neighborhood_explorer;
	protected List<CMPNeighborhood> neighborhoods = new ArrayList<CMPNeighborhood>();
	protected Input input;
	protected List<LinkStub> stubs_migr;
	protected List<ServerStub> stubs_after;
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph = new DefaultDirectedWeightedGraph<Node, LinkStub>(
			LinkStub.class);
	protected Map<Container, Boolean> inputTable = new HashMap<Container, Boolean>();
	protected Comparator<Container> comp;
	protected SolutionWrapper wrapper;
	protected CMPSolution best;
	
	
	protected abstract CMPSolution greedy_rand_constr(Input input, double alfa) throws InfeasibilityException;
	protected abstract double incrementalCost(Container c, ServerStub s, CMPSolution incumbent);
	public abstract void setNeighborhoods(List<CMPNeighborhood> neighs);
	protected abstract void changeNeighborhood();

	protected long timelimit=Long.MAX_VALUE;
	
	Date d1;	
	
	public CMPSolution grasp(String option,int param, double alfa, int seed) {
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		rng.setSeed((BigInteger.valueOf(seed).toByteArray()));

		

		switch(option) {
		case "time":
			return graspTime(param,alfa);
		case "maxIter":
			return graspIter(param,alfa);
		default: 
			return graspIter(param, alfa);
		}
	}
	
	public CMPSolution graspIter(int maxIter, double alfa) {

		best = new CMPSolution();

		 d1 = new Date();
		int i=0;
		for ( i = 0; i < maxIter; i++) {
			if(CMPMain.display) {
				   System.out.println("iter:"+(i));
			}

			grasp(alfa);
		}

		Date d2 = new Date();
		best.setValue(Double.POSITIVE_INFINITY);
		evaluate(best);
		wrapper.updateSolutions(best);
		wrapper.updateIterations(i);
		wrapper.updateTime(d2.getTime()-d1.getTime());
		synchronized (wrapper) {
			wrapper.notifyAll();
		}
		
		return best;
	}
	
	public CMPSolution graspTime(int time, double alfa) {
		int my_time = time*1000;
		timelimit = my_time;
		best = new CMPSolution();
		
		d1 = new Date();

		Date d2 = new Date();
		int iter =0;
		do {
			iter += 1;
			if(CMPMain.display) {
			   System.out.println("iter:"+(iter));
			}
			
			grasp(alfa);
		    d2 = new Date();
		    
		}while(d2.getTime()-d1.getTime() < my_time);
		
		best.setValue(Double.POSITIVE_INFINITY);
		evaluate(best);
		wrapper.updateSolutions(best);		
		wrapper.updateIterations(iter);
		wrapper.updateTime(d2.getTime()-d1.getTime());
		synchronized (wrapper) {
			wrapper.notifyAll();
		}
		
		return best;
	}
	
	protected void grasp(double alfa) {

		
			CMPSolution incumbent = new CMPSolution();

			try {
				incumbent = greedy_rand_constr(input, alfa);
			} catch (InfeasibilityException e) {
				if(CMPMain.display) {
				    System.out.println("infeasible");
				}
				reset((CMPSolution)e.getSolution());
				return;
			}

			evaluate(incumbent);
			
			if(incumbent.getValue() == Double.POSITIVE_INFINITY) {
				reset(incumbent);
				return;
			}
			wrapper.updateInit(incumbent);
			//System.out.println(incumbent);

			// -------- LOCAL SEARCH WITH MULTI-NEIGHBORHOODS --------------

			int count = 0;
			neigh_index = 0;
			neighborhood_explorer = neighborhoods.get(neigh_index);
			do {			
				CMPSolution newincumbent = localSearch(incumbent);
				if (!(newincumbent.getValue() < incumbent.getValue() - min_delta)) {
					count++;
				} else {
					count = 0;
				incumbent = newincumbent;
				}
				changeNeighborhood();
			} while (count < neighborhoods.size() && neighborhoods.size() > 1);

			// --------- UPDATE BEST SOLUTION AMONG ITERATIONS ------------
			if (incumbent.getValue() < best.getValue()) {
				best = (CMPSolution) incumbent.clone();
				
			}
			//System.out.println(incumbent.toString());
			//System.out.println(best.toString());
			// --------- PREPARE FOR NEXT ITERATION ----------------------
			reset(incumbent);

		}
	

	protected CMPSolution localSearch(CMPSolution init_sol) {
		
		if(new Date().getTime() - d1.getTime() > timelimit) return init_sol;
		CMPSolution sol = (CMPSolution) init_sol.clone();
		evaluate(sol);

		CMPSolution best_neighbor = sol;

		 if(CMPMain.display)System.out.println("start local search");

		do {
		//	 System.out.println("Try new neighborhood");
			
			sol = best_neighbor;
			
			
			
			neighborhood_explorer.setUp(dc, inputTable, stubs_after,graph, best_neighbor);

			while (neighborhood_explorer.hasNext()) {
				// System.out.println("next");
				CMPSolution current = null; 
				try {
					current = neighborhood_explorer.next();
				} catch (MyNoSuchElementException e) {
					current = new CMPSolution();
				}
				if (evaluate(current) < best_neighbor.getValue() - min_delta) {
					best_neighbor = current;
					wrapper.updateBests(best_neighbor);
		if(CMPMain.display)			 System.out.println("new best neighbor found "+best_neighbor.getValue());
				}

			}

		} while (sol.getValue() != best_neighbor.getValue() && new Date().getTime() - d1.getTime() < timelimit);

		neighborhood_explorer.clear();
		// System.out.println("end local search");
		
		// System.out.println(sol.toString());
		return sol;
	}

	protected double evaluate(CMPSolution sol) {
		
		if (sol.getValue() < Double.POSITIVE_INFINITY)
			return sol.getValue(); // lazy
		
		if(!checkFeasibility(sol)) {
		//	System.out.println("check feasib failed");
			sol.setValue(Double.POSITIVE_INFINITY);
			return Double.POSITIVE_INFINITY;
		}
		
		double t_value = 0;
		List<Customer> custs = Customer.custList;

		for (Customer c : custs) {
			List<Container> conts = c.getContainers();
			List<Container> migr_conts = c.getNewContainers();
			// nonmigr-migr and migr-nonmigr
			for (Container c1 : conts) {
				int s1 = dc.getPlacement().get(c1).getId();
				for (Container c2 : migr_conts) {
					int olds2 = dc.getPlacement().get(c2).getId();
					int s2 = sol.getTable().get(c2).intValue();
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						t_value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue() * dc.getCosts()[s1][s2];
						t_value -= c.getTraffic().get(new C_Couple(c1, c2)).doubleValue() * dc.getCosts()[s1][olds2];
					}
					if (c.getTraffic().get(new C_Couple(c2, c1)) != null) {
						t_value += c.getTraffic().get(new C_Couple(c2, c1)).doubleValue() * dc.getCosts()[s2][s1];
						t_value -= c.getTraffic().get(new C_Couple(c2, c1)).doubleValue() * dc.getCosts()[olds2][s1];
					}
				}
			}
			// migr-migr
			for (Container c1 : migr_conts) {
				int s1 = sol.getTable().get(c1).intValue();
				int olds1 = dc.getPlacement().get(c1).getId();
				for (Container c2 : migr_conts) {
					int olds2 = dc.getPlacement().get(c2).getId();
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						
						t_value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue()
								* dc.getCosts()[s1][sol.getTable().get(c2).intValue()];
						t_value -= c.getTraffic().get(new C_Couple(c1, c2)).doubleValue()
								* dc.getCosts()[olds1][olds2];
					}
				}
			}
		}
		
		List<Container> all_conts = new ArrayList<Container>();
		all_conts.addAll(input.getSinglesOBL());
		all_conts.addAll(input.getSinglesOPT());
		for(List<Container> ls : input.getClustersOBL()) {
			all_conts.addAll(ls);
		}
		for(List<Container> ls : input.getClustersOPT()) {
			all_conts.addAll(ls);
		}
		
		double p_value = 0;
		Set<Server> olds = new TreeSet<Server>();
		Set<Server> neWs = new TreeSet<Server>();

		for(Container vm : all_conts) {
			Server old = dc.getPlacement().get(vm);
			Server neW = stubs_after.get(sol.getTable().get(vm)).getRealServ();
			p_value += (CPUcalculator.fractionalUtilization(vm, neW))*(neW.getP_max() - neW.getP_idle());
			p_value -= (CPUcalculator.fractionalUtilization(vm, old))*(old.getP_max() - old.getP_idle());
			olds.add(old);
			neWs.add(neW);
		}
		
		for(Server s: olds) {
			if(!s.isStateON() && !neWs.contains(s)) {
				p_value -= s.getP_idle();
			}
		}
		
		for(Server s: neWs) {
			if(!olds.contains(s) && !s.isStateON()) {
				p_value += s.getP_idle();
			}
		}
		
		List<Container> optional = new ArrayList<Container>();
		optional.addAll(input.getSinglesOPT());
		for(List<Container> ls : input.getClustersOPT()) {
			optional.addAll(ls);
		}
		
		double migr_value =0;
		for(Container vm : optional) {
			if(dc.getPlacement().get(vm).getId() == sol.getTable().get(vm).intValue()) {
				migr_value += 1;
			}
			migr_value -= 1;
		}
		
		double value = p_value*pow_coeff + t_value * traff_coeff + migr_value*migr_coeff;
		//System.out.println(p_value+" + "+t_value+" + "+migr_value);
		sol.setValue(value);
		return value;
	}

	protected boolean checkFeasibility(CMPSolution sol) {
		List<Container> all_migrating = new ArrayList<Container>();
		all_migrating.addAll(input.getSinglesOBL());
		all_migrating.addAll(input.getSinglesOPT());
		for(List<Container> ls : input.getClustersOBL()) {
			all_migrating.addAll(ls);
		}
		for(List<Container> ls : input.getClustersOPT()) {
			all_migrating.addAll(ls);
		}
		
		if(all_migrating.size() != sol.getTable().keySet().size()) {
			if( CMPMain.display)System.out.println("MISSING SOMETHING \t"+all_migrating.size()+"\t"+sol.getTable().keySet().size());
			return false;
		}
		
		ArrayList<Container> nonMigr = new ArrayList<Container>();
		HashMap<Link,Double> tab =new HashMap<Link,Double>();
		for(Container v : all_migrating) {
			if(sol.getTable().get(v).intValue() == dc.getPlacement().get(v).getId() && inputTable.get(v)) {
				nonMigr.add(v);
			}
			for(LinkFlow lf: sol.getFlows().get(v)) {
				Link l = lf.getLink();
				Double d = tab.get(l);
				if(d == null) {
					tab.put(l, new Double(lf.getFlow()));
				}else {
					tab.remove(l);
					tab.put(l, new Double(lf.getFlow()+d.doubleValue()));
				}
			}
		}
		for(Container v1 : nonMigr) {
			Customer cust = Customer.custList.get(v1.getMy_customer());
			for(Container v2 : cust.getNewContainers()) {
				if(!(sol.getTable().get(v2).intValue() == dc.getPlacement().get(v2).getId() && inputTable.get(v2))) {
					continue;
				}
				Double t12 = cust.getTraffic().get(new C_Couple(v1,v2));
				Double t21 = cust.getTraffic().get(new C_Couple(v2,v1));
				if(t12 == null) continue;
				
		//		List<Link> p12 = dc.getPaths().get(new S_Couple(dc.getPlacement().get(v1),dc.getPlacement().get(v2)));
				List<Link> p12 = dc.getPath(dc.getPlacement().get(v1),dc.getPlacement().get(v2));
		//		List<Link> p21 = dc.getPaths().get(new S_Couple(dc.getPlacement().get(v2),dc.getPlacement().get(v1)));
				List<Link> p21 = dc.getPath(dc.getPlacement().get(v2),dc.getPlacement().get(v1));
				
				for(Link l : p12) {
					Double d = tab.get(l);
					if(d == null) {
						tab.put(l, new Double(t12.doubleValue()));
					}else {
						tab.remove(l);
						tab.put(l, new Double(t12.doubleValue()+d.doubleValue()));
					}
				}
				for(Link l : p21) {
					Double d = tab.get(l);
					if(d == null) {
						tab.put(l, new Double(t21.doubleValue()));
					}else {
						tab.remove(l);
						tab.put(l, new Double(t21.doubleValue()+d.doubleValue()));
					}
				}
			}
		}
		
		for(Link l : tab.keySet()) {
			if(l.getResidCapacity() < tab.get(l).doubleValue()) {
			//	System.out.println(l.getResidCapacity()+" \t"+tab.get(l).doubleValue());
				return false;
			}
		}
		return true;
		
	}

	protected abstract void reset(CMPSolution sol);

		
	
	
	public void setComparator(Comparator<Container> comparator) {
		comp = comparator;
	}

	public void setWrapper(SolutionWrapper w) {
		wrapper = w;
	}

	public SolutionWrapper getWrapper() {
		return wrapper;
	}
}
