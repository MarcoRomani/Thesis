package stCPP;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;


import general.*;
import heuristics.*;
import writeFiles.CPPtoAMPL;

public class Main {

	public static void main(String[] args) {

		int my_seed = 456;
		byte [] seed = BigInteger.valueOf(my_seed).toByteArray();
		SecureRandom rng = new SecureRandom(seed); // SHA1PRNG
		Catalog.setRNG(rng);
		int n_cust = 10;
		int n_newcust = 2;
		int n_newcont = 25;
		int n_pods = 8;
		
		DataCenter dc = DataCenter.buyFatTreeDC(n_pods);
		ArrayList<Customer> customers = new ArrayList<Customer>();
		
		for(int i=0; i< n_cust; i++) {			
			customers.add(new Customer(((rng.nextFloat()/200)+(float)0.001),Business.values()[rng.nextInt(2)],rng));
		}
		
		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for(int i=0; i< n_newcust; i++) {			
			new_customers.add(new Customer(((rng.nextFloat()/200)+(float)0.001),Business.values()[rng.nextInt(2)],rng));
			new_customers.get(i).transformIntoNew();
		}
		
		for(int i=0; i < n_newcont ; i++) {
			int rnd = rng.nextInt(customers.size());
			int tmp = rng.nextInt(3);
			
			switch(tmp) {
			
			case 0:
				customers.get(rnd).addWS();
			case 1:
				customers.get(rnd).addAS();
			case 2:
				customers.get(rnd).addDBMS();
			}
			
		}
		
		int count = 0;
		for(Customer c: customers) {
			count += c.getNewContainers().size();
		}
		for(Customer c: new_customers) {
			count += c.getNewContainers().size();
		}
		System.out.println("\n new containers: "+count);
		
		
		// passare roba al filler
		DC_filler filler= new FirstFit();
		filler.populate(dc, customers, (float)0.7);
		
	   for(Pod p: dc.getPods()) {
		   for(Rack r: p.getRacks()) {
			   for(Server s: r.getHosts()) {
				   System.out.println(s.toString());
			   }
		   }
	   }
	   
	   CPPtoAMPL writer = new CPPtoAMPL();
	   writer.writeDAT(dc, customers, new_customers, my_seed);
		
		GRASP_CPP_Scheme heur= new GRASP_CPP_Type1(dc, new CPPOneSwitchIter());
		//CPPSolution sol = heur.grasp(30, my_seed,(float) 0.1);
		//System.out.println("solution value: "+sol.getValue()+" size ="+sol.getTable().size()); 
	}

	
}
