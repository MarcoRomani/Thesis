package cpp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import general.Container;
import general.DataCenter;
import stCPP.Main;

public class PathRel_manager {

	public static int parallelism = 16;
	public static int maxIter = Integer.MAX_VALUE;
	public static long maxTime = Long.MAX_VALUE ;
	protected  int maxElite;
	public static int maxTaboo = 7;
	protected List<CPPSolution> pool = new ArrayList<CPPSolution>();
	protected HashMap<Sol_Couple, Boolean> table = new HashMap<Sol_Couple,Boolean>();
	
	protected ConcurrentLinkedQueue<CPPSolution> candidate = new ConcurrentLinkedQueue<CPPSolution>();
	protected List<CPPSolution> taboo = new ArrayList<CPPSolution>();
	protected CPPSolution best = new CPPSolution();
	
	protected SecureRandom rng = new SecureRandom();
	protected DataCenter dc;
	public static double alfa = 0;  // random
	public static double beta = 1;  // trunc
	public static int inner_iter = 1;
	public static int n_moves = 1;  // size of inner moves
	
	
	public PathRel_manager(DataCenter dc, int maxElite, List<CPPSolution> init_candidate) {
		this.dc = dc;
		this.maxElite = maxElite;
		this.candidate.addAll(init_candidate);
		updatePool();
	}
	
	public PathRel_manager(DataCenter dc, int maxElite,List<CPPSolution> init_candidate, SecureRandom rng){
		this(dc,maxElite,init_candidate);
		setRng(rng);
	}
	
	

	public CPPSolution path_relinking() {
		
		ConcurrentLinkedQueue<Sol_Couple> tasks = new ConcurrentLinkedQueue<Sol_Couple>();
		ArrayList<CPPPath_Relinking_Scheme> algs = new ArrayList<CPPPath_Relinking_Scheme>();
		
		for(int i=0;i<parallelism;i++) {
			algs.add(new CPPPath_Relinking_Scheme(dc,alfa,inner_iter,beta,n_moves,rng));
			ArrayList<CPPNeighborhood> neighs = new ArrayList<CPPNeighborhood>();
			neighs.add(new CPPOneSwitchSmallIter());
			neighs.add(new CPPOneSwitchMediumIter());
			neighs.add(new CPPOneSwapSmallIter());
			neighs.add(new CPPOneSwapIter());
			algs.get(algs.size()-1).setNeighborhoods(neighs);
		}
				
		int iter = 0;
		boolean flag = true;
		Date d1 = new Date();
		Date d2 = new Date();
		while(iter < maxIter && (d2.getTime()-d1.getTime()) < maxTime && flag) {
		
			iter++;
			if(Main.display) {
			     System.out.println("Path Relinking iteration: "+iter);
			}
			// SELECT UNEXPLORED PATHS
			tasks.clear();
			for(Sol_Couple sc : table.keySet()) {
				if(!table.get(sc).booleanValue()) {
					tasks.add(sc);
					table.replace(sc, new Boolean(true));
				}
			}
			
			// SET UP THREADS
			AtomicInteger semaforo = new AtomicInteger(0);
			ArrayList<PRThread> threads = new ArrayList<PRThread>();
			for(CPPPath_Relinking_Scheme al: algs) {
				threads.add(new PRThread(al, tasks, candidate, semaforo));
			}

			// LAUNCH THREADS
			for(PRThread thread : threads) {
				thread.start();
			}
			
			try {
				synchronized (tasks) {
				while (semaforo.get() < threads.size()) {
					
						tasks.wait();
						
					}
				}
			} catch (InterruptedException e) {
                  e.printStackTrace();
			}

			// THREADS FINISHED, UPDATE POOL
			flag = updatePool();
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
			
			if(most_sim != null) {
				removeFromPool(most_sim);
				addToTaboo(most_sim);
				addToPool(sol);
				update = true;
			}
		}
		
		candidate.clear();
		if(Main.display) {
		    System.out.println("POOL SIZE: \t"+pool.size());
		}
		return update;
	}
	
	
	protected boolean ammissionCondition(CPPSolution sol) {
		
		if(sol.getValue() == Double.POSITIVE_INFINITY) {
			return false;
		}
		
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
		
		if(best == null || sol.getValue() < best.getValue()) {
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
	
	public void setRng(SecureRandom rng) {
		this.rng = rng;
	}
	
	public void setAlpha(double alpha) {
		alfa = alpha;
	}
	
	public void setBeta(double beta) {
		PathRel_manager.beta = beta;
	}
	
	public void setNMoves(int n_moves) {
		PathRel_manager.n_moves = n_moves;
	}
	
	public void setInnerIter(int iter) {
		inner_iter = iter;
	}
	
	public class Sol_Couple {
		protected CPPSolution s;
		protected CPPSolution t;
		
		Sol_Couple(CPPSolution s,CPPSolution t){
			this.s = s;
			this.t= t;
		}
		
		public CPPSolution getS() {
			return s;
		}
		
		public CPPSolution getT() {
			return t;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
