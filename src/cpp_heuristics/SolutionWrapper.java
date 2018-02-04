package cpp_heuristics;


import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * 
 * @author Marco
 * Wrapper for final solutions produced by concurrent threads.
 *  It stores the best of them in a separate field.
 */
public class SolutionWrapper {

	protected CopyOnWriteArrayList<CPPSolution> solutions;
	protected CPPSolution best ;
	protected int count = 0;
	protected Integer iterations = new Integer(0);

	public SolutionWrapper() {
		solutions = new CopyOnWriteArrayList<CPPSolution>();
		best = new CPPSolution();
	}

	public synchronized void updateIterations(int batch) {
		iterations = new Integer(iterations.intValue()+batch);
	}
	
	public synchronized int getIterations() {
		return iterations.intValue();
	}
	public synchronized void updateSolutions(CPPSolution sol) {
		solutions.add(sol);

		count++;
		if (sol.getValue() < best.getValue()) {
			best = sol;
		}
	}

	public synchronized CPPSolution getBest() {
		return best;
	}
	
	public synchronized CopyOnWriteArrayList<CPPSolution> getSolutions(){
		return solutions;
	}
	
	public synchronized int getCount() {
		return count;
	}
}
