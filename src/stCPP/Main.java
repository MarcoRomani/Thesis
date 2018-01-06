package stCPP;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;

import cpp_heuristics.*;
import general.*;
import writeFiles.CPPtoAMPL;
import java.util.Date;
public class Main {

	public static void main(String[] args) {

		int iter = 1;
		int my_seed = 26;
		int n_newcust = 5;
		int n_cust = 10000;
		int n_newcont = 100;
		int n_pods = 34;
		
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
			customers.add(new Customer(((double)(rng.nextInt(3500)+400)/1000000)*8,Business.values()[rng.nextInt(2)],rng));
		}
		
		ArrayList<Customer> new_customers = new ArrayList<Customer>();
		for(int i=0; i< n_newcust; i++) {			
			//new_customers.add(new Customer(((rng.nextDouble()/500)+(double)0.001),Business.values()[rng.nextInt(2)],rng));
			new_customers.add(new Customer(((double)(rng.nextInt(3500)+400)/1000000)*8,Business.values()[rng.nextInt(2)],rng));

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
	  // writer.writeDAT(dc, customers, new_customers, my_seed);
	   
	   /*
		ArrayList<Iterator<CPPSolution>> iters1 = new ArrayList<Iterator<CPPSolution>>();
		iters1.add(new CPPOneSwitchStrictIter());
		iters1.add(new CPPOneSwitchIter());
		iters1.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur= new GRASP_CPP_Type1(dc, iters1);
		//GRASP_CPP_Scheme heur= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol = heur.grasp(10, my_seed,(float) 0.1);
		
		ArrayList<Iterator<CPPSolution>> iters2 = new ArrayList<Iterator<CPPSolution>>();
		iters2.add(new CPPOneSwitchStrictIter());
		iters2.add(new CPPOneSwitchIter());
		iters2.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur2= new GRASP_CPP_Type2(dc, iters2);
		//GRASP_CPP_Scheme heur2= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol2 = heur2.grasp(10, my_seed,(float) 0.1);
	
		ArrayList<Iterator<CPPSolution>> iters3 = new ArrayList<Iterator<CPPSolution>>();
		iters3.add(new CPPOneSwitchStrictIter());
		iters3.add(new CPPOneSwitchIter());
		iters3.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur3 = new GRASP_CPP_Type3(dc,iters3);
		CPPSolution sol3 = heur3.grasp(10, my_seed, (float)0.1);
	*/	
	   Date d4 = new Date();
		ArrayList<Iterator<CPPSolution>> iters4 = new ArrayList<Iterator<CPPSolution>>();
		iters4.add(new CPPOneSwitchStrictIter());
		iters4.add(new CPPOneSwitchIter());
		iters4.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur4= new GRASP_CPP_Type4(dc, iters4);
		//GRASP_CPP_Scheme heur2= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol4 = heur4.grasp(10, my_seed,(float) 0.1);
		
		Date d5 = new Date();
		ArrayList<Iterator<CPPSolution>> iters5 = new ArrayList<Iterator<CPPSolution>>();
		iters5.add(new CPPOneSwitchStrictIter());
		iters5.add(new CPPOneSwitchIter());
		iters5.add(new CPPOneSwapIter());
		GRASP_CPP_Scheme heur5= new GRASP_CPP_Type4(dc, iters5);
		//GRASP_CPP_Scheme heur2= new GRASP_CPP_Type1(dc, new CPPOneSwapIter());
		CPPSolution sol5 = heur5.grasp(10, my_seed,(float) 0.1);
		
		Date d6 = new Date();
	/*	
		System.out.println("solution value: "+sol.getValue()+" size ="+sol.getTable().size()); 
		System.out.println("solution value: "+sol2.getValue()+" size ="+sol2.getTable().size()); 
		System.out.println("solution value: "+sol3.getValue()+" size ="+sol3.getTable().size()); 
	*/
		System.out.println("solution value: "+sol4.getValue()+" size ="+sol4.getTable().size()+" time ="+(d5.getTime()-d4.getTime()));
		System.out.println("solution value: "+sol5.getValue()+" size ="+sol4.getTable().size()+" time ="+(d6.getTime()-d5.getTime()));
		
		int tot = 0;
		for(Customer c: Customer.custList) {
			tot += c.getContainers().size();
		}
		System.out.println(tot);
	}
}
