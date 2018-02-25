package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Customer;
import general.Node;
import general.Pod;
import general.Rack;
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
			Response resp = canMigrate(conts.get(cont_index), s, t);

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
		this.sol.getFlows().remove(conts.get(cont_index));
		deltacurrent = deltaObj(conts.get(cont_index),
				stubs_after.get(this.sol.getTable().get(conts.get(cont_index)).intValue()), copy, false);

	}

	protected Double deltaObj(Container container, ServerStub serverStub, CMPSolution copy2, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	protected Response canMigrate(Container vm, Node s, Node t) {

		return null;
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
