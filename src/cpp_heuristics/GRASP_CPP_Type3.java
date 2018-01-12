package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;
import general.Customer;
import general.DataCenter;
/**
 * 
 * @author Marco
 * Identical to Type1 except the containers of already existing customers are
 * sorted by descending ram order before being placed
 *
 */
public class GRASP_CPP_Type3 extends GRASP_CPP_Type1 {

	public GRASP_CPP_Type3(DataCenter dc) {
		super(dc);
	}

	public GRASP_CPP_Type3(DataCenter dc, List<CPPNeighborhood> iters) {
		super(dc, iters);
	}

	@Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {

		CPPSolution sol = new CPPSolution();

		Result result = allnew_constr(alfa, sol);
	//	System.out.println("new cust done \n");
		sol = result.getSol();
		ArrayList<Container> toPlace = result.getRest();
		for (Customer c : req) {
			toPlace.addAll(c.getNewContainers());
		}

		toPlace.sort(new ContainerRamComparator());
		sol = notnew_constr(toPlace, alfa, sol);
	//	System.out.println("other cust done");
		return sol;

	}

}
