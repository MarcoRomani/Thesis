package cpp_heuristics;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;
import stCPP.Main;

/**
 * 
 * @author Marco Template of the whole GRASP + local search for the cpp.
 *         Multiple neighborhoods can be specified for the local search.
 * 
 */
public abstract class GRASP_CPP_Scheme {

	public static double min_delta = 0.000001;
	protected SecureRandom rng =  new SecureRandom();
	protected CPPNeighborhood neighborhood_explorer;
	protected List<CPPNeighborhood> neighborhoods = new ArrayList<CPPNeighborhood>();
	protected int neigh_index = 0;
	protected DataCenter dc;
	protected List<Customer> req = new ArrayList<Customer>();
	protected List<Customer> newcust = new ArrayList<Customer>();
	protected List<ServerStub> stubs;
	protected List<ServerStub> stubs_u;
	protected SolutionWrapper wrapper;
	protected TreeIndex tree;
	protected Comparator<Container> comp;
	protected CPPSolution best;
	Date d1;
	protected long timelimit=Long.MAX_VALUE;
	
	// ----- ABSTRACT METHODS --------
	protected abstract CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException;

	protected abstract Double incrementalCost(Container vm, ServerStub e, CPPSolution incumbent);

	protected abstract void changeNeighborhood();

	// -------- OTHER METHODS ---------

	public CPPSolution grasp(String option, int param, float alfa, int seed) {
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		rng.setSeed((BigInteger.valueOf(seed).toByteArray()));

		

		switch(option) {
		case "time":
			return graspTime(param,alfa);
		case "maxIter":
			return graspIter(param,alfa);
		default: 
			return graspIter(param, alfa);
		}
		
	}

	public CPPSolution graspIter(int maxIter, float alfa) {

		best = new CPPSolution();

	d1 = new Date();
		int i=0;
		for ( ; i < maxIter; i++) {
			if(Main.display) {
				   System.out.println("iter:"+(i));
			}

			grasp(alfa);
		}

		Date d2 = new Date();
		wrapper.updateSolutions(best);
		wrapper.updateIterations(i);
		wrapper.updateTime(d2.getTime()-d1.getTime());
		synchronized (wrapper) {
			wrapper.notifyAll();
		}
		return best;
	}

	// time in seconds
	public CPPSolution graspTime(int time, float alfa) {
		int my_time = time*1000;
		timelimit = my_time;
		best = new CPPSolution();
		
		 d1 = new Date();
		Date d2 = new Date();
		int iter =0;
		do {
			iter += 1;
			if(Main.display) {
			   System.out.println("iter:"+(iter));
			}
			
			grasp(alfa);
		    d2 = new Date();
		    
		}while(d2.getTime()-d1.getTime() < my_time);
		
		wrapper.updateSolutions(best);
		wrapper.updateIterations(iter);
		wrapper.updateTime(d2.getTime()-d1.getTime());
		synchronized (wrapper) {
			wrapper.notifyAll();
		}
		return best;
		
	}
	protected void grasp(float alfa) {

		CPPSolution incumbent = new CPPSolution();

		// ------ GENERATE INITIAL SOLUTION ----------
		try {
			incumbent = this.greedy_rand_construction(alfa);
		} catch (InfeasibilityException e) {
			if(Main.display) {
			    System.out.println("infeasible");
			}
			reset(e.getSolution());
			return;
		}

		evaluate(incumbent);		
		wrapper.updateInit(incumbent);
		if(incumbent.getValue() == Double.POSITIVE_INFINITY) {
			reset(incumbent);
			return;
		}
		// System.out.println(incumbent.toString());

		// -------- LOCAL SEARCH WITH MULTI-NEIGHBORHOODS --------------

		int count = 0;
		neigh_index = 0;
		neighborhood_explorer = neighborhoods.get(neigh_index);
		do {			
			CPPSolution newincumbent = localSearch(incumbent);
			if (!(newincumbent.getValue() < incumbent.getValue() /*- min_delta*/)) {
				count++;
			} else {
				count = 0;
				incumbent = newincumbent;
			}
			
			changeNeighborhood();
		} while (count < neighborhoods.size() && neighborhoods.size() > 1);

		// --------- UPDATE BEST SOLUTION AMONG ITERATIONS ------------
		if (incumbent.getValue() < best.getValue()) {
			best = (CPPSolution) incumbent.clone();
			
		}

		// --------- PREPARE FOR NEXT ITERATION ----------------------
		reset(incumbent);

	}

	protected void reset(CPPSolution solution) {
		CPPSolution my_sol = solution;

		ArrayList<Container> toRemove = new ArrayList<Container>();
		toRemove.addAll(my_sol.getTable().keySet());
		for (Container vm : toRemove) {
			ServerStub tmp = stubs.get(my_sol.getTable().get(vm).intValue());
			tmp.remove(vm, stubs, my_sol, dc);
			my_sol.getTable().remove(vm);
		}
		/*
		 * for(ServerStub s: stubs) { if(s.getRes_out() !=
		 * s.getRealServ().getResidual_bdw_out()) {
		 * System.out.println("something's wrong: "+s.getRes_out()+" , "+s.getRealServ()
		 * .getResidual_bdw_out()+" containers="+s.getContainers());
		 * 
		 * } }
		 */
	}

	public void setIndexing(TreeIndex t) {
		tree = t;
	}

