package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;
import general.DataCenter;

public class GRASP_CPP_Type1Indexing extends GRASP_CPP_Type1 {

	public GRASP_CPP_Type1Indexing(DataCenter dc, List<CPPNeighborhood> neighs) {
		super(dc, neighs);

	}

	public GRASP_CPP_Type1Indexing(DataCenter dc) {
		super(dc);

	}

	@Override
	protected CPPSolution notnew_constr(List<Container> toPlace, float alfa, CPPSolution incumbent)
			throws InfeasibilityException {

		if (tree == null) {
			return super.notnew_constr(toPlace, alfa, incumbent);
		} else {

			List<Container> vms = toPlace;

			ArrayList<Double> costs = new ArrayList<Double>();
			ArrayList<Integer> E = new ArrayList<Integer>();
			TreeNodeExplorer iter = new TreeNodeExplorer(tree);

			while (!(vms.size() == 0)) {
				costs.clear();
				E.clear();

				iter.setStart(vms.get(0).getMem());
				while (iter.hasNext()) {
					int tmp = iter.next();
					E.add(new Integer(tmp));
					costs.add(this.incrementalCost(vms.get(0), stubs.get(tmp), incumbent));
				}

				double c_min = Double.POSITIVE_INFINITY;
				double c_max = 0;
				for (Double ce : costs) {
					if (ce.doubleValue() < c_min) {
						c_min = ce.doubleValue();
					}
					if (ce.doubleValue() < Double.POSITIVE_INFINITY && ce.doubleValue() > c_max) {
						c_max = ce.doubleValue();
					}
				}

				if (c_min == Double.POSITIVE_INFINITY)
					throw new InfeasibilityException();

				ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();
				for (int i = 0; i < costs.size(); i++) {
					if (costs.get(i).doubleValue() <= c_min + alfa * (c_max - c_min)) {
						RCL.add(stubs.get(E.get(i)));
					}
				}
				// System.out.println("RCL size = "+RCL.size());

				ServerStub candidate = RCL.get(rng.nextInt(RCL.size()));
				if (!(candidate.allocate(vms.get(0), stubs, incumbent, dc, true))) {
					continue;
				}
				incumbent.getTable().put(vms.get(0), candidate.getId()); //
				vms.remove(0);

			}

			return incumbent;

		}

	}
}
