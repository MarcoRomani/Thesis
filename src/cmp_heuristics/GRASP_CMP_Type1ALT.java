package cmp_heuristics;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import cpp_heuristics.InfeasibilityException;
import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Customer;
import general.Pod;
import general.Rack;
import general.Server;
import ltCMP.CMPMain;

public class GRASP_CMP_Type1ALT extends GRASP_CMP_Type1 {
	HashMap<Customer, Set<Pod>> podtable = new HashMap<Customer, Set<Pod>>();
	HashMap<Customer, Set<Pod>> tmppodtable = new HashMap<Customer, Set<Pod>>();

	public GRASP_CMP_Type1ALT(CMPDataCenter dc, Input input) {
		super(dc, input);
		for (Customer cm : Customer.custList) {
			Set<Pod> pds = new TreeSet<Pod>();
			for (Container c : cm.getContainers()) {
				for (Pod p : dc.getPods())
					if (p.containsServer(dc.getPlacement().get(c).getId())) {
						pds.add(p);
					}
			}
			podtable.put(cm, pds);
		}
	}

	@Override
	protected CMPSolution single_rand_constr(CMPSolution sol, List<Container> toPlace, double alfa)
			throws InfeasibilityException {
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();

		while (!toPlace.isEmpty()) {

			// System.out.println(toPlace.size());
			costs.clear();
			ids.clear();
			RCL.clear();
			Container m = toPlace.remove(0);
			Set<Pod> set1 = podtable.get(Customer.custList.get(m.getMy_customer()));
			Set<Pod> set2 = tmppodtable.get(Customer.custList.get(m.getMy_customer()));

			if ((set1 == null || set1.isEmpty()) && (set2 == null || set2.isEmpty())) {
				// System.out.println("chiamo normale");
				List<Container> singleton = new ArrayList<Container>();
				singleton.add(m);
				Integer s = super.single_rand_constr(sol, singleton, alfa).getTable().get(m);
				if (s == null)
					throw new InfeasibilityException(sol);
				for (Pod p : dc.getPods()) {
					if (p.containsServer(s.intValue())) {
						Set<Pod> pd = new TreeSet<Pod>();
						pd.add(p);
						tmppodtable.put(Customer.custList.get(m.getMy_customer()), pd);
						break;
					}
				}

				continue;
			}

			// System.out.println("chiamo ALT");
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;

			Set<Pod> my_pods = new TreeSet<Pod>();
			if (!(set1 == null))
				my_pods.addAll(set1);
			if (!(set2 == null))
				my_pods.addAll(set2);

			for (Pod p : my_pods) {
				for (Rack r : p.getRacks()) {
					for (Server s : r.getHosts()) {
						if(rng.nextInt(r.getHosts().size()) > SAMPLING_GREEDY) continue;
						double tmp = incrementalCost(m, stubs_after.get(s.getId()), sol);
						costs.add(new Double(tmp));
						ids.add(new Integer(s.getId()));
						if (tmp < min)
							min = tmp;
						if (tmp > max && tmp < Double.POSITIVE_INFINITY)
							max = tmp;
					}
				}
			}
			if (min == Double.POSITIVE_INFINITY)
				throw new InfeasibilityException(sol);

			for (int i = 0; i < costs.size(); i++) {
				if (costs.get(i).doubleValue() <= min + alfa * (max - min)) {
					RCL.add(stubs_after.get(ids.get(i).intValue()));
				}
			}

	//		 System.out.println(RCL.size());
			boolean found = false;
			ArrayList<Container> tmp = new ArrayList<Container>();
			tmp.add(m);

			while (!RCL.isEmpty() && !found) {
				// System.out.println(RCL.size());
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				Response r = null;
				if (e.getRealServ().getId() != dc.getPlacement().get(m).getId()) {
					r = canMigrate(tmp, dc.getPlacement().get(m), e.getRealServ());
					found = r.getAnswer();
					if (found) {
						e.forceAllocation(m, stubs_after, sol, dc);
						sol.getTable().put(m, new Integer(e.getId()));
						updateLinks(r.getFlow(), true);
						sol.getFlows().put(m, r.getFlow());
					}

				} else {
					r = nonMigrate(m, e, sol);
					found = r.getAnswer();
					if (found) {
						e.forceAllocation(m, stubs_after, sol, dc);
						sol.getTable().put(m, new Integer(e.getId()));
						sol.getFlows().put(m, new ArrayList<LinkFlow>());
						updateLinks(r.getFlow(), true);

					}
				}

			}

			if (!found) {
				// System.out.println("chiamo normale");
				List<Container> singleton = new ArrayList<Container>();
				singleton.add(m);
				Integer s = super.single_rand_constr(sol, singleton, alfa).getTable().get(m);
				if (s == null)
					throw new InfeasibilityException(sol);
				for (Pod p : dc.getPods()) {
					if (p.containsServer(s.intValue())) {
						Set<Pod> pd = new TreeSet<Pod>();
						pd.add(p);
						tmppodtable.put(Customer.custList.get(m.getMy_customer()), pd);
						break;
					}
				}
			}

		}

		return sol;
	}

