package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.CPPSolution;
import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Node;

public class CMPOneSwapIter implements CMPNeighborhood {

	public static double inv_offset = GRASP_CMP_Scheme.inv_offset;
	protected CMPDataCenter dc;
	protected CMPSolution sol = new CMPSolution();
	protected int index_one = 0;
	protected int index_two = 0;
	protected List<ServerStub> stubs_after;
	protected List<Container> conts = new ArrayList<Container>();
	protected CMPSolution copy;
	protected Double deltacurrent;
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph;

	@Override
	public boolean hasNext() {
		if (index_one + index_two >= 2 * conts.size() - 3) {
			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one),
					stubs_after, copy, dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));

			List<LinkFlow> ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
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
	public CMPSolution next() {
		index_two += 1;
		if (index_two >= conts.size()) {
			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one),
					stubs_after, copy, dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));
			List<LinkFlow> ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
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

			if (index_one >= conts.size()) {
				throw new NoSuchElementException();
			}

			stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one),
					stubs_after, copy, dc);
			copy.getTable().remove(conts.get(index_one));

			ls = sol.getFlows().get(conts.get(index_one));
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
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

	protected CMPSolution generateSolution() {
		Container c1 = conts.get(index_one);
		Container c2 = conts.get(index_two);
		Integer s1 = sol.getTable().get(c1);
		Integer s2 = sol.getTable().get(c2);

		if (c1 == c2 || s1.intValue() == s2.intValue()) {
			return sol;
		}

		stubs_after.get(s2.intValue()).remove(c2, stubs_after, copy, dc);
		copy.getTable().remove(c2);
		List<LinkFlow> ls = sol.getFlows().get(c2);
		for (LinkFlow lf : ls) {

			LinkStub l = lf.getLink();
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			l.setResCapacity(l.getResCapacity() + lf.getFlow());
			graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));

		}

		copy.getFlows().remove(c2);

		Double deltacurrent_2 = deltaObj(c2, stubs_after.get(s2.intValue()), copy, false);
		Double deltanext_2 = deltaObj(c2, stubs_after.get(s1.intValue()), copy, true);
		if (deltanext_2.doubleValue() < Double.POSITIVE_INFINITY) {
			stubs_after.get(s1.intValue()).allocate(c2, stubs_after, copy, dc, true);
			copy.getTable().put(c2, s1);

			// CAN MIGRATE c2 IN S1
			Response resp2 = canMigrate(c2, dc.getPlacement().get(c2), stubs_after.get(s1.intValue()).getRealServ());

			ls = resp2.getFlow();
			for (LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}

			Double deltanext = deltaObj(c1, stubs_after.get(s2.intValue()), copy, true);
			// CAN MIGRATE c1 in S2
			Response resp1 = canMigrate(c1, dc.getPlacement().get(c1), stubs_after.get(s2.intValue()).getRealServ());

			stubs_after.get(s1.intValue()).remove(c2, stubs_after, copy, dc);
			copy.getTable().remove(c2);
			for (LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}

			stubs_after.get(s2.intValue()).allocate(c2, stubs_after, copy, dc, true);
			copy.getTable().put(c2, s2);

			ls = sol.getFlows().get(c2);
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>();
			neWls.addAll(ls);
			this.sol.getFlows().put(c2, neWls);

			if (resp1.getAnswer() && resp2.getAnswer() && deltanext.doubleValue() + deltanext_2.doubleValue() < deltacurrent.doubleValue()
					+ deltacurrent_2.doubleValue()) {
				CMPSolution nextSol = (CMPSolution) copy.clone();
				nextSol.getTable().remove(c2);
				nextSol.getFlows().remove(c2);
				nextSol.getTable().put(c1, s2);
				nextSol.getFlows().put(c1, resp1.getFlow());
				nextSol.getTable().put(c2, s1);
				nextSol.getFlows().put(c2, resp2.getFlow());
				nextSol.setValue(sol.getValue() - deltacurrent.doubleValue() - deltacurrent_2.doubleValue()
						+ deltanext.doubleValue() + deltanext_2.doubleValue());
				return nextSol;
			}
		} else {

			stubs_after.get(s2.intValue()).allocate(c2, stubs_after, copy, dc, true);
			copy.getTable().put(c2, s2);
			ls = sol.getFlows().get(c2);
			for (LinkFlow lf : ls) {

				LinkStub l = lf.getLink();
				if (l.getResCapacity() == Double.POSITIVE_INFINITY)
					continue;
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
			}
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>();
			neWls.addAll(ls);
			this.sol.getFlows().put(c2, neWls);

		}

		return sol;

	}

	protected Response canMigrate(Container vm, Node s, Node t) {

		return null;
	}

	@Override
	public void setUp(CMPDataCenter dc, List<ServerStub> stubs, DefaultDirectedWeightedGraph<Node, LinkStub> graph,
			CMPSolution sol) {

		// System.out.println("set up");
		this.dc = dc;
		this.stubs_after = stubs;

		index_one = 0;
		index_two = 0;
		List<Container> toSwap = new ArrayList<Container>();
		for (Container vm : conts) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {

				toSwap.add(vm);
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
		for (Container v : toSwap) {
			stubs_after.get(this.sol.getTable().get(v).intValue()).remove(v, stubs, this.sol, dc);
			this.sol.getTable().remove(v);
		} // allocate phase
		for (Container v : toSwap) {
			int tmp = sol.getTable().get(v).intValue();
			stubs_after.get(tmp).forceAllocation(v, stubs_after, this.sol, dc);
			this.sol.getTable().put(v, new Integer(tmp));
		}

		if (conts.size() != sol.getTable().size()) {
			conts = new ArrayList<Container>();
			conts.addAll(sol.getTable().keySet());
		}

		copy = (CMPSolution) this.sol.clone();

		stubs_after.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs_after,
				copy, dc);
		copy.getTable().remove(conts.get(index_one));
		List<LinkFlow> ls = this.sol.getFlows().get(conts.get(index_one));
		for (LinkFlow lf : ls) {
			LinkStub l = lf.getLink();
			if (l.getResCapacity() == Double.POSITIVE_INFINITY)
				continue;
			l.setResCapacity(l.getResCapacity() + lf.getFlow());
			graph.setEdgeWeight(l, 1 / (l.getResCapacity() + inv_offset));
		}
		copy.getFlows().remove(conts.get(index_one));
		deltacurrent = deltaObj(conts.get(index_one),
				stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);
	}

	protected Double deltaObj(Container container, ServerStub serverStub, CMPSolution copy2, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		conts = new ArrayList<Container>();

	}

}
