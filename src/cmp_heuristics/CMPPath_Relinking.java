package cmp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.CPPNeighborhood;
import cpp_heuristics.CPPSolution;
import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;
import general.Link;
import general.Node;
import general.Pod;
import general.Rack;
import general.Server;
import stCPP.Main;

public class CMPPath_Relinking {
	public static double pow_coeff = GRASP_CMP_Scheme.pow_coeff;
	public static double traff_coeff = GRASP_CMP_Scheme.traff_coeff;
	public static double migr_coeff = GRASP_CMP_Scheme.migr_coeff;
	public static double min_delta = 0.000000001;
	protected double alfa; // randomization param
	protected int iterations;
	protected SecureRandom rng = new SecureRandom();
	protected int n_moves; // size of a move
	protected List<ServerStub> stubs_after = new ArrayList<ServerStub>();
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph = new DefaultDirectedWeightedGraph<Node, LinkStub>(
			LinkStub.class);
	protected CMPDataCenter dc;
	protected double beta; // truncation parameter
	protected int neigh_index = 0;

	protected CMPNeighborhood neighborhood_explorer;
	protected List<CMPNeighborhood> neighborhoods = new ArrayList<CMPNeighborhood>();
	protected Input input;
	protected Map<Container, Boolean> inputTable = new HashMap<Container, Boolean>();
	
