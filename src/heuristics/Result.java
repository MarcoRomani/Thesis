package heuristics;
import java.util.ArrayList;

import general.*;
public class Result {

	CPPSolution sol;
	ArrayList<Container> rest;
	
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
