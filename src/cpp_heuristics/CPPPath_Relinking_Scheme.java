package cpp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import general.C_Couple;
import general.Container;
import general.Customer;
import general.DataCenter;
import general.Server;

public abstract class CPPPath_Relinking_Scheme {

	protected double alfa;  // randomization param
	protected int iterations;   
	protected SecureRandom rng; 
	protected int n_moves;  // size of a move
	protected List<ServerStub> stubs;
	protected DataCenter dc;
	private int beta;  // truncation parameter

	public CPPSolution relink(CPPSolution _s, CPPSolution _t) {

		CPPSolution s = (CPPSolution) _s.clone();
		CPPSolution t = (CPPSolution) _t.clone();

		List<Container> difference = computeDifference(s, t);
		List<Container> diff = new ArrayList<Container>();

		CPPSolution best = (s.getValue() <= t.getValue()) ? s : t;

		for (int iter = 0; iter < iterations; iter++) {
			CPPSolution current = (CPPSolution) s.clone();

			HashMap<Container, Double> cost_gain = new HashMap<Container, Double>();
			ArrayList<Container> move = new ArrayList<Container>();
			while (!endCondition(diff, difference)) {
				cost_gain.clear();
				move.clear();

				for (int i = 0; i < diff.size(); i++) {
					Double tmp = costDifference(current, t, diff.get(i));
					cost_gain.put(diff.get(i), tmp);

				}

				diff.sort(this.new Implementation(cost_gain));

				for (int contatore = 0; contatore < n_moves; contatore++) {
					if (diff.isEmpty())
						break;
					Container m = diff.remove(rng.nextInt(Math.max(1,(int) (diff.size() * alfa))));
					move.add(m);
				}

				current = applyMove(current, t, move); // muove un batch di container

				if (current.getValue() < best.getValue()) {
					best = (CPPSolution) current.clone();
					// LOCAL SEARCH
				}

				
			}
			
			// reser for next iteration
		}
		
		

		return best;
	}

	private CPPSolution applyMove(CPPSolution current, CPPSolution target, ArrayList<Container> move) {
	     while(!move.isEmpty()) {
	    	 Container m = move.remove(0);
	    	 double delta = costDifference(current,target, m).doubleValue();
	    	 stubs.get(current.getTable().get(m).intValue()).remove(m, stubs, current, dc);
	    	 current.getTable().remove(m);
	    	 stubs.get(target.getTable().get(m).intValue()).forceAllocation(m, stubs, current, dc);
	    	 current.getTable().put(m, target.getTable().get(m));
	    	 if(delta > Double.NEGATIVE_INFINITY) {
	    		 current.setValue(current.getValue()+delta);
	    	 }else {
	    		 evaluate(current);
	    	 }
	     }
	     
	     return current;

	}

	private void evaluate(CPPSolution current) {
		// TODO Auto-generated method stub
		
	}

	private Double costDifference(CPPSolution current, CPPSolution t, Container v) {
		double cost1 = 0;
		double cost2 = 0;
		
		ServerStub st1 = stubs.get(current.getTable().get(v).intValue());
		ServerStub st2 = stubs.get(t.getTable().get(v).intValue());
		
		st1.remove(v, stubs, current, dc);
		current.getTable().remove(v);
		if (!(st2.allocate(v, stubs, current, dc, false))) {
			cost2 = Double.POSITIVE_INFINITY;
			return new Double(cost2);
		}
		if(current.getValue() == Double.POSITIVE_INFINITY) return new Double(Double.NEGATIVE_INFINITY);
		
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
				if (!(t1 == null))
					cost1 -= dc.getCosts()[st1.getId()][s.intValue()] * t1.doubleValue();
					cost2 += dc.getCosts()[st2.getId()][s.intValue()] * t1.doubleValue();
				if (!(t2 == null))
					cost1 -= dc.getCosts()[s.intValue()][st1.getId()] * t1.doubleValue();
					cost2 += dc.getCosts()[s.intValue()][st2.getId()] * t2.doubleValue();
			}
		}

		
		return new Double(cost1+cost2);
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

	protected boolean endCondition(List<Container> diff, List<Container> initial_diff) {
		return diff.size() <= ((1-beta)*initial_diff.size());
	}

	private class Implementation implements Comparator<Container> {

		private HashMap<Container, Double> map;

		Implementation(HashMap<Container, Double> mp) {
			map = mp;
		}

		@Override
		public int compare(Container arg0, Container arg1) {

			return (int) Math.signum(map.get(arg0) - map.get(arg1));
		}

	}

}
