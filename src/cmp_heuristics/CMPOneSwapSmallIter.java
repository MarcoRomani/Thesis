package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Customer;
import general.Node;

public class CMPOneSwapSmallIter extends CMPOneSwapIter {

	protected List<Customer> custs = new ArrayList<Customer>();
	protected int cust_index;
	protected List<Container> allconts = new ArrayList<Container>();

	public CMPOneSwapSmallIter() {
		for (Customer c : Customer.custList) {
			if (c.getNewContainers().size() > 1) {
				custs.add(c);
			}

		}
	}

	@Override
	public boolean hasNext() {
		if (cust_index + index_one + index_two >= custs.size() + 2 * conts.size() - 4) {
			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one),
					stubs_after, copy, dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));

			List<LinkFlow> ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

			}
			ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>();
			n_ls.addAll(ls);
			copy.getFlows().put(conts.get(index_one), n_ls);
			return false;
		}
		return true;
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
		for (Container vm : allconts) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {

				toSwap.add(vm);
			}

			List<LinkFlow> ls = this.sol.getFlows().get(vm);
			for (LinkFlow lf : ls) {
				LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
			this.sol.getFlows().remove(vm);

			ls = sol.getFlows().get(vm);
			
			for (LinkFlow lf : ls) {
				LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
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
		if (allconts.size() != sol.getTable().size()) {
			allconts = new ArrayList<Container>();
			allconts.addAll(sol.getTable().keySet());
		}

		cust_index = 0;
		conts = custs.get(cust_index).getNewContainers();
		copy = (CMPSolution) this.sol.clone();

		stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs_after,
				copy, dc);
		copy.getTable().remove(conts.get(index_one));
		List<LinkFlow> ls = this.sol.getFlows().get(conts.get(index_one));
		for (LinkFlow lf : ls) {
			LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			l.setResCapacity(l.getResCapacity() + lf.getFlow());
			graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
		}
		copy.getFlows().remove(conts.get(index_one));
		deltacurrent = deltaObj(conts.get(index_one),
				stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);
	}

	@Override
	public CMPSolution next() {
		index_two += 1;
		if (index_two >= conts.size()) {
			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one),
					stubs_after, copy, dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));
			List<LinkFlow> ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

			}
			ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>();
			n_ls.addAll(ls);
			copy.getFlows().put(conts.get(index_one), n_ls);

			index_one += 1;
			index_two = index_one + 1;

			if (index_one >= conts.size() - 1) {
				cust_index++;
				index_one = 0;
				index_two = 1;
				updateCustomer();
			}

			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one),
					stubs_after, copy, dc);
			copy.getTable().remove(conts.get(index_one));
			ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = graph.getEdge(lf.getLink().getMySource(), lf.getLink().getMyTarget());
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

			}

			copy.getFlows().remove(conts.get(index_one));
			deltacurrent = deltaObj(conts.get(index_one),
					stubs_after.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);

		}

		return generateSolution();
	}

	protected void updateCustomer() {
		conts = custs.get(cust_index).getNewContainers();

	}

	@Override
	public void clear() {
		super.clear();
		allconts.clear();

	}
}
