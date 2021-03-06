package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;
import general.Customer;
import general.DataCenter;

/**
 * @author Marco grasp configuration:
 *  identical to Type1 except that already
 *  existing customers are tried first, then all the new,
 *  and finally any vm in excess from the previous phase.
 *  All is done in timestamp order.
 */
public class GRASP_CPP_Type2 extends GRASP_CPP_Type1Indexing {
	
	public GRASP_CPP_Type2(DataCenter dc) {
		super(dc);
	}

	public GRASP_CPP_Type2(DataCenter dc, List<CPPNeighborhood> iters) {
		super(dc, iters);
	}

	@Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {

		CPPSolution sol = new CPPSolution();
		ArrayList<Container> toPlace = new ArrayList<Container>();
        ArrayList<Container> toPlaceRandom = new ArrayList<Container>();
        
		for (Customer c : req) {
			toPlace.addAll(c.getNewContainers());
		}
		
		while(toPlace.size() > 0) {
			Container tmp = toPlace.remove(rng.nextInt(toPlace.size()));
			toPlaceRandom.add(tmp);
		}
		sol = notnew_constr(toPlaceRandom, alfa, sol);
	//	System.out.println("other cust done");
		Result result = allnew_constr(alfa, sol);
	//	System.out.println("new cust done \n");
		sol = result.getSol();

		toPlace = result.getRest();
		toPlaceRandom.clear();
		while(toPlace.size() > 0) {
			Container tmp = toPlace.remove(rng.nextInt(toPlace.size()));
			toPlaceRandom.add(tmp);
		}
		sol = notnew_constr(toPlaceRandom, alfa, sol);
	//	System.out.println("rest done \n");

		return sol;

	}

}
