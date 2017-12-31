package stCPP;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;


import general.*;
import heuristics.*;
import writeFiles.CPPtoAMPL;

public class Main {

	public static void main(String[] args) {

		int iter = 25;
		int my_seed = 226;
		int n_cust = 20;
		int n_newcust = 2;
		int n_newcont = 30;
		int n_pods = 6;
		
		for(int i = my_seed; i < my_seed+iter ; i++) {
		      doStuff(i,n_pods,n_cust,n_newcust,n_newcont,"FatTree");
		}
	}

	private static void doStuff(int my_seed, int n_pods, int n_cust, int n_newcust, int n_newcont, String dctype) {
		
		Customer.cust_id = 0;
		Customer.custList.clear();
		
		
		byte [] seed = BigInteger.valueOf(my_seed).toByteArray();
		SecureRandom rng = new SecureRandom(seed); // SHA1PRNG
		Catalog.setRNG(rng);
		
		
		DataCenter dc = new DataCenter(dctype,n_pods);
		ArrayList<Customer> customers = new ArrayList<Customer>();
		
		for(int i=0; i< n_cust; i++) {			
		//	customers.add(new Customer(((rng.nextDouble()/500)+(double)0.001),Business.values()[rng.nextInt(2)],rng));
			customers.add(new Customer(((double)(rng.nextInt(4500)+500)/10000000)*8,Business.values()[rng.nextInt(2)],rng));
		}
		
		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for(int i=0; i< n_newcust; i++) {			
			//new_customers.add(new Customer(((rng.nextDouble()/500)+(double)0.001),Business.values()[rng.nextInt(2)],rng));
			new_customers.add(new Customer(((double)(rng.nextInt(4500)+500)/10000000)*8,Business.values()[rng.nextInt(2)],rng));

			new_customers.get(i).transformIntoNew();
		}
		
		int howmanyalready = 0;
		for(Customer c: new_customers) {
			howmanyalready += c.getNewContainers().size();
		}
		
		int rnd = rng.nextInt(Customer.custList.size());
		
		for(int i=0; i < n_newcont - howmanyalready ; i++) {
			if(rng.nextInt(3) <=1) {
			     // dont update rnd - customer
			} else {
				rnd = rng.nextInt(Customer.custList.size());
			}
			
			int tmp = rng.nextInt(3);
			
			switch(tmp) {
			
			case 0:
				Customer.custList.get(rnd).addWS();
				break;
			case 1:
				Customer.custList.get(rnd).addAS();
				break;
			case 2:
				Customer.custList.get(rnd).addDBMS();
				break;
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
		filler = new BestRack(rng);
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
	   
	   
		
		GRASP_CPP_Scheme heur= new GRASP_CPP_Type1(dc, new CPPOneSwitchStrictIter());
		//GRASP_CPP_Scheme heur= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol = heur.grasp(10, my_seed,(float) 0.1);
		
		
		GRASP_CPP_Scheme heur2= new GRASP_CPP_Type2(dc, new CPPOneSwitchIter());
		//GRASP_CPP_Scheme heur2= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol2 = heur2.grasp(10, my_seed,(float) 0.1);
		
		
		
		System.out.println("solution value: "+sol.getValue()+" size ="+sol.getTable().size()); 
		System.out.println("solution value: "+sol2.getValue()+" size ="+sol2.getTable().size()); 
	}
}
