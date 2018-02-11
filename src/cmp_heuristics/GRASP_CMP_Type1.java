package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
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
	protected CMPSolution greedy_rand_constr(List<Container> toPlace, double alfa) {

		CMPSolution sol = new CMPSolution();
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
			while (!RCL.isEmpty() || found) {
				ServerStub e = RCL.remove(rng.nextInt(RCL.size()));
				Response r = canMigrate(m, dc.getPlacement().get(m).getId(), e.getId());
				found = r.getAnswer();
				if (found) {
					e.forceAllocation(m, stubs_after, sol, dc);
					sol.getTable().put(m, new Integer(e.getId()));
					updateLinks(r.getFlow());
					sol.getFlows().put(m, r.getFlow());
				}

			}

		}

		return null;
	}

	private Response canMigrate(Container m, int s, int t) {

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

	private void updateLinks(ArrayList<LinkFlow> flow) {
		// TODO Auto-generated method stub

	}

	@Override
	protected double incrementalCost(Container c, ServerStub s) {
		// TODO Auto-generated method stub
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
			graph.setEdgeWeight(st, (1 / st.getResCapacity())); // distance = inverse of residual capacity
		}

	}
}
