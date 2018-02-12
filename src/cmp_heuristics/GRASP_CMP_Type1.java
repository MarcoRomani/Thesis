package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.CPUcalculator;
import general.Container;
import general.Link;
import general.Node;
import general.Pod;
import general.Rack;
import general.Server;

public class GRASP_CMP_Type1 extends GRASP_CMP_Scheme {

	public GRASP_CMP_Type1(CMPDataCenter dc, List<Container> mandatory, List<Container> optional) {
		this.mandatory = mandatory;
		this.optional = optional;
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
	protected CMPSolution greedy_rand_constr(CMPSolution sol, List<Container> toPlace, double alfa) {

		
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();

		while (!toPlace.isEmpty()) {
			costs.clear();
			RCL.clear();
			Container m = toPlace.remove(0);

			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < stubs_after.size(); i++) {
				double tmp = incrementalCost(m, stubs_after.get(i));
				costs.add(new Double(tmp));
				if (tmp < min)
					min = tmp;
				if (tmp > max && tmp < Double.POSITIVE_INFINITY)
					max = tmp;
			}

			for (int i = 0; i < costs.size(); i++) {
				if (costs.get(i).doubleValue() < min + alfa * (max - min)) {
					RCL.add(stubs_after.get(i));
				}
			}

			boolean found = false;
			while (!RCL.isEmpty() && !found) {
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				Response r = canMigrate(m, dc.getPlacement().get(m).getId(), e.getId());
				found = r.getAnswer();
				if (found) {
					e.forceAllocation(m, stubs_after, sol, dc);
					sol.getTable().put(m, new Integer(e.getId()));
					updateLinks(r.getFlow(),true);
					sol.getFlows().put(m, r.getFlow());
				}

			}

		}

		return sol;
	}

	protected Response canMigrate(Container m, int s, int t) {

		double c_state = m.getState() / MIGR_TIME;
		KShortestPaths<Node, LinkStub> kp = new KShortestPaths<Node, LinkStub>(graph, k_paths, maxHops);
		Server _s = stubs_after.get(s).getRealServ();
		Server _t = stubs_after.get(t).getRealServ();
		List<GraphPath<Node, LinkStub>> paths = kp.getPaths(_s, _t);
		List<Double> flows = new ArrayList<Double>();

		for (int i = 0; i < paths.size(); i++) {

			if (c_state <= 0 + min_delta) {
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
				tmp.setResCapacity(tmp.getResCapacity() - flows.get(i).doubleValue());
			}

		}

		boolean can = (c_state <= 0 + min_delta) ? true : false;
		List<LinkFlow> fl = new ArrayList<LinkFlow>();
		if (can) {
			for (int k = 0; k < paths.size(); k++) {
				if (flows.get(k).doubleValue() == 0)
					continue;
				for (LinkStub lst : paths.get(k).getEdgeList()) {
					fl.add(new LinkFlow(lst, flows.get(k).doubleValue()));
				}
			}
		}
		Response resp = new Response(can, fl);

		// rollback
		for (int k = 0; k < paths.size(); k++) {
			if (flows.get(k).doubleValue() == 0)
				continue;
			for (LinkStub lst : paths.get(k).getEdgeList()) {
				lst.setResCapacity(lst.getResCapacity() + flows.get(k).doubleValue());
			}
		}

		return resp;
	}

	// sign = 1 subtract, sign =0 add
	protected void updateLinks(List<LinkFlow> flow, boolean sign) {
		
		for(LinkFlow lf : flow) {
			LinkStub l = lf.getLink();
			if(sign) {
			    l.setResCapacity(l.getResCapacity() - lf.getFlow());
			}else {
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
			}
			graph.setEdgeWeight(l, (1/(l.getResCapacity() + inv_offset)));
			
		}

	}

	@Override
	protected double incrementalCost(Container c, ServerStub s) {
		double pow_cost = 0;
		double traff_cost = 0;
		double migr_cost = 0;
		
		Server old = dc.getPlacement().get(c);
		double old_pow = (old.getP_max() - old.getP_idle())*CPUcalculator.fractionalUtilization(c, old);
		double new_pow = (s.getRealServ().getP_max() - s.getRealServ().getP_idle())*CPUcalculator.fractionalUtilization(c, s.getRealServ());
		double fix_cost = (!s.isState()) ? s.getRealServ().getP_idle() : 0;   // symmetric fix_gain has already been counted by the caller
		pow_cost = new_pow - old_pow + fix_cost;
		
		// TRAFFIC COST
		
		return 0;
	}

	@Override
	protected void changeNeighborhood() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	protected void buildGraph() {

		Set<Link> links = dc.getNetwork().edgeSet();
		Set<Node> nodes = dc.getNetwork().vertexSet();

		for (Node n : nodes) {
			graph.addVertex(n);
		}

		for (Link l : links) {
			LinkStub st = new LinkStub(l);
			graph.addEdge(l.getMySource(), l.getMyTarget(), st);
			graph.setEdgeWeight(st, (1 / (st.getResCapacity() + inv_offset))); // distance = inverse of residual capacity
		}

	}
}
