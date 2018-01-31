package cpp_heuristics;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import general.Container;

public class PathRel_manager {

	protected int maxIter = Integer.MAX_VALUE;
	protected long maxTime = Long.MAX_VALUE ;
	protected int maxElite;
	protected int maxTaboo;
	protected List<CPPSolution> pool = new ArrayList<CPPSolution>();
	protected HashMap<Sol_Couple, Boolean> table = new HashMap<Sol_Couple,Boolean>();
	
	protected List<CPPSolution> candidate = new ArrayList<CPPSolution>();
	protected List<CPPSolution> taboo = new ArrayList<CPPSolution>();
	protected CPPSolution best;
	

	public CPPSolution path_relinking() {
		
		
		ConcurrentLinkedQueue<Sol_Couple> tasks = new ConcurrentLinkedQueue<Sol_Couple>();
		int iter = 0;
		Date d1 = new Date();
		Date d2 = new Date();
		while(iter < maxIter && (d2.getTime()-d1.getTime()) < maxTime && !stopCondition()) {
		
			// SELECT UNEXPLORED PATHS
			tasks.clear();
			for(Sol_Couple sc : table.keySet()) {
				if(!table.get(sc).booleanValue()) {
					tasks.add(sc);
				}
			}
			
			// THREADS TODO
			
			updatePool();
			d2 = new Date();
		}
		
		
		
		
		return best;
	}
	
	protected boolean stopCondition() {
		
		for(Sol_Couple sc : table.keySet()) {
			if(!table.get(sc).booleanValue()) {
				return false;
			}
		}
		return true;
	}

	protected boolean updatePool() {
		boolean update = false;
		
		for(CPPSolution sol: candidate) {
			
			if(!ammissionCondition(sol)) {
				continue;
			}
			
			if(pool.size() < maxElite) {
				addToPool(sol);
				update = true;
				continue;
			}
			
			List<CPPSolution> worse = findWorse(sol);
			CPPSolution most_sim =null;
			int max_similarity = -1;
			for(CPPSolution w: worse) {
				int tmp = computeSimilarity(sol,w);
				if(tmp > max_similarity ) {
					most_sim = w;
					max_similarity = tmp;
				}
			}
			
			removeFromPool(most_sim);
			addToPool(sol);
			update = true;
			
		}
		
		candidate.clear();
		return update;
	}
	
	
	protected boolean ammissionCondition(CPPSolution sol) {
		
		for(CPPSolution s : pool) {
			if(s.equals(sol)) {
				return false;
			}
		}
		for(CPPSolution s : taboo) {
			if(s.equals(sol)) {
				return false;
			}
		}
		
		return true;
	}


	protected void addToPool(CPPSolution sol) {
		for(CPPSolution s: pool) {
			table.put(new Sol_Couple(s,sol), new Boolean(false));
			table.put(new Sol_Couple(sol,s), new Boolean(false));
		}
		pool.add(sol);
		if(sol.getValue() < best.getValue()) {
			best = sol;
		}
	}
	
	protected void removeFromPool(CPPSolution sol) {
		pool.remove(sol);
		for(CPPSolution s : pool) {
			
			table.remove(new Sol_Couple(s,sol));
			table.remove(new Sol_Couple(sol,s));
			
		}
		
	}
	
	protected void addToTaboo(CPPSolution sol) {
		taboo.add(sol);
		if(taboo.size() > maxTaboo) {
			taboo.remove(0);
		}
	}
	
	protected List<CPPSolution> findWorse(CPPSolution sol){
		ArrayList<CPPSolution> worse = new ArrayList<CPPSolution>();
		for(CPPSolution cs: pool) {
			if (cs.getValue() > sol.getValue()) {
				worse.add(cs);
			}
		}
		return worse;
	}
	
	protected int computeSimilarity(CPPSolution x,CPPSolution y) {
		if (x.equals(y)) return x.getTable().size(); // SHOULD NOT HAPPEN
		int sim_count = 0;
		for(Container c: x.getTable().keySet()) {
			if(x.getTable().get(c).intValue() == y.getTable().get(c).intValue()) {
				sim_count++;
			}
		}
		return sim_count;
	}
	
	protected class Sol_Couple {
		protected CPPSolution s;
		protected CPPSolution t;
		
		Sol_Couple(CPPSolution s,CPPSolution t){
			this.s = s;
			this.t= t;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
		//	result = prime * result + getOuterType().hashCode();
			result = prime * result + ((s == null) ? 0 : s.hashCode());
			result = prime * result + ((t == null) ? 0 : t.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Sol_Couple other = (Sol_Couple) obj;
		
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

		
	}
}
