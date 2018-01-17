package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;
import general.Container;
import general.Customer;
import general.DataCenter;
/**
 * 
 * @author Marco
 * Identical to Type2 except containers belonging to already existing customers
 * are sorted by descending residual ram order before being placed
 *
 */
public class GRASP_CPP_Type4 extends GRASP_CPP_Type1Indexing {

	public GRASP_CPP_Type4(DataCenter dc) {
		super(dc);
	}
	
	public GRASP_CPP_Type4(DataCenter dc, List<CPPNeighborhood> iters) {
		super(dc, iters);
	}

	@Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {

		CPPSolution sol = new CPPSolution();
		ArrayList<Container> toPlace = new ArrayList<Container>();

		for (Customer c : req) {
			toPlace.addAll(c.getNewContainers());
		}
		toPlace.sort(comp);
		sol = notnew_constr(toPlace, alfa, sol);
	//	System.out.println("other cust done");
		Result result = allnew_constr(alfa, sol);
	//	System.out.println("new cust done \n");
		sol = result.getSol();

		toPlace = result.getRest();
		toPlace.sort(comp);

		sol = notnew_constr(toPlace, alfa, sol);
	//	System.out.println("rest done \n");

		return sol;

	}

}
