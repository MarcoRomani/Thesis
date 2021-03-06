package cpp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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


public class CPPPath_Relinking_Scheme {
	public static double min_delta = 0.000000001;
	protected double alfa;  // randomization param
	protected int iterations;   
	protected SecureRandom rng = new SecureRandom(); 
	protected int n_moves;  // size of a move
	protected List<ServerStub> stubs = new ArrayList<ServerStub>();
	protected DataCenter dc;
	protected double beta;  // truncation parameter
	protected int neigh_index = 0;
	
	protected CPPNeighborhood neighborhood_explorer;
	protected List<CPPNeighborhood> neighborhoods = new ArrayList<CPPNeighborhood>();
	
	public CPPPath_Relinking_Scheme(DataCenter dc,double alfa, int iter, double beta, int n_moves) {
		this.dc = dc;
		this.alfa = alfa;
		this.beta = beta;
		this.iterations = iter;
		this.n_moves = n_moves;		
		
		for(Pod p:dc.getPods()) {
			for(Rack r:p.getRacks()) {
				for(Server s:r.getHosts()) {
					stubs.add(new ServerStub(s));
				}
			}
		}
	}

	public CPPPath_Relinking_Scheme(DataCenter dc,double alfa, int iter, double beta, int n_moves, SecureRandom rng) {		
		this(dc,alfa,iter,beta,n_moves);
		this.rng = rng;
	}
	
	public void setNeighborhoods(List<CPPNeighborhood> neighs) {
		this.neighborhoods.addAll(neighs);
		neigh_index = 0;
		this.neighborhood_explorer = neighs.get(neigh_index);
	}
	
	protected void changeNeighborhood() {
		neigh_index++;
		if (neigh_index >= neighborhoods.size()) {
			neigh_index = 0;
		}
		neighborhood_explorer = neighborhoods.get(neigh_index);
	}
	
	public CPPSolution relink(CPPSolution s, CPPSolution t) {

	// System.out.println("TRY PATH BETWEEN \n"+s.toString()+"AND \n"+t.toString());

		List<Container> difference = computeDifference(s, t);
		List<Container> diff = new ArrayList<Container>();
	

		CPPSolution best = (s.getValue() <= t.getValue()) ? s : t;
		CPPSolution current = new CPPSolution();
		for(Container v: s.getTable().keySet()) {
			int st = s.getTable().get(v).intValue();
			stubs.get(st).forceAllocation(v, stubs, current, dc);
			current.getTable().put(v, new Integer(st));
		}
		current.setValue(s.getValue());

		for (int iter = 0; iter < iterations; iter++) {
			
      // System.out.println("INNER ITERATION "+iter);
			diff.addAll(difference);
			
			HashMap<Container, Double> cost_gain = new HashMap<Container, Double>();
			ArrayList<Container> move = new ArrayList<Container>();
			
			while (!endCondition(diff, difference)) {
			//	System.out.println("WHILE LOOP");
			//	System.out.println("DISTANCE: \t"+diff.size());
				cost_gain.clear();
				move.clear();

				for (int i = 0; i < diff.size(); i++) {
					Double tmp = costDifference(current, t, diff.get(i));
					cost_gain.put(diff.get(i), tmp);

				}

				diff.sort(this.new CostComparator(cost_gain));

				for (int contatore = 0; contatore < n_moves; contatore++) {
					if (diff.isEmpty())
						break;
					Container m = diff.remove(rng.nextInt(Math.max(1,(int) (diff.size() * alfa))));
					move.add(m);
				}

				current = applyMove(current, t, move); // muove un batch di container

				if (current.getValue() < best.getValue() - min_delta) {
					if(Main.display) {
					   System.out.println("BETTER: "+current.getValue()+"\t"+best.getValue());
					}
					CPPSolution incumbent = (CPPSolution)current.clone();
					int count = 0;
					neigh_index = 0;
					neighborhood_explorer = neighborhoods.get(neigh_index);
					do {						
						CPPSolution newincumbent = localSearch(incumbent);
						if (!(newincumbent.getValue() < incumbent.getValue() - min_delta)) {
							count++;
						} else {
							count = 0;
						}
						incumbent = newincumbent;
						changeNeighborhood();
					} while (count < neighborhoods.size() && neighborhoods.size() > 1);

					best = (CPPSolution)incumbent.clone();
					reset(incumbent,current);
				}else { 
			//	System.out.println("WORSE: "+current.getValue()+"\t"+best.getValue());
					}

				
			}
			
			// soft-reset for next iteration
			reset(current,s);
		}
		
		//hard-reset
	
		ArrayList<Container> keys = new ArrayList<Container>();
		keys.addAll(current.getTable().keySet());
		for(Container v: keys) {
			int st = current.getTable().get(v).intValue();
			stubs.get(st).remove(v, stubs, current, dc);
			current.getTable().remove(v);
		}
		

		return best;
	}

