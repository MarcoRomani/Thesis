package cpp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import general.Container;

public abstract class CPPPath_Relinking_Scheme {

	protected double alfa;
	protected SecureRandom rng;
	protected int n_moves;

	public CPPSolution relink(CPPSolution s, CPPSolution t) {

		List<Container> diff = computeDifference(s, t);
		int init_diff = diff.size();
		CPPSolution best = (s.getValue() <= t.getValue()) ? s : t;
		CPPSolution current = (CPPSolution) s.clone();

		HashMap<Container, Double> cost_gain = new HashMap<Container, Double>();
		ArrayList<Container> move = new ArrayList<Container>();
		while (!endCondition(diff, init_diff)) {
			cost_gain.clear();
			move.clear();

			for (int i = 0; i < diff.size(); i++) {
				Double tmp = costDifference(current, t, diff.get(i));
				cost_gain.put(diff.get(i), tmp);

			}

			diff.sort(this.new Implementation(cost_gain));
			 
			for(int contatore = 0; contatore < n_moves; contatore++) {
				if(diff.isEmpty()) break;
				Container m = diff.remove(rng.nextInt((int)(diff.size()*alfa)));
				move.add(m);
			}
			
			applyMove(current,  move); // muove un batch di container
			
		}

		return null;
	}

	private void applyMove(CPPSolution current, ArrayList<Container> move) {
		// TODO Auto-generated method stub
		
	}

	private Double costDifference(CPPSolution current, CPPSolution t, Container v) {
		// TODO Auto-generated method stub
		return null;
	}

	protected List<Container> computeDifference(CPPSolution x, CPPSolution y) {
		return null;
	}

	protected boolean endCondition(List<Container> diff, int init_diff) {
		return diff.isEmpty();
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
