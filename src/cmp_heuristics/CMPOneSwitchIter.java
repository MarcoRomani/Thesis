package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import general.Pod;
import general.Rack;
import general.S_Couple;
import general.Server;

public class CMPOneSwitchIter implements CMPNeighborhood {

	public static double inv_offset = GRASP_CMP_Scheme.inv_offset;
	protected CMPSolution sol = new CMPSolution();
	protected CMPDataCenter dc;
	protected List<ServerStub> stubs_after;
	protected int cust_index = 0;
	protected int cont_index = 0;
	protected int serv_index = 0;
	protected List<Container> conts = new ArrayList<Container>();
	protected List<ServerStub> servs = new ArrayList<ServerStub>();
	protected List<Customer> custs = new ArrayList<Customer>();
	protected CMPSolution copy;
	protected Double deltacurrent;
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph;

	public CMPOneSwitchIter() {
		for (Customer c : Customer.custList) {
			if (c.getNewContainers().size() > 0) {
				custs.add(c);
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (cust_index + cont_index + serv_index >= custs.size() + conts.size() + servs.size() - 3) {
			stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()).allocate(conts.get(cont_index),
					stubs_after, copy, dc, true);

			copy.getTable().put(conts.get(cont_index),
					new Integer(sol.getTable().get(conts.get(cont_index)).intValue()));

			List<LinkFlow> ls = sol.getFlows().get(conts.get(cont_index));
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

			}
			ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>();
			n_ls.addAll(ls);
			copy.getFlows().put(conts.get(cont_index), n_ls);
			// copy.setValue(sol.getValue());

			return false;
		}
		return true;
	}

	@Override
	public CMPSolution next() {

		serv_index += 1;
		if (serv_index >= servs.size()) {
			stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()).allocate(conts.get(cont_index),
					stubs_after, copy, dc, true);
			copy.getTable().put(conts.get(cont_index),
					new Integer(sol.getTable().get(conts.get(cont_index)).intValue()));
			List<LinkFlow> ls = sol.getFlows().get(conts.get(cont_index));
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

			}
			ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>();
			n_ls.addAll(ls);
			copy.getFlows().put(conts.get(cont_index), n_ls);
			// copy.setValue(sol.getValue());

