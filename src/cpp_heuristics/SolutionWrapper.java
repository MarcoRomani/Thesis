package cpp_heuristics;


import java.util.concurrent.CopyOnWriteArrayList;

public class SolutionWrapper {

	protected CopyOnWriteArrayList<CPPSolution> solutions;
	protected CPPSolution best ;

	public SolutionWrapper() {
		solutions = new CopyOnWriteArrayList<CPPSolution>();
		best = new CPPSolution();
	}

	public synchronized void updateSolutions(CPPSolution sol) {
		solutions.add(sol);

		if (sol.getValue() < best.getValue()) {
			best = sol;
		}
	}

	public synchronized CPPSolution getBest() {
		return best;
	}
	
	public CopyOnWriteArrayList<CPPSolution> getSolutions(){
		return solutions;
	}
}
