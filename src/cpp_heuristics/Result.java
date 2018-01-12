package cpp_heuristics;
import java.util.ArrayList;

import general.*;

/**
 * 
 * @author Marco
 * Wrapper of current partial solution + container in excess produced by 
 * partial initial solution contruction
 */
public class Result {

	private CPPSolution sol;
	private ArrayList<Container> rest;
	
	public Result(CPPSolution my_sol, ArrayList<Container> my_rest) {
		sol = my_sol;
		rest = my_rest;
	}

	public CPPSolution getSol() {
		return sol;
	}

	public ArrayList<Container> getRest() {
		return rest;
	}
}