			serv_index = 0;
			cont_index += 1;
			if (cont_index < conts.size()) {
				stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index),
						stubs_after, copy, dc);
				copy.getTable().remove(conts.get(cont_index));
				ls = sol.getFlows().get(conts.get(cont_index));
				for (LinkFlow lf : ls) {

					LinkStub l = lf.getLink();
					if (l.getResCapacity() == Double.POSITIVE_INFINITY)
						continue;
					l.setResCapacity(l.getResCapacity() + lf.getFlow());
					graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

				}

				copy.getFlows().remove(conts.get(cont_index));

				deltacurrent = deltaObj(conts.get(cont_index),
						stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()), copy, false);
			}
		}

		if (serv_index >= servs.size())
			return sol;

		Integer tmp = new Integer(servs.get(serv_index).getId());
		Integer tmp2 = sol.getTable().get(conts.get(cont_index));

		if (tmp.intValue() == tmp2.intValue())
			return sol; 

		double value = sol.getValue();

		Double deltanext = deltaObj(conts.get(cont_index), stubs_after.get(tmp.intValue()), copy, true);

		if (deltanext.doubleValue() < deltacurrent.doubleValue()) {

			Server s = stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()).getRealServ();
			Server t = servs.get(serv_index).getRealServ();
			Response resp = null;
			if(s != t) {
				resp = canMigrate(conts.get(cont_index), s, t);
			}else {
				resp = nonMigrate(conts.get(cont_index), stubs_after.get(s.getId()),copy);
			}

			if (resp.getAnswer()) {
				CMPSolution nextSol = (CMPSolution) copy.clone();
				nextSol.getTable().put(conts.get(cont_index), tmp);
				nextSol.getFlows().put(conts.get(cont_index), resp.getFlow());
				nextSol.setValue(value - deltacurrent.doubleValue() + deltanext.doubleValue());

				return nextSol;
			}
		}

		return sol;

	}

	@Override
	public void setUp(CMPDataCenter dc, List<ServerStub> stubs, DefaultDirectedWeightedGraph<Node, LinkStub> graph,
			CMPSolution sol) {
		this.dc = dc;
		this.stubs_after = stubs;
		this.graph = graph;

		cust_index = 0;
		cont_index = 0;
		serv_index = -1;

		ArrayList<Container> toSwitch = new ArrayList<Container>();
		for (Container vm : this.sol.getTable().keySet()) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
				toSwitch.add(vm);

				// System.out.println("da correggere");
			}
			List<LinkFlow> ls = this.sol.getFlows().get(vm);
			for (LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
			this.sol.getFlows().remove(vm);
			ls = sol.getFlows().get(vm);
			for (LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() - inv_offset));
			}
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>();
			neWls.addAll(ls);
			this.sol.getFlows().put(vm, neWls);
		}

		// remove phase
		for (Container v : toSwitch) {
			stubs_after.get(this.sol.getTable().get(v).intValue()).remove(v, stubs_after, this.sol, dc);
			this.sol.getTable().remove(v);

		} // allocate phase
		for (Container v : toSwitch) {
			int tmp = sol.getTable().get(v).intValue();
			stubs_after.get(tmp).forceAllocation(v, stubs_after, this.sol, dc);
			this.sol.getTable().put(v, new Integer(tmp));
		}
		this.sol.setValue(sol.getValue());

		updateCust();

		// PREPARE THE COPY
		copy = (CMPSolution) this.sol.clone();

		stubs_after.get(this.sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index),
				stubs_after, copy, dc);
		copy.getTable().remove(conts.get(cont_index));
		List<LinkFlow> ls = this.sol.getFlows().get(conts.get(cont_index));
		for (LinkFlow lf : ls) {
			LinkStub l = lf.getLink();
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			l.setResCapacity(l.getResCapacity() + lf.getFlow());
			graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
		}
		copy.getFlows().remove(conts.get(cont_index));
		deltacurrent = deltaObj(conts.get(cont_index),
				stubs_after.get(this.sol.getTable().get(conts.get(cont_index)).intValue()), copy, false);

	}

	protected Double deltaObj(Container vm, ServerStub e, CMPSolution incumbent, boolean b) {
		double cost = 0;
		if (b && !(e.allocate(vm, stubs_after, incumbent, dc, false))) {
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
		p_cost += CPUcalculator.fractionalUtilization(vm, e.getRealServ())*(e.getRealServ().getP_max() - e.getRealServ().getP_idle());
		p_cost += (!e.isState()) ? e.getRealServ().getP_idle() : 0;
		
		double migr_cost = 0;
		if (dc.getPlacement().get(vm).getId() == e.getId()) {
			migr_cost = 1;
		}
		
		cost = GRASP_CMP_Scheme.pow_coeff*p_cost + GRASP_CMP_Scheme.traff_coeff*t_cost + GRASP_CMP_Scheme.migr_coeff*migr_cost;
		return new Double(cost);
	}

	protected Response canMigrate(Container vm, Node s, Node t) {
		
	
		if(s == t) { 
			
		}
		
		
		double c_state = vm.getState();
		KShortestPaths<Node, LinkStub> kp = new KShortestPaths<Node, LinkStub>(graph, GRASP_CMP_Scheme.k_paths, GRASP_CMP_Scheme.maxHops);

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
	
	protected Response nonMigrate(Container v, ServerStub _s, CMPSolution sol) {
		Server s = _s.getRealServ();
		Customer r = Customer.custList.get(v.getMy_customer());
		List<Container> conts = r.getContainers();
		List<LinkFlow> flows = new ArrayList<LinkFlow>();
		
		Double c_c0 = r.getTraffic().get(new C_Couple(v,Container.c_0));
		if(c_c0 != null) {
			List<Link> path = dc.getTo_wan().get(s);
			for(Link l : path) {
				LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
				lstub.setResCapacity(lstub.getResCapacity() - c_c0.doubleValue());		
				LinkFlow f = new LinkFlow(lstub, c_c0.doubleValue());
				flows.add(f);
			}
		}
		Double c0_c = r.getTraffic().get(new C_Couple(Container.c_0,v));
		if(c0_c != null){
			List<Link> path = dc.getFrom_wan().get(s);
			for(Link l : path) {
				LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
				lstub.setResCapacity(lstub.getResCapacity() - c0_c.doubleValue());		
				LinkFlow f = new LinkFlow(lstub, c0_c.doubleValue());
				flows.add(f);
			}
		}
		
		for(Container v2 : conts) {
			Double t1 = r.getTraffic().get(new C_Couple(v,v2));				
			Double t2 =  r.getTraffic().get(new C_Couple(v2,v));
			Server s2 = dc.getPlacement().get(v2);
			if(t1 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s,s2));
				for(Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
					lstub.setResCapacity(lstub.getResCapacity() - t1.doubleValue());			
					LinkFlow f = new LinkFlow(lstub, t1.doubleValue());
					flows.add(f);
				}
			}
			if(t2 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s2,s));
				for(Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
					lstub.setResCapacity(lstub.getResCapacity() - t2.doubleValue()); 
					LinkFlow f = new LinkFlow(lstub, t2.doubleValue());
					flows.add(f);
				}
			}
			
		}
		conts = r.getNewContainers();
		for(Container v2 : conts) {
			Integer s2 = sol.getTable().get(v2);
			if(s2 == null) continue;
			
			Double t1 = r.getTraffic().get(new C_Couple(v,v2));				
			Double t2 =  r.getTraffic().get(new C_Couple(v2,v));
			if(t1 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(s,stubs_after.get(s2).getRealServ()));
				for(Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
					lstub.setResCapacity(lstub.getResCapacity() - t1.doubleValue());		
					LinkFlow f = new LinkFlow(lstub, t1.doubleValue());
					flows.add(f);
				}
			}
			if(t2 != null) {
				List<Link> path = dc.getPaths().get(new S_Couple(stubs_after.get(s2).getRealServ(),s));
				for(Link l : path) {
					LinkStub lstub = graph.getEdge(l.getMySource(), l.getMyTarget());
					lstub.setResCapacity(lstub.getResCapacity() - t2.doubleValue());	
					LinkFlow f = new LinkFlow(lstub, t2.doubleValue());
					flows.add(f);
				}
			}
			
		}
		
		boolean can = true;
		for(LinkFlow lf : flows) {
			if(lf.getLink().getResCapacity() < 0){can = false;}
			lf.getLink().setResCapacity(lf.getLink().getResCapacity() + lf.getFlow());
		}
		
		return new Response(can, flows);
		
	
	}

	protected void updateCust() {
		servs.clear();
		conts = custs.get(cust_index).getNewContainers();
		Set<Integer> c_serv = new TreeSet<Integer>();
		for (Container ct : conts) {
			c_serv.add(this.sol.getTable().get(ct));
		}
		for (Container ct : custs.get(cust_index).getContainers()) {
			c_serv.add(new Integer(this.dc.getPlacement().get(ct).getId()));
		}

		Set<Pod> c_pods = new TreeSet<Pod>();
		boolean flag = false;
		for (Integer sv : c_serv) {
			flag = false;
			for (Pod p : dc.getPods()) {
				if (flag == true)
					break;
				if (p.containsServer(sv.intValue())) {
					c_pods.add(p);
					flag = true;
				}
			}
		}

		for (Pod p : c_pods) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {

					if (s.isUnderUtilized()) {
						servs.add(stubs_after.get(s.getId()));
					}
				}
			}
		}
		
	}

	
	@Override
	public void clear() {
		conts = new ArrayList<Container>();
		servs = new ArrayList<ServerStub>();
		this.sol.getTable().clear();
		this.sol.getFlows().clear();
		this.sol.setValue(Double.POSITIVE_INFINITY);
	}

}
