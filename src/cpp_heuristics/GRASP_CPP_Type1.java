package cpp_heuristics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import general.*;

/**
 * 
 * @author Marco grasp configuration: initial solution created by placing new
 *         customers first, than old customers, both in timestamp order;
 *         incremental cost used for creating initial solution is the partial
 *         objective function. All is done in timestamp order.
 * 
 *         Placing of new customers is done by selecting one of the closest
 *         racks in terms of residual ram w.r.t the customers' containers total
 *         ram, and than placing containers one by one in the servers. At each
 *         iteration the container with the most traffic towards containers
 *         already placed in the current server is selected.
 */

public class GRASP_CPP_Type1 extends GRASP_CPP_Scheme {

	

	public GRASP_CPP_Type1(DataCenter dc) {
		wrapper = new SolutionWrapper(); // default wrapper

		comp = new ContainerRAMComparator();
		this.dc = dc;
		stubs = new ArrayList<ServerStub>();
		stubs_u = new ArrayList<ServerStub>();

		for (Customer c : Customer.custList) {
			if (c.getContainers().size() == 0) {
				newcust.add(c);
			} else {
				req.add(c);
			}
		}

		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					ServerStub tmp = new ServerStub(s);
					stubs.add(tmp);
					if (s.isUnderUtilized()) {
						stubs_u.add(tmp);
					}
				}
			}
		}
	}

	public GRASP_CPP_Type1(DataCenter dc, List<CPPNeighborhood> neighs) {

		this(dc);
		setNeighborhoods(neighs);

	}

	@Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {

		CPPSolution sol = new CPPSolution();

		Result result = allnew_constr(alfa, sol);
		// System.out.println("new cust done \n");
		sol = result.getSol();
		ArrayList<Container> toPlace = result.getRest();
		for (Customer c : req) {
			toPlace.addAll(c.getNewContainers());
		}

		ArrayList<Container> toPlaceRandom = new ArrayList<Container>();
		while (toPlace.size() > 0) {
			Container tmp = toPlace.remove(rng.nextInt(toPlace.size()));
			toPlaceRandom.add(tmp);
		}
		sol = notnew_constr(toPlaceRandom, alfa, sol);
		// System.out.println("other cust done");
		return sol;

	}

	/**
	 * 
	 * @param toPlace
	 * @param alfa
	 * @param incumbent
	 * @return
	 * @throws InfeasibilityException
	 */

	protected CPPSolution notnew_constr(List<Container> toPlace, float alfa, CPPSolution incumbent)
			throws InfeasibilityException {

		List<ServerStub> E = stubs_u;

		List<Container> vms = toPlace;

		ArrayList<Double> costs = new ArrayList<Double>();

		while (!(vms.size() == 0)) {
			costs.clear();

			for (ServerStub e : E) {
				costs.add(this.incrementalCost(vms.get(0), e, incumbent));
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
				throw new InfeasibilityException(incumbent);

			ArrayList<ServerStub> RCL = new ArrayList<ServerStub>();
			for (int i = 0; i < costs.size(); i++) {
				if (costs.get(i).doubleValue() <= c_min + alfa * (c_max - c_min)) {
					RCL.add(E.get(i));
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

	/**
	 * 
	 * @param incumbent
	 * @param alfa
	 * 
	 * @return
	 */

	protected Result allnew_constr(float alfa, CPPSolution incumbent) {

		// SecureRandom rng = new SecureRandom();

		CPPSolution sol = incumbent;
		ArrayList<Container> rest = new ArrayList<Container>();

		ArrayList<Rack> racks = new ArrayList<Rack>();
		for (Pod p : dc.getPods()) {
			racks.addAll(p.getRacks());
		}

		// for each rack calculate its residual capacity
		ArrayList<Double> estCap = new ArrayList<Double>();

		for (Rack r : racks) {

			estCap.add(computeCapacity(r));

		}

		ArrayList<Double> c_req = new ArrayList<Double>();
		for (Customer c : newcust) {
			c_req.add(computeRequirement(c));
		}

		ArrayList<Container> vms = new ArrayList<Container>();
		ArrayList<Double> costs = new ArrayList<Double>();
		ArrayList<Rack> RCL = new ArrayList<Rack>();
		double c_min = Double.POSITIVE_INFINITY;
		double c_max = 0;

		for (int j = 0; j < newcust.size(); j++) {
			
			Customer c = newcust.get(j);
			costs.clear();
			RCL.clear();
			vms.clear();
			vms.addAll(c.getNewContainers());
			c_min = Float.POSITIVE_INFINITY;
			c_max = 0;

			// incremental costs of racks, based on residual capacity
			for (int i = 0; i < racks.size(); i++) {
				if (estCap.get(i).doubleValue() < c_req.get(j)) {
					costs.add(new Double(0 + (c_req.get(j) - estCap.get(i)))); // big M
				} else
					costs.add(new Double(estCap.get(i) - c_req.get(j)));

				if (costs.get(i).doubleValue() < c_min)
					c_min = costs.get(i).doubleValue();
				if (costs.get(i).doubleValue() > c_max)
					c_max = costs.get(i).doubleValue();
			}

			// build RCL of racks
			for (int i = 0; i < racks.size(); i++) {
				if (costs.get(i) <= c_min + alfa * (c_max - c_min)) {
					RCL.add(racks.get(i));
				}
			}

			// pick one rack at random from RCL
			Rack r = RCL.get(rng.nextInt(RCL.size()));
			ArrayList<ServerStub> substub = new ArrayList<ServerStub>();

			for (Server s : r.getHosts()) {
				if (s.isUnderUtilized()) {
					substub.add(stubs.get(s.getId()));
				}
			}
			if (substub.isEmpty()) {
				rest.addAll(vms);
				continue; // skip to next customer
			}

			Comparator<ServerStub> comp = new StubRamComparator();
			substub.sort(comp); // descending

			int n = 0; // index of ordered servers within the rack
			ArrayList<Double> profit = new ArrayList<Double>();
			ArrayList<Container> here = new ArrayList<Container>();

			while (vms.size() > 0 && n < substub.size()) {
				
				profit.clear();
				double max_profit = Double.NEGATIVE_INFINITY;
				double min_profit = Double.POSITIVE_INFINITY;
				for (Container v : vms) {
					if (!(substub.get(n).allocate(v, stubs, sol, dc, false))) {
						profit.add(Double.NEGATIVE_INFINITY);
						continue;
					}
					double pr = 0;
					for (Container h : here) {
						pr += (c.getTraffic().get(new C_Couple(v, h)) == null) ? 0
								: c.getTraffic().get(new C_Couple(v, h)).doubleValue();
						pr += (c.getTraffic().get(new C_Couple(h, v)) == null) ? 0
								: c.getTraffic().get(new C_Couple(h, v)).doubleValue();
					}
					profit.add(new Double(pr));
					if (pr < Double.POSITIVE_INFINITY && pr > max_profit )
						max_profit = pr;
					if (pr >= 0 && pr < min_profit)
						min_profit = pr;
				}

				ArrayList<Container> inner_RCL = new ArrayList<Container>();
				for (int i = 0; i < profit.size(); i++) {
					if (profit.get(i).doubleValue() >= max_profit - alfa * (max_profit - min_profit)) {
						inner_RCL.add(vms.get(i));
					} else {
						if (max_profit == Double.NEGATIVE_INFINITY || min_profit == Double.POSITIVE_INFINITY) {
							inner_RCL.add(vms.get(i));
						}
					}
				}
				int max = rng.nextInt(inner_RCL.size());
				if (profit.get(max).doubleValue() >= 0 && profit.get(max) < Double.POSITIVE_INFINITY) {
					substub.get(n).forceAllocation(vms.get(max), stubs, sol, dc);
					sol.getTable().put(vms.get(max), new Integer(substub.get(n).getId()));
					here.add(vms.get(max));
					vms.remove(vms.get(max));
				} else {
					n++;
					here.clear();
				}

			
			}

			estCap.set(r.getId(), computeCapacity(r));
			rest.addAll(vms);
			
		}
		return new Result(sol, rest);
	}

	protected Double computeCapacity(Rack r) {

		double tmp = 0;
		for (Server s : r.getHosts()) {
			if (s.isUnderUtilized())
				tmp += stubs.get(s.getId()).getRes_mem();
		}
		return new Double(tmp);
	}

	protected Double computeRequirement(Customer c) {
		double tmp = 0;
		for (Container v : c.getNewContainers()) {
			tmp += v.getMem();
		}
		return new Double(tmp);
	}

	@Override
	protected Double incrementalCost(Container vm, ServerStub e, CPPSolution incumbent) {
		double cost = 0;

		if (!(e.allocate(vm, stubs, incumbent, dc, false))) {
			cost = Double.POSITIVE_INFINITY;
			return new Double(cost);
		}

		Customer r = Customer.custList.get(vm.getMy_customer());
		ArrayList<Container> conts = r.getContainers();

		for (Container c : conts) {
			Server s = dc.getPlacement().get(c);
			Double t1 = r.getTraffic().get(new C_Couple(vm, c));
			Double t2 = r.getTraffic().get(new C_Couple(c, vm));
			if (!(t1 == null))
				cost += dc.getCosts()[e.getId()][s.getId()] * t1.doubleValue();
			if (!(t2 == null))
				cost += dc.getCosts()[s.getId()][e.getId()] * t2.doubleValue();
		}
		conts = r.getNewContainers();

		for (Container c : conts) {
			Integer s = incumbent.getTable().get(c);
			if (!(s == null)) {
				Double t1 = r.getTraffic().get(new C_Couple(vm, c));
				Double t2 = r.getTraffic().get(new C_Couple(c, vm));
				if (!(t1 == null))
					cost += dc.getCosts()[e.getId()][s.intValue()] * t1.doubleValue();
				if (!(t2 == null))
					cost += dc.getCosts()[s.intValue()][e.getId()] * t2.doubleValue();
			}
		}

		return new Double(cost);
	}

	@Override
	protected void changeNeighborhood() {
		neigh_index++;
		if (neigh_index >= neighborhoods.size()) {
			neigh_index = 0;
		}
		neighborhood_explorer = neighborhoods.get(neigh_index);
	}

	@Override
	protected void reset(CPPSolution solution) {

		super.reset(solution);

		// ---- RESET NEIGHBORHOODS ------
		neigh_index = 0;
		neighborhood_explorer = neighborhoods.get(neigh_index);
		for (CPPNeighborhood n : neighborhoods) {
			n.clear();
		}
	}

	@Override
	public void setNeighborhoods(List<CPPNeighborhood> neighs) {
		this.neighborhoods.addAll(neighs);
		neigh_index = 0;
		this.neighborhood_explorer = neighs.get(neigh_index);
	}

}