	protected CPPSolution localSearch(CPPSolution init_sol) {

	//	 System.out.println("start local search");
		if(new Date().getTime() - d1.getTime() > timelimit) return init_sol;
		
		CPPSolution sol = (CPPSolution) init_sol.clone();
		evaluate(sol);

		CPPSolution best_neighbor = sol;

		

		do {
			// System.out.println("Try new neighborhood");
			sol = best_neighbor;
		
			neighborhood_explorer.setUp(dc, stubs, best_neighbor);
			
			while (neighborhood_explorer.hasNext()) {
				// System.out.println("next");
				CPPSolution current = neighborhood_explorer.next();
				if (evaluate(current) < best_neighbor.getValue() - min_delta) {
					best_neighbor = current;
					wrapper.updateBests(best_neighbor);
		if(Main.display)		 System.out.println("new best neighbor found "+best_neighbor.getValue());
				}

			}

		} while (sol.getValue() != best_neighbor.getValue() && new Date().getTime() - d1.getTime() < timelimit);

		neighborhood_explorer.clear();
		// System.out.println("end local search");
		
		// System.out.println(sol.toString());
		return sol;
	}

	protected double evaluate(CPPSolution sol) {

		if (sol.getValue() < Double.POSITIVE_INFINITY)
			return sol.getValue(); // lazy
		if (!checkFeasibility(sol)) {
			sol.setValue(Double.POSITIVE_INFINITY);
			return sol.getValue();
		}
		double value = 0;
		List<Customer> custs = Customer.custList;

		for (Customer c : custs) {
			List<Container> conts = c.getContainers();
			List<Container> newconts = c.getNewContainers();
			// old-new and new-old
			for (Container c1 : conts) {
				int s1 = dc.getPlacement().get(c1).getId();
				for (Container c2 : newconts) {
					int s2 = sol.getTable().get(c2).intValue();
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue() * dc.getCosts()[s1][s2];
					}
					if (c.getTraffic().get(new C_Couple(c2, c1)) != null) {
						value += c.getTraffic().get(new C_Couple(c2, c1)).doubleValue() * dc.getCosts()[s2][s1];
					}
				}
			}
			// new-new
			for (Container c1 : newconts) {
				int s1 = sol.getTable().get(c1).intValue();
				for (Container c2 : newconts) {
					if (c.getTraffic().get(new C_Couple(c1, c2)) != null) {
						value += c.getTraffic().get(new C_Couple(c1, c2)).doubleValue()
								* dc.getCosts()[s1][sol.getTable().get(c2).intValue()];
					}
				}
			}

		}
		sol.setValue(value);
		return value;
	}

	protected boolean checkFeasibility(CPPSolution incumbent) {

		List<Container> tmp2 = new ArrayList<Container>();

		for (Customer c : req) {
			tmp2.addAll(c.getNewContainers());
		}
		for (Customer c : newcust) {
			tmp2.addAll(c.getNewContainers());
		}

		if (tmp2.size() != incumbent.getTable().size())
			return false;

		List<Server> servers = new ArrayList<Server>();
		for (Pod p : dc.getPods()) {
			for (Rack r : p.getRacks()) {
				for (Server s : r.getHosts()) {
					servers.add(s);
				}
			}
		}

		double[] usedBDWout = new double[servers.size()];
		double[] usedBDWin = new double[servers.size()];
		double[] usedCPU = new double[servers.size()];
		double[] usedRAM = new double[servers.size()];
		double[] usedDISK = new double[servers.size()];

		for (int i = 0; i < servers.size(); i++) {
			List<Container> tmp = servers.get(i).getContainers();
			for (Container c1 : tmp) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				for (Container c2 : r.getNewContainers()) {
					if (!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId())) {
						usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
								: r.getTraffic().get(new C_Couple(c1, c2)).doubleValue();
						usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
								: r.getTraffic().get(new C_Couple(c2, c1)).doubleValue();
					}
				}
			}
		}

		for (Container c1 : tmp2) {
			Customer r = Customer.custList.get(c1.getMy_customer());
			int i = incumbent.getTable().get(c1).intValue();
			usedCPU[i] += CPUcalculator.utilization(c1, servers.get(i)); //* ((float) 2500 / servers.get(i).getFrequency());
			usedRAM[i] += c1.getMem();
			usedDISK[i] += c1.getDisk();
			usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, Container.c_0)) == null) ? 0
					: r.getTraffic().get(new C_Couple(c1, Container.c_0)).doubleValue();
			usedBDWin[i] += (r.getTraffic().get(new C_Couple(Container.c_0, c1)) == null) ? 0
					: r.getTraffic().get(new C_Couple(c1, Container.c_0)).doubleValue();
			for (Container c2 : r.getContainers()) {
				if (!(dc.getPlacement().get(c2).getId() == servers.get(i).getId())) {
					usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c1, c2)).doubleValue();
					usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c2, c1)).doubleValue();
				}
			}

			for (Container c2 : r.getNewContainers()) {
				if (!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId())) {
					usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c1, c2)).doubleValue();
					usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c2, c1)).doubleValue();
				}
			}
		}

		for (int i = 0; i < servers.size(); i++) {
			if (servers.get(i).getResidual_bdw_out() - usedBDWout[i] < 0)
				return false;
			if (servers.get(i).getResidual_bdw_in() - usedBDWin[i] < 0)
				return false;
			if (servers.get(i).getResidual_cpu() - usedCPU[i] < 0)
				return false;
			if (servers.get(i).getResidual_mem() - usedRAM[i] < 0)
				return false;
			if (servers.get(i).getResidual_disk() - usedDISK[i] < 0)
				return false;
		}

		return true;
	}

	public void setWrapper(SolutionWrapper w) {
		wrapper = w;
	}

	public SolutionWrapper getWrapper() {
		return wrapper;
	}

	public abstract void setNeighborhoods(List<CPPNeighborhood> neighs);

	public void setComparator(Comparator<Container> comparator) {
		comp = comparator;
	}

}
