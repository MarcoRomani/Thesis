package cpp_heuristics;

import cmp_heuristics.CMPSolution;

public class InfeasibilityException extends Exception {

	protected CPPSolution sol;
	public InfeasibilityException(CPPSolution sol) {
		this.sol = sol;
	}
	
	public CPPSolution getSolution() {
		return sol;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9194936293928654030L;

}
