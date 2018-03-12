package cmp_heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

import cpp_heuristics.InfeasibilityException;
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
import general.S_Couple;
import general.Server;
import ltCMP.CMPMain;

public class GRASP_CMP_Type1 extends GRASP_CMP_Scheme {

	public static int inner_cpp_iter = 1;
	public static int BIG_M = 100000;
	private static double my_delta = 0.00000001;

	public GRASP_CMP_Type1(CMPDataCenter dc, Input input) {
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

		stubs_migr = new ArrayList<LinkStub>();
		stubs_after = new ArrayList<ServerStub>();

		this.dc = dc;
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					stubs_after.add(new ServerStub(s));
				}
			}
		}

		buildGraph();
	}

	@Override
	protected CMPSolution greedy_rand_constr(Input input, double alfa) throws InfeasibilityException {
		CMPSolution sol = new CMPSolution();

		List<Container> singles = new ArrayList<Container>();
		singles.addAll(input.getSinglesOBL());

		List<List<Container>> clusters = new ArrayList<List<Container>>();
		for (List<Container> ls : input.getClustersOBL()) {
			List<Container> clust_copy = new ArrayList<Container>();
			clust_copy.addAll(ls);
			clust_copy.sort(comp);
			clusters.add(clust_copy);
		}
		List<List<Container>> cs_copy = new ArrayList<List<Container>>();
		sortByFirst(clusters, cs_copy);

		List<Container> rest = new ArrayList<Container>();

		for (List<Container> cluster : cs_copy) {
			if(CMPMain.display) {
	//		System.out.println("DOING NEW OBL CLUSTER");
			}
			sol = cluster_rand_constr(sol, cluster, alfa, rest);
		}

		singles.addAll(rest);
		singles.sort(comp);
		if(CMPMain.display) {
	//	System.out.println("DOING OBL SINGLES");
		}
		
		sol = single_rand_constr(sol, singles, alfa);

		clusters.clear();
		cs_copy.clear();
		singles.clear();
		rest.clear();

		// OPTIONALS

		singles.addAll(input.getSinglesOPT());
		for (List<Container> ls : input.getClustersOPT()) {
			List<Container> clust_copy = new ArrayList<Container>();
			clust_copy.addAll(ls);
			clust_copy.sort(comp);
			clusters.add(clust_copy);
		}

		sortByFirst(clusters, cs_copy);

		for (List<Container> cluster : clusters) {
			if(CMPMain.display) {
	//		System.out.println("DOING NEW OPT CLUSTER");
			}
			sol = cluster_rand_constr(sol, cluster, alfa, rest);
		}
		singles.addAll(rest);
		if(CMPMain.display) {
//		System.out.println("DOING OPT SINGLES");
		}
		singles.sort(comp);
		sol = single_rand_constr(sol, singles, alfa);

		return sol;

	}

	protected CMPSolution single_rand_constr(CMPSolution sol, List<Container> toPlace, double alfa)
			throws InfeasibilityException {

		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();

		while (!toPlace.isEmpty()) {
			costs.clear();
			RCL.clear();
			Container m = toPlace.remove(0);

			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < stubs_after.size(); i++) {
				double tmp = incrementalCost(m, stubs_after.get(i), sol);
				costs.add(new Double(tmp));
				if (tmp < min)
					min = tmp;
				if (tmp > max && tmp < Double.POSITIVE_INFINITY)
					max = tmp;
			}

			if (min == Double.POSITIVE_INFINITY)
				throw new InfeasibilityException(sol);

			for (int i = 0; i < costs.size(); i++) {
				if (costs.get(i).doubleValue() <= min + alfa * (max - min)) {
					RCL.add(stubs_after.get(i));
				}
			}

		//	System.out.println(RCL.size());
			boolean found = false;
			ArrayList<Container> tmp = new ArrayList<Container>();
			tmp.add(m);

			while (!RCL.isEmpty() && !found) {
				// System.out.println(RCL.size());
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				Response r = null;
				if (e.getRealServ() != dc.getPlacement().get(m)) {
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
			
			if(!found) {
				throw new InfeasibilityException(sol);
			}

		}

		return sol;
	}

	protected CMPSolution cluster_rand_constr(CMPSolution incumbent, List<Container> cluster, double alfa,
			List<Container> rest) {

		
	
		ArrayList<Container> my_rest = new ArrayList<Container>();
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<Rack> racks = new ArrayList<Rack>();
		double min_cost = Double.POSITIVE_INFINITY;
		double max_cost = Double.NEGATIVE_INFINITY;

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				racks.add(r);

				double tmp = rackCost(cluster, r, incumbent);
				costs.add(new Double(tmp));
				if (tmp < min_cost)
					min_cost = tmp;
				if (tmp != Double.POSITIVE_INFINITY && tmp > max_cost)
					max_cost = tmp;
			}
		}

		ArrayList<Rack> RCL = new ArrayList<Rack>();
		for (int i = 0; i < racks.size(); i++) {
			if (costs.get(i).doubleValue() <= min_cost + alfa * (max_cost - min_cost)) {
				RCL.add(racks.get(i));
			}
		}

		boolean found = false;
		Server s = dc.getPlacement().get(cluster.get(0));
		while (!found && !RCL.isEmpty()) {

	//		 System.out.println(RCL.size());
			my_rest.clear();
			Rack r = RCL.remove(rng.nextInt(RCL.size()));
			Response resp = canMigrate(cluster, s, r.getSwitches().get(0));
			found = resp.getAnswer();

			if (found) {
				double all_state = 0;
				for (Container m : cluster) {
					all_state += m.getState();
				}
				updateLinks(resp.getFlow(), true);
				ArrayList<LinkFlow> fl = resp.getFlow();
				for (Container m : cluster) {
					double ratio = m.getState() / all_state;
					ArrayList<LinkFlow> my_flow = new ArrayList<LinkFlow>();
					for (LinkFlow l : fl) {
						my_flow.add(new LinkFlow(l.getLink(), l.getFlow() * ratio));
					}
					incumbent.getFlows().put(m, my_flow);

				}
				// INNER ASSIGNMENT

				CMPSolution newincumbent = inner_cpp(incumbent, r, cluster, alfa, my_rest);
				for (Container v : cluster) {
					Integer serv = newincumbent.getTable().get(v);
					if (serv == null)
						continue;
					stubs_after.get(serv.intValue()).forceAllocation(v, stubs_after, incumbent, dc);

					incumbent.getTable().put(v, serv);
					List<LinkFlow> oldflows = incumbent.getFlows().remove(v);
					updateLinks(oldflows, false);
					List<LinkFlow> flows;
					if (serv.intValue() != dc.getPlacement().get(v).getId()) {
						flows = newincumbent.getFlows().get(v);
						incumbent.getFlows().put(v, flows);
					} else {
						flows = nonMigrate(v, stubs_after.get(serv.intValue()), incumbent).getFlow();
						incumbent.getFlows().put(v, new ArrayList<LinkFlow>());
					}
					updateLinks(flows, true);

				}

				for (Container v : my_rest) {
					List<LinkFlow> list = incumbent.getFlows().get(v);
					updateLinks(list, false);
					incumbent.getFlows().remove(v);
				}

			}

		}

		rest.addAll(my_rest);
		
		return incumbent;

	}

	protected Response nonMigrate(Container v, ServerStub _s, CMPSolution sol) {

		/// we put the normal traffics with precomputed flows instead of the migration
		/// burst

		Server s = _s.getRealServ();
		Customer r = Customer.custList.get(v.getMy_customer());
		List<Container> conts = r.getContainers();
		List<LinkFlow> flows = new ArrayList<LinkFlow>();

		Double c_c0 = r.getTraffic().get(new C_Couple(v, Container.c_0));
		if (c_c0 != null) {
			List<Link> path = dc.getTo_wan().get(s);
			for (Link l : path) {
				LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

				LinkFlow f = new LinkFlow(lstub.getRealLink(), c_c0.doubleValue());
				flows.add(f);
			}
		}
		Double c0_c = r.getTraffic().get(new C_Couple(Container.c_0, v));
		if (c0_c != null) {
			List<Link> path = dc.getFrom_wan().get(s);
			for (Link l : path) {
				LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

				LinkFlow f = new LinkFlow(lstub.getRealLink(), c0_c.doubleValue());
				flows.add(f);
			}
		}

		for (Container v2 : conts) {
			Double t1 = r.getTraffic().get(new C_Couple(v, v2));
			Double t2 = r.getTraffic().get(new C_Couple(v2, v));
			Server s2 = dc.getPlacement().get(v2);
			if (t1 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s, s2));
				for (Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

					LinkFlow f = new LinkFlow(lstub.getRealLink(), t1.doubleValue());
					flows.add(f);
				}
			}
			if (t2 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s2, s));
				for (Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

					LinkFlow f = new LinkFlow(lstub.getRealLink(), t2.doubleValue());
					flows.add(f);
				}
			}

		}
		conts = r.getNewContainers();
		for (Container v2 : conts) {
			Integer s2 = sol.getTable().get(v2);
			if (s2 == null)
				continue;
			if (s2.intValue() != dc.getPlacement().get(v2).getId())
				continue; // IMPORTANTE

			Double t1 = r.getTraffic().get(new C_Couple(v, v2));
			Double t2 = r.getTraffic().get(new C_Couple(v2, v));
			if (t1 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s, stubs_after.get(s2).getRealServ()));
				for (Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

					LinkFlow f = new LinkFlow(lstub.getRealLink(), t1.doubleValue());
					flows.add(f);
				}
			}
			if (t2 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(stubs_after.get(s2).getRealServ(), s));
				for (Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());

					LinkFlow f = new LinkFlow(lstub.getRealLink(), t2.doubleValue());
					flows.add(f);
				}
			}

		}

		boolean can = true;
		for (LinkFlow lf : flows) {
			Link l = lf.getLink();
			LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
			if (lstub.getResCapacity() < 0) {
				can = false;
			}
			lstub.setResCapacity(lstub.getResCapacity() - lf.getFlow());
		}

		// rollback
		for (LinkFlow lf : flows) {
			Link l = lf.getLink();
			LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
			lstub.setResCapacity(lstub.getResCapacity() + lf.getFlow());
		}

		return new Response(can, flows);

	}

	protected Response canMigrate(List<Container> cluster, Node s, Node t) {

	
		
		if (s.getId() == t.getId()) { // SHOULD NOT HAPPEN
			List<LinkFlow> fl = new ArrayList<LinkFlow>();
			return new Response(true, fl);
		}

		double c_state = 0;
		for (Container m : cluster) {
			c_state += m.getState() / MIGR_TIME;
		}

	//	KShortestPaths<Node, LinkStub> kp = new KShortestPaths<Node, LinkStub>(graph, k_paths, maxHops);
		
	
	//	List<GraphPath<Node, LinkStub>> paths = kp.getPaths(s, t);
		List<GraphPath<Node, LinkStub>> paths = new ArrayList<GraphPath<Node,LinkStub>>();
			
		
		List<Double> flows = new ArrayList<Double>();

	//	for (int i = 0; i < paths.size(); i++) {
	    for (int i = 0; i < k_paths; i++) {
	    	
	    	DijkstraShortestPath<Node,LinkStub> alg = new DijkstraShortestPath<Node,LinkStub>(graph);
	    	
			paths.add(alg.getPath(s, t));
			
			if (c_state <= 0 + my_delta) {
				flows.add(new Double(0));
				continue;
			}

			
			GraphPath<Node, LinkStub> gp = paths.get(i);
	
			List<LinkStub> ls = gp.getEdgeList();
			double min = ls.get(0).getResCapacity();

			for (int j = 0; j < ls.size(); j++) {
				if (ls.get(j).getResCapacity() < min) {
					min = ls.get(j).getResCapacity();
				}
			}
			flows.add(Math.min(c_state, min));
			c_state -= flows.get(i).doubleValue();

			for (int j = 0; j < ls.size(); j++) {
				LinkStub tmp = ls.get(j);
				
				if (tmp.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				
				
				tmp.setResCapacity(tmp.getResCapacity() - flows.get(i).doubleValue());
				
				if (tmp.getResCapacity() > 0) {
					graph.setEdgeWeight(tmp, (1 / (tmp.getResCapacity() + GRASP_CMP_Scheme.inv_offset)));
				}else {
					graph.setEdgeWeight(tmp, BIG_M);
				}
			}

		}

		boolean can = (c_state <= 0 + my_delta) ? true : false;
		List<LinkFlow> fl = new ArrayList<LinkFlow>();
		if (can) {
			for (int k = 0; k < paths.size(); k++) {
				if (flows.get(k).doubleValue() == 0)
					continue;
				for (LinkStub lst : paths.get(k).getEdgeList()) {
					fl.add(new LinkFlow(lst.getRealLink(), flows.get(k).doubleValue()));
				}
			}
		}
		Response resp = new Response(can, fl);

		// rollback 
		/*
		for (int k = 0; k < paths.size(); k++) {
			if (flows.get(k).doubleValue() == 0)
				continue;
			for (LinkStub lst : paths.get(k).getEdgeList()) {
				lst.setResCapacity(lst.getResCapacity() + flows.get(k).doubleValue());
			}
		}

*/
		updateLinks(resp.getFlow(),false);
		
	
		return resp;
	}

	// sign = 1 subtract, sign =0 add
	protected void updateLinks(List<LinkFlow> flow, boolean sign) {

		for (LinkFlow lf : flow) {
			LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			if (sign) {
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
			} else {
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
			}
			if (l.getResCapacity() > 0) {
				graph.setEdgeWeight(l, (1 / (l.getResCapacity() + GRASP_CMP_Scheme.inv_offset)));
			}else {
				graph.setEdgeWeight(l, BIG_M);
			}
		}

	}

	protected double rackCost(List<Container> cluster, Rack r, CMPSolution incumbent) {

		double pow_cost = 0;
		double traffic_cost = 0;

		double req = computeRequirement(cluster, r.getHosts().get(0));

		double rackcap = computeCapacity(r);

		Server tmp = r.getHosts().get(0);
		pow_cost += (rackcap < req) ? tmp.getP_idle() : 0;
		for (Container c : cluster) {
			pow_cost += CPUcalculator.fractionalUtilization(c, tmp) * (tmp.getP_max() - tmp.getP_idle());
		}

		Customer cust = Customer.custList.get(cluster.get(0).getMy_customer());
		List<Container> conts = cust.getContainers();

		for (Container c : cluster) {
			for (Container c2 : conts) {

				int s2 = dc.getPlacement().get(c2).getId();

				Double t1 = cust.getTraffic().get(new C_Couple(c, c2));
				Double t2 = cust.getTraffic().get(new C_Couple(c2, c));

				traffic_cost += (t1 == null) ? 0 : t1.doubleValue() * (dc.getCosts()[tmp.getId()][s2] - 1);
				traffic_cost += (t2 == null) ? 0 : t2.doubleValue() * (dc.getCosts()[s2][tmp.getId()] - 1);
			}
			conts = cust.getNewContainers();
			for (Container c2 : conts) {

				Integer _s2 = incumbent.getTable().get(c2);
				if (_s2 == null)
					continue;
				int s2 = _s2.intValue();

				Double t1 = cust.getTraffic().get(new C_Couple(c, c2));
				Double t2 = cust.getTraffic().get(new C_Couple(c2, c));

				traffic_cost += (t1 == null) ? 0 : t1.doubleValue() * (dc.getCosts()[tmp.getId()][s2] - 1);
				traffic_cost += (t2 == null) ? 0 : t2.doubleValue() * (dc.getCosts()[s2][tmp.getId()] - 1);
			}

		}

		return pow_cost * pow_coeff + traffic_cost * traff_coeff;

	}

	protected CMPSolution inner_cpp(CMPSolution sol, Rack r, List<Container> cluster, double alfa,
			List<Container> rest) {

		
	
		double best_delta = Double.POSITIVE_INFINITY;
		CMPSolution best = (CMPSolution) sol.clone();
		List<Container> best_rest = new ArrayList<Container>();

		List<ServerStub> E = new ArrayList<ServerStub>();
		for (Server s : r.getHosts()) {
			E.add(stubs_after.get(s.getId()));
		}

		List<Container> vms = new ArrayList<Container>();
		for (int iter = 0; iter < inner_cpp_iter; iter++) {

			// ArrayList<Container> my_rest = new ArrayList<Container>();
			CMPSolution copy = (CMPSolution) sol.clone();
			vms.clear();
			vms.addAll(cluster);
			ArrayList<Double> costs = new ArrayList<Double>();
			ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();
			ArrayList<Double> RCL_costs = new ArrayList<Double>();
			double delta = 0;

			while (!vms.isEmpty()) {
				costs.clear();
				RCL.clear();
				RCL_costs.clear();

				Container v = vms.remove(0);
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;
				for (ServerStub e : E) {
					double tmp = incrementalCost(v, e, copy);
					costs.add(new Double(tmp));
					if (tmp < min)
						min = tmp;
					if (tmp > max && tmp != Double.POSITIVE_INFINITY)
						max = tmp;
				}

				for (int i = 0; i < E.size(); i++) {
					if (costs.get(i) <= min + alfa * (max - min)) {
						RCL.add(E.get(i));
						RCL_costs.add(costs.get(i));

					}
				}

				if (RCL.isEmpty() || min == Double.POSITIVE_INFINITY) {

					continue;
				}

				int next = rng.nextInt(RCL.size());

				ServerStub chosen = RCL.get(next);

				chosen.forceAllocation(v, stubs_after, copy, dc);
				delta += RCL_costs.get(next);
				copy.getTable().put(v, chosen.getId());

				if (chosen.getId() != dc.getPlacement().get(v).getId()) {
					LinkStub in = graph.getEdge(r.getSwitches().get(0), chosen.getRealServ());
					boolean ans = ((v.getState() / MIGR_TIME) <= in.getResCapacity());
					if (ans) {
						in.setResCapacity(in.getResCapacity() - (v.getState() / MIGR_TIME));

						graph.setEdgeWeight(in, 1 / (in.getResCapacity() + inv_offset));
						List<LinkFlow> l = copy.getFlows().get(v);
						List<LinkFlow> n_l = new ArrayList<LinkFlow>(l);
						n_l.add(new LinkFlow(in.getRealLink(), (v.getState() / MIGR_TIME)));
						copy.getFlows().remove(v);
						copy.getFlows().put(v, n_l);
					} else {
						chosen.remove(v, stubs_after, copy, dc);
						copy.getTable().remove(v);
						delta -= RCL_costs.get(next);
						delta += BIG_M; // big M
					}
					// nonMigrate
				} else {
					List<LinkFlow> oldlist = copy.getFlows().remove(v);
					updateLinks(oldlist, false); // free bdw
					Response resp = nonMigrate(v, chosen, copy);
					if (resp.getAnswer()) {
						updateLinks(resp.getFlow(), true);
						copy.getFlows().put(v, new ArrayList<LinkFlow>());

					} else {
						updateLinks(oldlist, true);
						copy.getFlows().put(v, oldlist);
						chosen.remove(v, stubs_after, copy, dc);
						copy.getTable().remove(v);
						delta -= RCL_costs.get(next);
						delta += BIG_M; // big M
					}
				}
			}

			if (delta < best_delta) {
				best = (CMPSolution) copy.clone();
				best_delta = delta;
				// best_rest = my_rest;
			}
			// rollback
			for (Container v : cluster) {
				Integer s = copy.getTable().get(v);
				if (s != null) {

					stubs_after.get(s.intValue()).remove(v, stubs_after, copy, dc);
					copy.getTable().remove(v);

					if (s.intValue() != dc.getPlacement().get(v).getId()) {

						List<LinkFlow> l = copy.getFlows().get(v);
						List<LinkFlow> o_l = new ArrayList<LinkFlow>(l);
						LinkFlow lf = o_l.remove(l.size() - 1);
						copy.getFlows().remove(v);
						copy.getFlows().put(v, o_l);
						LinkStub lstub = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
						lstub.setResCapacity(lstub.getResCapacity() + lf.getFlow());
						// graph.setEdgeWeight(lstub, 1 / (lstub.getResCapacity() + inv_offset)); fare
						// dopo per sicurezza

					} else {
						List<LinkFlow> l = nonMigrate(v, stubs_after.get(s.intValue()), copy).getFlow();
						copy.getFlows().remove(v);
						updateLinks(l, false); // free
						List<LinkFlow> oldlist = sol.getFlows().get(v);
						updateLinks(oldlist, true);
						List<LinkFlow> old_copy = new ArrayList<LinkFlow>(oldlist);
						copy.getFlows().put(v, old_copy);
					}
				}
			}
			for (Container v : cluster) {
				Integer s = copy.getTable().get(v);
				if (s == null)
					continue;
				for (LinkFlow lf : sol.getFlows().get(v)) {
					LinkStub lstub = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
					graph.setEdgeWeight(lstub, 1 / (lstub.getResCapacity() + inv_offset));
				}
			}

		}

		for (Container v : cluster) {

			if (best.getTable().get(v) == null) {
				best_rest.add(v);
			}
		}

		rest.addAll(best_rest);
		/*
		if (CMPMain.display) {
			System.out.println("CLUSTER: \t" + cluster.size());
			System.out.println("placed: \t" + (best.getTable().keySet().size() - sol.getTable().keySet().size()));
			System.out.println("MY_REST: \t" + best_rest.size());
		}*/
	
		return best;
	}

	@Override
	protected double incrementalCost(Container c, ServerStub s, CMPSolution incumbent) {
		double cost = 0;
		double pow_cost = 0;
		double traff_cost = 0;
		double migr_cost = 0;
		Server old = dc.getPlacement().get(c);
		boolean allowSamePosition = inputTable.get(c);

		if (!allowSamePosition && old.getId() == s.getId()) {
			cost = Double.POSITIVE_INFINITY;
			return cost;
		}

		if (!(s.allocate(c, stubs_after, incumbent, dc, Server.overUtilization_constant, false))) {
			cost = Double.POSITIVE_INFINITY;
			return cost;
		}

		// POWER COST

		double old_pow = (old.getP_max() - old.getP_idle()) * CPUcalculator.fractionalUtilization(c, old);
		double new_pow = (s.getRealServ().getP_max() - s.getRealServ().getP_idle())
				* CPUcalculator.fractionalUtilization(c, s.getRealServ());
		double fix_cost = (!s.isState()) ? s.getRealServ().getP_idle() : 0; // symmetric fix_gain has already been
																			// counted by the caller
		pow_cost = new_pow - old_pow + fix_cost;

		// TRAFFIC COST

		Customer r = Customer.custList.get(c.getMy_customer());
		List<Container> conts = r.getContainers();
		for (Container v : conts) {

			int s2 = dc.getPlacement().get(c).getId();

			Double t1 = r.getTraffic().get(new C_Couple(c, v));
			Double t2 = r.getTraffic().get(new C_Couple(v, c));
			if (!(t1 == null))
				traff_cost += dc.getCosts()[s.getId()][s2] * t1.doubleValue();
			if (!(t2 == null))
				traff_cost += dc.getCosts()[s2][s.getId()] * t2.doubleValue();
		}
		conts = r.getNewContainers();
		for (Container v : conts) {

			Integer _s2 = incumbent.getTable().get(v);
			if (_s2 == null)
				continue;
			int s2 = _s2.intValue();

			Double t1 = r.getTraffic().get(new C_Couple(c, v));
			Double t2 = r.getTraffic().get(new C_Couple(v, c));
			if (!(t1 == null))
				traff_cost += dc.getCosts()[s.getId()][s2] * t1.doubleValue();
			if (!(t2 == null))
				traff_cost += dc.getCosts()[s2][s.getId()] * t2.doubleValue();
		}

		if (allowSamePosition) {
			migr_cost -= 1;
			if (old.getId() == s.getId()) {
				migr_cost += 1;
			}
		}

		return (pow_coeff * pow_cost + traff_coeff * traff_cost + migr_coeff * migr_cost);
	}

	@Override
	protected void changeNeighborhood() {
		neigh_index++;
		if (neigh_index >= neighborhoods.size()) {
			neigh_index = 0;
		}
		neighborhood_explorer = neighborhoods.get(neigh_index);
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
			graph.setEdgeWeight(st, (1 / (st.getResCapacity() + inv_offset))); // distance = inverse of residual
																				// capacity
		}

	}

	@Override
	public void setNeighborhoods(List<CMPNeighborhood> neighs) {
		this.neighborhoods.addAll(neighs);
		neigh_index = 0;
		this.neighborhood_explorer = neighs.get(neigh_index);
	}

	protected double computeRequirement(List<Container> cluster, Server s) {
		double ramreq = 0;
		for (Container c : cluster) {
			ramreq += c.getMem();
		}
		return ramreq;
	}

	protected double computeCapacity(Rack r) {
		double rackram = 0;
		for (Server s : r.getHosts()) {
			if (stubs_after.get(s.getId()).isState()) {
				rackram += stubs_after.get(s.getId()).getRes_mem();
			}
		}
		return rackram;
	}

	protected void sortByFirst(List<List<Container>> clusters, List<List<Container>> ordered) {
		List<Container> firsts = new ArrayList<Container>();
		HashMap<Container, List<Container>> t = new HashMap<Container, List<Container>>();
		for (List<Container> ls : clusters) {
			firsts.add(ls.get(0));
			t.put(ls.get(0), ls);
		}
		firsts.sort(comp);

		while (!firsts.isEmpty()) {
			Container tmp = firsts.remove(0);
			ordered.add(t.get(tmp));
		}

	}

	@Override
	protected void reset(CMPSolution sol) {

		ArrayList<Container> toRemove = new ArrayList<Container>();
		toRemove.addAll(sol.getTable().keySet());
		for (Container c : toRemove) {
			ServerStub tmp = stubs_after.get(sol.getTable().get(c).intValue());
			tmp.remove(c, stubs_after, sol, dc);

			////
			if (sol.getTable().get(c).intValue() != dc.getPlacement().get(c).getId()) {
				updateLinks(sol.getFlows().get(c),false);
				sol.getFlows().remove(c);
			}else {
				updateLinks(nonMigrate(c,tmp,sol).getFlow(),false);
				sol.getFlows().remove(c);
			}
			sol.getTable().remove(c);
		}
	}

}