	protected CPPSolution applyMove(CPPSolution current, CPPSolution target, ArrayList<Container> move) {
		
	     while(!move.isEmpty()) {
	    //	 System.out.println("Applymove");
	    	 boolean prev_feasib = (current.getValue() != Double.POSITIVE_INFINITY);
	    	 Container m = move.remove(0);
	    	 double delta = costDifference(current,target, m).doubleValue();
	    	 stubs.get(current.getTable().get(m).intValue()).remove(m, stubs, current, dc);
	    	 current.getTable().remove(m);
	    	 stubs.get(target.getTable().get(m).intValue()).forceAllocation(m, stubs, current, dc);
	    	 current.getTable().put(m, target.getTable().get(m));
	    	 if(prev_feasib) {
	    		 current.setValue(current.getValue()+delta);
	    	 }else {
	    		 current.setValue(Double.POSITIVE_INFINITY);
	    		 evaluate(current);
	    	 }
	     }
	     
	     return current;

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

	protected Double costDifference(CPPSolution current, CPPSolution t, Container v) {
		double cost1 = 0;
		double cost2 = 0;
		
		ServerStub st1 = stubs.get(current.getTable().get(v).intValue());
		ServerStub st2 = stubs.get(t.getTable().get(v).intValue());
		
		st1.remove(v, stubs, current, dc);
		current.getTable().remove(v);
		if (!(st2.allocate(v, stubs, current, dc, false))) {
			cost2 = Double.POSITIVE_INFINITY;
			st1.forceAllocation(v, stubs, current, dc); // rollback
			current.getTable().put(v, new Integer(st1.getId()));
			return new Double(cost2);
		}
	//	if(current.getValue() == Double.POSITIVE_INFINITY) return new Double(Double.NEGATIVE_INFINITY);
		
		st1.forceAllocation(v, stubs, current, dc); // rollback
		current.getTable().put(v, new Integer(st1.getId()));

		Customer r = Customer.custList.get(v.getMy_customer());
		ArrayList<Container> conts = r.getContainers();

		for (Container c : conts) {
			Server s = dc.getPlacement().get(c);
			Double t1 = r.getTraffic().get(new C_Couple(v, c));
			Double t2 = r.getTraffic().get(new C_Couple(c, v));
			if (!(t1 == null)) {
				cost1 -= dc.getCosts()[st1.getId()][s.getId()] * t1.doubleValue();
				cost2 += dc.getCosts()[st2.getId()][s.getId()] * t1.doubleValue();
			}
			if (!(t2 == null)) {
				cost1 -= dc.getCosts()[s.getId()][st1.getId()] * t2.doubleValue();
				cost2 += dc.getCosts()[s.getId()][st2.getId()] * t2.doubleValue();
			}
		}
		conts = r.getNewContainers();

		for (Container c : conts) {
			Integer s = current.getTable().get(c);
			if (!(s == null)) {
				Double t1 = r.getTraffic().get(new C_Couple(v, c));
				Double t2 = r.getTraffic().get(new C_Couple(c, v));
				if (!(t1 == null)) {
					cost1 -= dc.getCosts()[st1.getId()][s.intValue()] * t1.doubleValue();
					cost2 += dc.getCosts()[st2.getId()][s.intValue()] * t1.doubleValue();
				}
				if (!(t2 == null)) {
					cost1 -= dc.getCosts()[s.intValue()][st1.getId()] * t2.doubleValue();
					cost2 += dc.getCosts()[s.intValue()][st2.getId()] * t2.doubleValue();
				}
			}
		}

		
		return new Double(cost1+cost2);
	}

	protected boolean checkFeasibility(CPPSolution incumbent) {

		List<Container> tmp2 = new ArrayList<Container>();

		for (Customer c : Customer.custList) {
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

		float[] usedBDWout = new float[servers.size()];
		float[] usedBDWin = new float[servers.size()];
		float[] usedCPU = new float[servers.size()];
		float[] usedRAM = new float[servers.size()];
		float[] usedDISK = new float[servers.size()];

		for (int i = 0; i < servers.size(); i++) {
			List<Container> tmp = servers.get(i).getContainers();
			for (Container c1 : tmp) {
				Customer r = Customer.custList.get(c1.getMy_customer());
				for (Container c2 : r.getNewContainers()) {
					if (!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId())) {
						usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
								: r.getTraffic().get(new C_Couple(c1, c2)).floatValue();
						usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
								: r.getTraffic().get(new C_Couple(c2, c1)).floatValue();
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
					: r.getTraffic().get(new C_Couple(c1, Container.c_0)).floatValue();
			usedBDWin[i] += (r.getTraffic().get(new C_Couple(Container.c_0, c1)) == null) ? 0
					: r.getTraffic().get(new C_Couple(c1, Container.c_0)).floatValue();
			for (Container c2 : r.getContainers()) {
				if (!(dc.getPlacement().get(c2).getId() == servers.get(i).getId())) {
					usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c1, c2)).floatValue();
					usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c2, c1)).floatValue();
				}
			}

			for (Container c2 : r.getNewContainers()) {
				if (!(incumbent.getTable().get(c2).intValue() == servers.get(i).getId())) {
					usedBDWout[i] += (r.getTraffic().get(new C_Couple(c1, c2)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c1, c2)).floatValue();
					usedBDWin[i] += (r.getTraffic().get(new C_Couple(c2, c1)) == null) ? 0
							: r.getTraffic().get(new C_Couple(c2, c1)).floatValue();
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
	
	protected List<Container> computeDifference(CPPSolution x, CPPSolution y) {
		ArrayList<Container> difference = new ArrayList<Container>();
		for(Container v: x.getTable().keySet()) {
			if(x.getTable().get(v).intValue() != y.getTable().get(v).intValue()) {
				difference.add(v);
			}
		}
		return difference;
	}

	protected CPPSolution localSearch(CPPSolution init) {
		CPPSolution sol = (CPPSolution) init.clone();
		evaluate(sol);

		CPPSolution best_neighbor = sol;

		// System.out.println("start local search");

		do {
			// System.out.println("Try new neighborhood");
			sol = best_neighbor;
			neighborhood_explorer.setUp(dc, stubs, best_neighbor);

			while (neighborhood_explorer.hasNext()) {
				// System.out.println("next");
				CPPSolution current = neighborhood_explorer.next();
				if (evaluate(current) < best_neighbor.getValue() - min_delta) {
					best_neighbor = current;
		//			 System.out.println("new best neighbor found "+best_neighbor.getValue());
				}

			}

		} while (sol.getValue() != best_neighbor.getValue());

		neighborhood_explorer.clear();
		// System.out.println("end local search");
		sol = best_neighbor;
		// System.out.println(sol.toString());
		return sol;
	}
	
	protected boolean endCondition(List<Container> diff, List<Container> initial_diff) {
		return diff.size() <= ((1-beta)*initial_diff.size());
	}

	protected void reset(CPPSolution current, CPPSolution s) {
		List<Container> difference = new ArrayList<Container>();
		difference = computeDifference(s,current);
		for(Container v: difference) {
			int st = current.getTable().get(v).intValue();
			stubs.get(st).remove(v, stubs, current, dc);
			current.getTable().remove(v);
		}
		for(Container v: difference) {
			int st = s.getTable().get(v).intValue();
			stubs.get(st).forceAllocation(v, stubs, current, dc);
			current.getTable().put(v, new Integer(st));
		}
		current.setValue(s.getValue());
	}
	
	protected class CostComparator implements Comparator<Container> {

		private HashMap<Container, Double> map;

		CostComparator(HashMap<Container, Double> mp) {
			map = mp;
		}

		@Override
		public int compare(Container arg0, Container arg1) {

			return (int) Math.signum(map.get(arg0) - map.get(arg1));
		}

	}

}
