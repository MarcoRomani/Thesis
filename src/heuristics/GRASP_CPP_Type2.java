package heuristics;

import java.util.ArrayList;
import java.util.Iterator;

import general.Container;
import general.Customer;
import general.DataCenter;

public class GRASP_CPP_Type2 extends GRASP_CPP_Type1 {

	public GRASP_CPP_Type2(DataCenter dc, Iterator<CPPSolution> iter) {
		super(dc,iter);
	}

	@Override
	protected CPPSolution greedy_rand_construction(float alfa) throws InfeasibilityException {
        
		CPPSolution sol = new CPPSolution();
		ArrayList<Container> toPlace = new ArrayList<Container>();
		
		for(Customer c: req) {
			toPlace.addAll(c.getNewContainers());
		}
		sol = notnew_constr(toPlace,alfa,sol);
		System.out.println("other cust done");
		Result result = allnew_constr(alfa,stubs_u,stubs,sol);
		System.out.println("new cust done \n");
		sol = result.getSol();
	   
		toPlace = result.getRest();
		sol = notnew_constr(toPlace,alfa,sol);
		System.out.println("rest done \n");
		
		return sol;
		
	}
	
	
}