	@Override
	protected CMPSolution cluster_rand_constr(CMPSolution incumbent, List<Container> cluster, double alfa,
			List<Container> rest) {
		CMPSolution sol = super.cluster_rand_constr(incumbent, cluster, alfa, rest);
		for (Container c : cluster) {
			Integer s = sol.getTable().get(c);
			if (s == null)
				continue;

			for (Pod p : dc.getPods()) {
				if (p.containsServer(s.intValue())) {
					if (!podtable.get(Customer.custList.get(c.getMy_customer())).contains(p)) {
						Set<Pod> set = tmppodtable.get(Customer.custList.get(c.getMy_customer()));
						if (set != null) {
							set.add(p);
						} else {
							set = new TreeSet<Pod>();
							set.add(p);
							tmppodtable.put(Customer.custList.get(c.getMy_customer()), set);
						}
					}
					break;
				}
			}
		}

		return sol;

	}

	@Override
	protected void reset(CMPSolution sol) {
		super.reset(sol);
		tmppodtable.clear();
	}

	@Override
	protected CMPSolution localSearch(CMPSolution init_sol) {
		
		
		if (CMPMain.display)
			System.out.println("start local search");
		if(new Date().getTime() - d1.getTime() > timelimit) return init_sol;
		
		CMPSolution sol = (CMPSolution) init_sol.clone();
		evaluate(sol);

		double v = wrapper.getBest().getValue();
		if (v < sol.getValue() && Math.abs(sol.getValue()) < Math.abs(v) * DISCARD_FACTOR && v != Double.POSITIVE_INFINITY) {
			return sol;
		}
		
		CMPSolution best_neighbor = sol;

		
		boolean abruptstop = false;

		do {
			abruptstop = false;
			// System.out.println("Try new neighborhood");
			sol = best_neighbor;
			
			neighborhood_explorer.setUp(dc, inputTable, stubs_after, graph, best_neighbor);

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
					if (CMPMain.display) {
				//		System.out.println("new best neighbor found " + best_neighbor.getValue());
						}
						
				}

			}

			 v = wrapper.getBest().getValue();
			if (v < best_neighbor.getValue() && Math.abs(best_neighbor.getValue()) < Math.abs(v) * DISCARD_FACTOR && v != Double.POSITIVE_INFINITY) {
				abruptstop = true;
			}
		} while (sol.getValue() != best_neighbor.getValue() && !(abruptstop) &&   ((new Date().getTime() - d1.getTime()) < timelimit));

		neighborhood_explorer.clear();
		// System.out.println("end local search");
	
		// System.out.println(sol.toString());
		return sol;
	}

	@Override
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
		double v = wrapper.getBestInit().getValue();
		if (Math.abs(incumbent.getValue()) < Math.abs(v) * DISCARD_FACTOR_2 && v != Double.POSITIVE_INFINITY) {
			reset(incumbent);
			return ;
		}
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
}
