package cpp_heuristics;


import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * 
 * @author Marco
 * Wrapper for final solutions produced by concurrent threads.
 *  It stores the best of them in a separate field.
 */
public class SolutionWrapper {

	protected Date my_date = new Date();
	protected CopyOnWriteArrayList<CPPSolution> solutions;
	protected CopyOnWriteArrayList<Best_Entry> bestlist;
	protected CPPSolution best ;
	protected CPPSolution best_init;
	protected long time_best;
	protected int count = 0;
	protected Integer iterations = new Integer(0);
	protected Long time = new Long(0);

	public SolutionWrapper() {
		solutions = new CopyOnWriteArrayList<CPPSolution>();
		bestlist = new CopyOnWriteArrayList<Best_Entry>();
		best = new CPPSolution();
		best_init = new CPPSolution();
	}

	public synchronized void updateIterations(int batch) {
		iterations = new Integer(iterations.intValue()+batch);
	}
	
	public synchronized void updateTime(long n_time) {
		time = new Long(n_time);
	}
	
	public synchronized int getIterations() {
		return iterations.intValue();
	}
	
	public synchronized void updateBests(CPPSolution sol) {
		if (sol.getValue() < best.getValue()) {
			best = sol;
			long ts = (new Date()).getTime() - my_date.getTime();
			time_best = ts ;
			bestlist.add(new Best_Entry(sol, ts));
		}
	}
	
	public synchronized void updateSolutions(CPPSolution sol) {
		solutions.add(sol);

		count++;
		
	}

	public synchronized CPPSolution getBest() {
		return best;
	}
	
	public synchronized CopyOnWriteArrayList<CPPSolution> getSolutions(){
		return solutions;
	}
	
	public synchronized CopyOnWriteArrayList<Best_Entry> getBestList(){
		return bestlist;
	}
	
	public synchronized int getCount() {
		return count;
	}
	
	public synchronized void updateInit(CPPSolution sol) {
		if(best_init == null || sol.getValue() < best_init.getValue()) {
			best_init = sol;
		}
	}
	
	public synchronized CPPSolution getBestInit() {
		return best_init;
	}
	
	public synchronized long getTimeBest() {
		return time_best;
	}
	
	public synchronized long getTime() {
		return time;
	}
	
	public class Best_Entry{
		private CPPSolution sol;
		private long timestamp;
		
		protected Best_Entry(CPPSolution s, long t) {
			sol = s;
			timestamp = t;
		}
		
		public CPPSolution getSol() {
			return sol;
		
		}
		
		public long getTime() {
			return timestamp;
		}
	}
}