	public CMPPath_Relinking(CMPDataCenter dc, double alfa, int iter, double beta, int n_moves, Input input) {
		this.dc = dc;
		this.alfa = alfa;
		this.beta = beta;
		this.iterations = iter;
		this.n_moves = n_moves;

		this.input = input;
		for (Container v : input.getSinglesOBL()) {
			inputTable.put(v, new Boolean(false));
		}

		for (Container v : input.getSinglesOPT()) {
			inputTable.put(v, new Boolean(true));
		}

		for (List<Container> ls : input.getClustersOBL()) {
			for (Container v : ls) {
				inputTable.put(v, new Boolean(false));
			}
		}

		for (List<Container> ls : input.getClustersOPT()) {
			for (Container v : ls) {
				inputTable.put(v, new Boolean(true));
			}
		}
		
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					stubs_after.add(new ServerStub(s));
				}
			}
		}

		buildGraph();
	}

	public CMPPath_Relinking(CMPDataCenter dc, double alfa, int iter, double beta, int n_moves, Input input, SecureRandom rng) {
		this(dc, alfa, iter, beta, n_moves,input);
		this.rng = rng;
	}

	protected void buildGraph() {
		Set<Link> links = dc.getNetwork().edgeSet();
		Set<Node> nodes = dc.getNetwork().vertexSet();

		for (Node n : nodes) {
			graph.addVertex(n);
		}

		for (Link l : links) {
			LinkStub st = new LinkStub(l);
			graph.addEdge(l.getMySource(), l.getMyTarget(), st);

		}

	}

	public void setNeighborhoods(List<CMPNeighborhood> neighs) {
		this.neighborhoods.addAll(neighs);
		neigh_index = 0;
		this.neighborhood_explorer = neighs.get(neigh_index);
	}

	protected void changeNeighborhood() {
		neigh_index++;
		if (neigh_index >= neighborhoods.size()) {
			neigh_index = 0;
		}
		neighborhood_explorer = neighborhoods.get(neigh_index);
	}

	public CMPSolution relink(CMPSolution s, CMPSolution t) {
		List<Container> difference = computeDifference(s, t);
		List<Container> diff = new ArrayList<Container>();

		CMPSolution best = (s.getValue() <= t.getValue()) ? s : t;
		CMPSolution current = new CMPSolution();
		for (Container v : s.getTable().keySet()) {
			int st = s.getTable().get(v).intValue();
			stubs_after.get(st).forceAllocation(v, stubs_after, current, dc);
			current.getTable().put(v, new Integer(st));
			updateLinks(s.getFlows().get(v), true);
			current.getFlows().put(v, s.getFlows().get(v));
		}
		current.setValue(s.getValue());

		for (int iter = 0; iter < iterations; iter++) {
			diff.addAll(difference);

			HashMap<Container, Double> cost_gain = new HashMap<Container, Double>();
			ArrayList<Container> move = new ArrayList<Container>();
			while (!endCondition(diff, difference)) {
				cost_gain.clear();
				move.clear();

				for (int i = 0; i < diff.size(); i++) {
					Double tmp = costDifference(current, t, diff.get(i));
					cost_gain.put(diff.get(i), tmp);

				}

				diff.sort(this.new CostComparator(cost_gain));
				for (int contatore = 0; contatore < n_moves; contatore++) {
					if (diff.isEmpty())
						break;
					Container m = diff.remove(rng.nextInt(Math.max(1, (int) (diff.size() * alfa))));
					move.add(m);
				}

				current = applyMove(current, t, move); // muove un batch di container
				if (current.getValue() < best.getValue() - min_delta) {
					if (Main.display) {
						System.out.println("BETTER: " + current.getValue() + "\t" + best.getValue());
					}
					CMPSolution incumbent = (CMPSolution) current.clone();
					int count = 0;
					neigh_index = 0;
					neighborhood_explorer = neighborhoods.get(neigh_index);
					do {
						CMPSolution newincumbent = localSearch(incumbent);
						if (!(newincumbent.getValue() < incumbent.getValue() - min_delta)) {
							count++;
						} else {
							count = 0;
						}
						incumbent = newincumbent;
						changeNeighborhood();
					} while (count < neighborhoods.size() && neighborhoods.size() > 1);

					best = (CMPSolution) incumbent.clone();
					reset(incumbent, current);
				} else {
					// System.out.println("WORSE: "+current.getValue()+"\t"+best.getValue());
				}

			}
			// soft-reset for next iteration
			reset(current, s);
		}

		// hard-reset

		ArrayList<Container> keys = new ArrayList<Container>();
		keys.addAll(current.getTable().keySet());
		for (Container v : keys) {
			int st = current.getTable().get(v).intValue();
			stubs_after.get(st).remove(v, stubs_after, current, dc);
			current.getTable().remove(v);
			updateLinks(current.getFlows().remove(v), false);
		}

		return best;
	}

	protected CMPSolution applyMove(CMPSolution current, CMPSolution target, ArrayList<Container> move) {

		while (!move.isEmpty()) {
			// System.out.println("Applymove");
			boolean prev_feasib = (current.getValue() != Double.POSITIVE_INFINITY);
			Container m = move.remove(0);
			double delta = costDifference(current, target, m).doubleValue();
			stubs_after.get(current.getTable().get(m).intValue()).remove(m, stubs_after, current, dc);
			current.getTable().remove(m);
			updateLinks(current.getFlows().remove(m), false);

			stubs_after.get(target.getTable().get(m).intValue()).forceAllocation(m, stubs_after, current, dc);
			current.getTable().put(m, target.getTable().get(m));
			updateLinks(target.getFlows().get(m), true);
			current.getFlows().put(m, target.getFlows().get(m));

			boolean b = checkLinks(current.getFlows().get(m));
			
			if (prev_feasib && b) {
				current.setValue(current.getValue() + delta);
			} else {
				current.setValue(Double.POSITIVE_INFINITY);
				evaluate(current);
			}
		}

		return current;

	}

	protected boolean checkLinks(List<LinkFlow> ls) {
	
		for(LinkFlow lf : ls) {
			if(lf.getLink().getResCapacity() < 0) {
				return false;
			}
		}
		return true;
	}
	
	protected double evaluate(CMPSolution sol) {

		if (sol.getValue() < Double.POSITIVE_INFINITY)
			return sol.getValue(); // lazy

		if (!checkFeasibility(sol)) {
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
		for (List<Container> ls : input.getClustersOBL()) {
			all_conts.addAll(ls);
		}
		for (List<Container> ls : input.getClustersOPT()) {
			all_conts.addAll(ls);
		}

		double p_value = 0;
		Set<Server> olds = new TreeSet<Server>();
		Set<Server> neWs = new TreeSet<Server>();

		for (Container vm : all_conts) {
			Server old = dc.getPlacement().get(vm);
			Server neW = stubs_after.get(sol.getTable().get(vm)).getRealServ();
			p_value += (CPUcalculator.fractionalUtilization(vm, neW)) * (neW.getP_max() - neW.getP_idle());
			p_value -= (CPUcalculator.fractionalUtilization(vm, old)) * (old.getP_max() - old.getP_idle());
			olds.add(old);
			neWs.add(neW);
		}

		for (Server s : olds) {
			if (!s.isStateON() && !neWs.contains(s)) {
				p_value -= s.getP_idle();
			}
		}

		for (Server s : neWs) {
			if (!olds.contains(s) && !s.isStateON()) {
				p_value += s.getP_idle();
			}
		}

		List<Container> optional = new ArrayList<Container>();
		optional.addAll(input.getSinglesOPT());
		for (List<Container> ls : input.getClustersOPT()) {
			optional.addAll(ls);
		}

		double migr_value = 0;
		for (Container vm : optional) {
			if (dc.getPlacement().get(vm).getId() == sol.getTable().get(vm).intValue()) {
				migr_value += 1;
			}
			migr_value -= 1;
		}

		double value = p_value * pow_coeff + t_value * traff_coeff + migr_value * migr_coeff;
		// System.out.println(p_value+" + "+t_value+" + "+migr_value);
		sol.setValue(value);
		return value;
	}

	protected boolean checkFeasibility(CMPSolution sol) {
		
		List<Container> all_migrating = new ArrayList<Container>();
		all_migrating.addAll(input.getSinglesOBL());
		all_migrating.addAll(input.getSinglesOPT());
		for (List<Container> ls : input.getClustersOBL()) {
			all_migrating.addAll(ls);
		}
		for (List<Container> ls : input.getClustersOPT()) {
			all_migrating.addAll(ls);
		}

		if (all_migrating.size() != sol.getTable().keySet().size()) {
			System.out.println("MISSING SOMETHING \t" + all_migrating.size() + "\t" + sol.getTable().keySet().size());
			return false;
		}
		
		HashMap<Link, Double> tab = new HashMap<Link, Double>();
		for(Container v : all_migrating) {
			for(LinkFlow lf : sol.getFlows().get(v)) {
				Double d = tab.get(lf.getLink().getRealLink());
				if(d == null) {
					tab.put(lf.getLink().getRealLink(), new Double(lf.getFlow()));					
				}else {
					tab.remove(lf.getLink().getRealLink());
					tab.put(lf.getLink().getRealLink(), new Double(lf.getFlow()+d.doubleValue()));
				}
			}
		}
		
		for(Link l: tab.keySet()) {
			if(l.getResCapacity() < tab.get(l).doubleValue()) {
				return false;
			}
		}
		return true;

	}
	
	protected List<Container> computeDifference(CMPSolution x, CMPSolution y) {
		ArrayList<Container> difference = new ArrayList<Container>();
		for(Container v: x.getTable().keySet()) {
			if(x.getTable().get(v).intValue() != y.getTable().get(v).intValue()) {
				difference.add(v);
			}
		}
		return difference;
	}

	protected Double costDifference(CMPSolution current, CMPSolution t, Container v) {
		double t_cost1 = 0;
		double t_cost2 = 0;
		
		ServerStub st1 = stubs_after.get(current.getTable().get(v).intValue());
		ServerStub st2 = stubs_after.get(t.getTable().get(v).intValue());
		
		st1.remove(v, stubs_after, current, dc);
		boolean is_on1 = st1.isState();  // serve per dopo
		current.getTable().remove(v);
		if (!(st2.allocate(v, stubs_after, current, dc, false))) {
			t_cost2 = Double.POSITIVE_INFINITY;
			st1.forceAllocation(v, stubs_after, current, dc); // rollback
			current.getTable().put(v, new Integer(st1.getId()));
			return new Double(t_cost2);
		}
	//	if(current.getValue() == Double.POSITIVE_INFINITY) return new Double(Double.NEGATIVE_INFINITY);
		
		st1.forceAllocation(v, stubs_after, current, dc); // rollback
		current.getTable().put(v, new Integer(st1.getId()));

		Customer r = Customer.custList.get(v.getMy_customer());
		ArrayList<Container> conts = r.getContainers();

		for (Container c : conts) {
			Server s = dc.getPlacement().get(c);
			Double t1 = r.getTraffic().get(new C_Couple(v, c));
			Double t2 = r.getTraffic().get(new C_Couple(c, v));
			if (!(t1 == null)) {
				t_cost1 -= dc.getCosts()[st1.getId()][s.getId()] * t1.doubleValue();
				t_cost2 += dc.getCosts()[st2.getId()][s.getId()] * t1.doubleValue();
			}
			if (!(t2 == null)) {
				t_cost1 -= dc.getCosts()[s.getId()][st1.getId()] * t2.doubleValue();
				t_cost2 += dc.getCosts()[s.getId()][st2.getId()] * t2.doubleValue();
			}
		}
		conts = r.getNewContainers();

		for (Container c : conts) {
			Integer s = current.getTable().get(c);
			if (!(s == null)) {
				Double t1 = r.getTraffic().get(new C_Couple(v, c));
				Double t2 = r.getTraffic().get(new C_Couple(c, v));
				if (!(t1 == null)) {
					t_cost1 -= dc.getCosts()[st1.getId()][s.intValue()] * t1.doubleValue();
					t_cost2 += dc.getCosts()[st2.getId()][s.intValue()] * t1.doubleValue();
				}
				if (!(t2 == null)) {
					t_cost1 -= dc.getCosts()[s.intValue()][st1.getId()] * t2.doubleValue();
					t_cost2 += dc.getCosts()[s.intValue()][st2.getId()] * t2.doubleValue();
				}
			}
		}

		double p_cost = 0;
		p_cost -= CPUcalculator.fractionalUtilization(v, st1.getRealServ())*(st1.getRealServ().getP_max() - st1.getRealServ().getP_idle());
		p_cost += CPUcalculator.fractionalUtilization(v, st2.getRealServ())*(st2.getRealServ().getP_max() - st2.getRealServ().getP_idle());
		if(!is_on1) {
			p_cost -= st1.getRealServ().getP_idle();
		}
		if(!st2.isState()) {
			p_cost += st2.getRealServ().getP_idle();
		}
		
		double m_cost = 0;
		if(inputTable.get(v)) {
			boolean tmp1 = (dc.getPlacement().get(v).getId() == current.getTable().get(v).intValue());
			boolean tmp2 = (dc.getPlacement().get(v).getId() == t.getTable().get(v).intValue());
			
			if(tmp1)m_cost -= 1;
			if(tmp2)m_cost += 1;
		}
		
		double cost = traff_coeff*(t_cost1+t_cost2) + pow_coeff*p_cost + migr_coeff*m_cost;
		return new Double(cost);
	}
	
	protected CMPSolution localSearch(CMPSolution init_sol) {
		CMPSolution sol = (CMPSolution) init_sol.clone();
		evaluate(sol);

		CMPSolution best_neighbor = sol;

		// System.out.println("start local search");

		do {
		//	 System.out.println("Try new neighborhood");
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

	protected boolean endCondition(List<Container> diff, List<Container> initial_diff) {
		return diff.size() <= ((1-beta)*initial_diff.size());
	}
	
	protected void reset(CMPSolution current, CMPSolution s) {
		List<Container> difference = new ArrayList<Container>();
		difference = computeDifference(s,current);
		for(Container v: difference) {
			int st = current.getTable().get(v).intValue();
			stubs_after.get(st).remove(v, stubs_after, current, dc);
			current.getTable().remove(v);
			updateLinks(current.getFlows().remove(v), false);
		}
		for(Container v: difference) {
			int st = s.getTable().get(v).intValue();
			stubs_after.get(st).forceAllocation(v, stubs_after, current, dc);
			current.getTable().put(v, new Integer(st));
			updateLinks(s.getFlows().get(v),true);
			current.getFlows().put(v, s.getFlows().get(v));
		}
		current.setValue(s.getValue());
	}
	
	protected void updateLinks(List<LinkFlow> flow, boolean sign) {

		for (LinkFlow lf : flow) {
			LinkStub l = lf.getLink();
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			if (sign) {
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
			} else {
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
			}

		}

	}
	
	protected class CostComparator implements Comparator<Container> {

		private HashMap<Container, Double> map;

		CostComparator(HashMap<Container, Double> mp) {
			map = mp;
		}

		@Override
		public int compare(Container arg0, Container arg1) {

			return (int) Math.signum(map.get(arg0) - map.get(arg1));
		}

	}
}
