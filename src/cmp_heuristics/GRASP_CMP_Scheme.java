package cmp_heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.CPPNeighborhood;
import cpp_heuristics.CPPSolution;
import cpp_heuristics.ServerStub;
import general.*;

public abstract class GRASP_CMP_Scheme {

	public static double min_delta = 0.0000000001;
	public static double MIGR_TIME = 25;
	public static int maxHops = 10;
	public static int k_paths = 3;
	public static double pow_coeff =1;
	public static double traff_coeff =1;
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
	protected abstract CMPSolution greedy_rand_constr(Input input, double alfa);

	protected abstract double incrementalCost(Container c, ServerStub s, CMPSolution incumbent);
	public abstract void setNeighborhoods(List<CMPNeighborhood> neighs);
	protected abstract void changeNeighborhood();

	public CMPSolution grasp(int maxIter, int seed, double alfa) {

		rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CMPSolution best = new CMPSolution();

		for (int iter = 0; iter < maxIter; iter++) {

			System.out.println("\n iter:" + iter);
			CMPSolution incumbent = new CMPSolution();

			incumbent = greedy_rand_constr(input, alfa);

			evaluate(incumbent);

			// -------- LOCAL SEARCH WITH MULTI-NEIGHBORHOODS --------------

			int count = 0;
			neigh_index = 0;
			neighborhood_explorer = neighborhoods.get(neigh_index);
			do {			
				CMPSolution newincumbent = localSearch(incumbent);
				if (!(newincumbent.getValue() < incumbent.getValue() - min_delta)) {
					count++;
				} else
					count = 0;
				incumbent = newincumbent;
				changeNeighborhood();
			} while (count < neighborhoods.size() && neighborhoods.size() > 1);

			// --------- UPDATE BEST SOLUTION AMONG ITERATIONS ------------
			if (incumbent.getValue() < best.getValue()) {
				best = (CMPSolution) incumbent.clone();
			}
			System.out.println(incumbent.toString());
			System.out.println(best.toString());
			// --------- PREPARE FOR NEXT ITERATION ----------------------
			reset(incumbent);

		}
		return best;
	}

	protected CMPSolution localSearch(CMPSolution init_sol) {
		CMPSolution sol = (CMPSolution) init_sol.clone();
		evaluate(sol);

		CMPSolution best_neighbor = sol;

		// System.out.println("start local search");

		do {
			// System.out.println("Try new neighborhood");
			sol = best_neighbor;
			neighborhood_explorer.setUp(dc, inputTable, stubs_after,graph, best_neighbor);

			while (neighborhood_explorer.hasNext()) {
				// System.out.println("next");
				CMPSolution current = neighborhood_explorer.next();
				if (evaluate(current) < best_neighbor.getValue() - min_delta) {
					best_neighbor = current;
		//			 System.out.println("new best neighbor found "+best_neighbor.getValue());
				}

			}

		} while (sol.getValue() != best_neighbor.getValue());

		neighborhood_explorer.clear();
		// System.out.println("end local search");
		sol = best_neighbor;
		// System.out.println(sol.toString());
		return sol;
	}

	protected double evaluate(CMPSolution sol) {
		
		if(!checkFeasibility(sol)) {
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
					int s2 = sol.getTable().get(c2).intValue();
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						t_value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue() * dc.getCosts()[s1][s2];
					}
					if (c.getTraffic().get(new C_Couple(c2, c1)) != null) {
						t_value += c.getTraffic().get(new C_Couple(c2, c1)).doubleValue() * dc.getCosts()[s2][s1];
					}
				}
			}
			// migr-migr
			for (Container c1 : migr_conts) {
				int s1 = sol.getTable().get(c1).intValue();
				for (Container c2 : migr_conts) {
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						
						t_value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue()
								* dc.getCosts()[s1][sol.getTable().get(c2).intValue()];
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
		}
		
		double value = p_value*pow_coeff + t_value * traff_coeff + migr_value*migr_coeff;
		System.out.println(p_value+" + "+t_value+" + "+migr_value);
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
			System.out.println("MISSING SOMETHING \t"+all_migrating.size()+"\t"+sol.getTable().keySet().size());
			return false;
		}
		return true;
		
	}

	protected void reset(CMPSolution sol) {

		ArrayList<Container> toRemove = new ArrayList<Container>();
		toRemove.addAll(sol.getTable().keySet());
		for (Container c : toRemove) {
			ServerStub tmp = stubs_after.get(sol.getTable().get(c).intValue());
			tmp.remove(c, stubs_after, sol, dc);
			sol.getTable().remove(c);

			////
			for (LinkFlow lf : sol.getFlows().get(c)) {
				LinkStub l = lf.getLink();
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
		}
	}
}
