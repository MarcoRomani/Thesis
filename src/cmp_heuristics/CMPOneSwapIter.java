package cmp_heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;
import general.Link;
import general.Node;
import general.S_Couple;
import general.Server;

public class CMPOneSwapIter implements CMPNeighborhood {

	public static double inv_offset = GRASP_CMP_Scheme.inv_offset;
	public static double MIGR_TIME = GRASP_CMP_Scheme.MIGR_TIME;
	protected CMPDataCenter dc;
	protected CMPSolution sol = new CMPSolution();
	protected int index_one = 0;
	protected int index_two = 0;
	protected List<ServerStub> stubs_after;
	protected List<Container> conts = new ArrayList<Container>();
	protected CMPSolution copy;
	protected Double deltacurrent;
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph;
	protected Map<Container, Boolean> inputTable = new HashMap<Container, Boolean>();

	@Override
	public boolean hasNext() {
		if (index_one + index_two >= 2 * conts.size() - 3) {
			
			Container vm = conts.get(index_one);
			ServerStub st = stubs_after.get(sol.getTable().get(vm).intValue());
			if(st.getId() != dc.getPlacement().get(vm).getId()) {
				put(vm,st,copy,sol.getFlows().get(vm));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(vm));
				copy.getFlows().put(vm, n_ls);
			}else {
				List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
				put(vm,st,copy,ls);
				copy.getFlows().put(vm, new ArrayList<LinkFlow>());
			}
			return false;
		}
		return true;
	}

	@Override
	public CMPSolution next() {
		index_two += 1;
		if (index_two >= conts.size()) {
		
			
			Container vm = conts.get(index_one);
			ServerStub st = stubs_after.get(sol.getTable().get(vm));
			if(st.getId() != dc.getPlacement().get(vm).getId()) {
				put(vm,st,copy,sol.getFlows().get(vm));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(vm));
				copy.getFlows().put(vm, n_ls);
			}else {
				List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
				put(vm,st,copy,ls);
				copy.getFlows().put(vm, new ArrayList<LinkFlow>());
			}

			index_one += 1;
			index_two = index_one + 1;

			if (index_one >= conts.size()) {
				throw new NoSuchElementException();
			}

			 vm = conts.get(index_one);
			 st = stubs_after.get(sol.getTable().get(vm));
			if(st.getId() != dc.getPlacement().get(vm).getId()) {
				togli(vm,st,copy,sol.getFlows().get(vm));
				copy.getFlows().remove(vm);
			}else {
				List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
				togli(vm,st,copy,ls);
				copy.getFlows().remove(vm);
			}

			deltacurrent = deltaObj(conts.get(index_one),
					stubs_after.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);

		}

		return generateSolution();
	}

	protected CMPSolution generateSolution() {

		Container c1 = conts.get(index_one);
		Container c2 = conts.get(index_two);
		Integer s1 = sol.getTable().get(c1);
		Integer s2 = sol.getTable().get(c2);

		if (c1 == c2 || s1.intValue() == s2.intValue()) {
			return sol;
		}

		
		
		ServerStub st = stubs_after.get(s2.intValue());
		
		if(st.getId() != dc.getPlacement().get(c2).getId()) {
			togli(c2,st,copy,sol.getFlows().get(c2));
			copy.getFlows().remove(c2);
		}else {
			List<LinkFlow> ls = nonMigrate(c2,st,copy).getFlow();
			togli(c2,st,copy,ls);
			copy.getFlows().remove(c2);
		}

		Double deltacurrent_2 = deltaObj(c2, stubs_after.get(s2.intValue()), copy, false);
		Double deltanext_2 = deltaObj(c2, stubs_after.get(s1.intValue()), copy, true);
		if (deltanext_2.doubleValue() < Double.POSITIVE_INFINITY) {
			stubs_after.get(s1.intValue()).allocate(c2, stubs_after, copy, dc, true);
			copy.getTable().put(c2, s1);

			// CAN MIGRATE c2 IN S1
			int old2 = dc.getPlacement().get(c2).getId();
			Response resp2 = null;
			if (old2 != s1.intValue()) {
				resp2 = canMigrate(c2, dc.getPlacement().get(c2), stubs_after.get(s1.intValue()).getRealServ());
			} else {
				resp2 = nonMigrate(c2, stubs_after.get(s1.intValue()), copy);
			}

			List<LinkFlow> ls = resp2.getFlow();
			updateLinks(ls,true);

			Double deltanext = deltaObj(c1, stubs_after.get(s2.intValue()), copy, true);
			// CAN MIGRATE c1 in S2
			int old1 = dc.getPlacement().get(c1).getId();
			Response resp1 = null;
			if (old1 != s2.intValue()) {
				resp1 = canMigrate(c1, dc.getPlacement().get(c1), stubs_after.get(s2.intValue()).getRealServ());
			} else {
				resp1 = nonMigrate(c1, stubs_after.get(s2.intValue()), copy);
			}

			stubs_after.get(s1.intValue()).remove(c2, stubs_after, copy, dc);
			copy.getTable().remove(c2);
			updateLinks(ls,false);

			stubs_after.get(s2.intValue()).allocate(c2, stubs_after, copy, dc, true);
			copy.getTable().put(c2, s2);

			ls = sol.getFlows().get(c2);
			updateLinks(ls,true);
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>(ls);
		
			copy.getFlows().put(c2, neWls);

			if (resp1.getAnswer() && resp2.getAnswer() && deltanext.doubleValue()
					+ deltanext_2.doubleValue() < deltacurrent.doubleValue() + deltacurrent_2.doubleValue()) {
				CMPSolution nextSol = (CMPSolution) copy.clone();
				nextSol.getTable().remove(c2);
				nextSol.getFlows().remove(c2);
				nextSol.getTable().put(c1, s2);
				if (old1 != s2.intValue()) {
				nextSol.getFlows().put(c1, resp1.getFlow());
				}else {
					nextSol.getFlows().put(c1, new ArrayList<LinkFlow>());
				}
				nextSol.getTable().put(c2, s1);
				if (old2 != s1.intValue()) {
			     	nextSol.getFlows().put(c2, resp2.getFlow());
				}
				else{
					nextSol.getFlows().put(c2, new ArrayList<LinkFlow>());
				}
				
				nextSol.setValue(sol.getValue() - deltacurrent.doubleValue() - deltacurrent_2.doubleValue()
						+ deltanext.doubleValue() + deltanext_2.doubleValue());
				return nextSol;
			}
		} else {

			
			
			if(st.getId() != dc.getPlacement().get(c2).getId()) {
				put(c2,st,copy,sol.getFlows().get(c2));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(c2));
				copy.getFlows().put(c2, n_ls);
			}else {
				List<LinkFlow> ls = nonMigrate(c2,st,copy).getFlow();
				put(c2,st,copy,ls);
				copy.getFlows().put(c2, new ArrayList<LinkFlow>());
			}

		}

		return sol;

	}

	protected Response canMigrate(Container vm, Node s, Node t) {
		double c_state = vm.getState() / MIGR_TIME;
		KShortestPaths<Node, LinkStub> kp = new KShortestPaths<Node, LinkStub>(graph, GRASP_CMP_Scheme.k_paths,
				GRASP_CMP_Scheme.maxHops);

		List<GraphPath<Node, LinkStub>> paths = kp.getPaths(s, t);
		List<Double> flows = new ArrayList<Double>();

		for (int i = 0; i < paths.size(); i++) {

			if (c_state <= 0 + GRASP_CMP_Scheme.min_delta) {
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
		boolean can = (c_state <= 0 + GRASP_CMP_Scheme.min_delta) ? true : false;
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
		for (int k = 0; k < paths.size(); k++) {
			if (flows.get(k).doubleValue() == 0)
				continue;
			for (LinkStub lst : paths.get(k).getEdgeList()) {
				lst.setResCapacity(lst.getResCapacity() + flows.get(k).doubleValue());
			}
		}

		return resp;

	}

	protected Response nonMigrate(Container v, ServerStub _s, CMPSolution sol) {
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
			if(s2.intValue() != dc.getPlacement().get(v2).getId()) continue; // IMPORTANTE
			
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

	@Override
	public void setUp(CMPDataCenter dc, Map<Container, Boolean> t, List<ServerStub> stubs,
			DefaultDirectedWeightedGraph<Node, LinkStub> graph, CMPSolution sol) {

		// System.out.println("set up");
		this.dc = dc;
		this.inputTable = t;
		this.stubs_after = stubs;
		this.graph = graph;

		index_one = 0;
		index_two = 0;
		List<Container> toSwap = new ArrayList<Container>();
		for (Container vm : conts) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {

				toSwap.add(vm);
			}

			List<LinkFlow> ls = this.sol.getFlows().get(vm);
			updateLinks(ls,false);
			this.sol.getFlows().remove(vm);

			ls = sol.getFlows().get(vm);

			updateLinks(ls,true);
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>();
			neWls.addAll(ls);
			this.sol.getFlows().put(vm, neWls);
		}

		// remove phase
		for (Container v : toSwap) {
			stubs_after.get(this.sol.getTable().get(v).intValue()).remove(v, stubs, this.sol, dc);
			this.sol.getTable().remove(v);
		} // allocate phase
		for (Container v : toSwap) {
			int tmp = sol.getTable().get(v).intValue();
			stubs_after.get(tmp).forceAllocation(v, stubs_after, this.sol, dc);
			this.sol.getTable().put(v, new Integer(tmp));
		}

		this.sol = (CMPSolution) sol.clone();
		if (conts.size() != sol.getTable().size()) {
			conts = new ArrayList<Container>();
			conts.addAll(sol.getTable().keySet());
		}

		copy = (CMPSolution) this.sol.clone();

	
		Container vm = conts.get(index_one);
		ServerStub st = stubs_after.get(sol.getTable().get(vm).intValue());
		if(st.getId() != dc.getPlacement().get(vm).getId()) {
			togli(vm,st,copy,sol.getFlows().get(vm));
			
			copy.getFlows().remove(vm);
		}else {
			List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
			togli(vm,st,copy,ls);
			copy.getFlows().remove(vm);
		}
		
		deltacurrent = deltaObj(conts.get(index_one),
				stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);
		
	}

	protected Double deltaObj(Container vm, ServerStub e, CMPSolution incumbent, boolean b) {
		double cost = 0;
		boolean allowSamePosition = inputTable.get(vm);

		if (!allowSamePosition && dc.getPlacement().get(vm).getId() == e.getId()) {
			cost = Double.POSITIVE_INFINITY;
			return new Double(cost);
		}

		if (b && !(e.allocate(vm, stubs_after, incumbent, dc, Server.overUtilization_constant, false))) {
			cost = Double.POSITIVE_INFINITY;
			return new Double(cost);
		}

		double t_cost = 0;
		Customer r = Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> lconts = r.getContainers();

		for (Container c : lconts) {
			Server s = dc.getPlacement().get(c);
			Double t1 = r.getTraffic().get(new C_Couple(vm, c));
			Double t2 = r.getTraffic().get(new C_Couple(c, vm));
			if (!(t1 == null))
				t_cost += dc.getCosts()[e.getId()][s.getId()] * t1.doubleValue();
			if (!(t2 == null))
				t_cost += dc.getCosts()[s.getId()][e.getId()] * t2.doubleValue();
		}
		lconts = r.getNewContainers();

		for (Container c : lconts) {
			Integer s = incumbent.getTable().get(c);
			if (!(s == null)) {
				Double t1 = r.getTraffic().get(new C_Couple(vm, c));
				Double t2 = r.getTraffic().get(new C_Couple(c, vm));
				if (!(t1 == null))
					t_cost += dc.getCosts()[e.getId()][s.intValue()] * t1.doubleValue();
				if (!(t2 == null))
					t_cost += dc.getCosts()[s.intValue()][e.getId()] * t2.doubleValue();
			}
		}

		double p_cost = 0;
		p_cost += CPUcalculator.fractionalUtilization(vm, e.getRealServ())
				* (e.getRealServ().getP_max() - e.getRealServ().getP_idle());
		p_cost += (!e.isState()) ? e.getRealServ().getP_idle() : 0;

		double migr_cost = 0;

		if (allowSamePosition) {
			migr_cost -= 1;
			if (dc.getPlacement().get(vm).getId() == e.getId()) {
				migr_cost += 1;
			}
		}
		cost = GRASP_CMP_Scheme.pow_coeff * p_cost + GRASP_CMP_Scheme.traff_coeff * t_cost
				+ GRASP_CMP_Scheme.migr_coeff * migr_cost;
		return new Double(cost);

	}

	@Override
	public void clear() {
		conts = new ArrayList<Container>();

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
					graph.setEdgeWeight(l, (1 / (l.getResCapacity() + inv_offset)));

				}

			}
			
		protected void put(Container v,ServerStub st, CMPSolution copy, List<LinkFlow>ls) {
			
			st.forceAllocation(v,
					stubs_after, copy, dc);

			copy.getTable().put(v,
					new Integer(st.getId()));

			updateLinks(ls,true);
			
		}
		
		protected void togli(Container v,ServerStub st, CMPSolution copy, List<LinkFlow>ls) {
			
			st.remove(v,
					stubs_after, copy, dc);

			copy.getTable().remove(v);

			updateLinks(ls,false);
			
		}
		

}
