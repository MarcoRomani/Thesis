package heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import general.Container;
import general.Customer;

public class CPPOneSwitchIter implements Iterator<CPPSolution> {

	private CPPSolution sol;
	private int cust_index = 0;
	private int cont_index = 0;
	private int serv_index = 0;
	private ArrayList<Container> conts;
	private ArrayList<ServerStub> servs;
	
	public CPPOneSwitchIter(CPPSolution solution) {
		sol = (CPPSolution) solution.clone();
	}
	
	@Override
	public boolean hasNext() {
		if(cust_index + cont_index + serv_index == Customer.custList.size() + conts.size() + servs.size() - 3) {
			return false;
		}
		return true;
	}

	@Override
	public CPPSolution next() {
		// TODO Auto-generated method stub
		
		return null;
	}

}
