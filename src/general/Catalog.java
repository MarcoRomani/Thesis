package general;

import java.security.SecureRandom;
import java.util.*;

public class Catalog {

	private static boolean flag = false;
	private static ArrayList<Server_model> server_catalog = new ArrayList<Server_model>(Arrays.asList(Server_model.values()));
	private static ArrayList<Container_model> container_catalog = new ArrayList<Container_model>(Arrays.asList(Container_model.values()));
	private static HashMap<Server_model,double[]> servTable = new HashMap<Server_model, double[]>();
	private static HashMap<Container_model,double[]> contTable = new HashMap<Container_model, double []>();
	private static SecureRandom rng;
	
	private Catalog() {
	
		if(flag == false) {
			
	
			// pmax,pidle,core,freq,mem,iops,bdw
			double [][] t = {
					{245,(double)45.7,44,2200,128,128,10},
					{419,(double)67.2,56,2500,192,192,10},
					{915,106,112,2500,384,384,10},
					{459,(double)58.1,56,2500,192,192,10},
					{233,39,28,2500,48,48,10},
					{183,(double)45.6,28,2100,96,96,10},
					{426,(double)39.5,28,2500,192,192,10}
					};
			
			Server_model [] s = Server_model.values();
			
			for(int i = 0; i< s.length; i++){
				servTable.put(s[i],t[i]);
			}
			
			// vcpu, mem, bdw, iops/100
			double [][] t2 = {
					{1,1,(double)0.7,4},
					{1,2,(double)1.1,8},
					{2,4,(double)1.5,16},
					{2,8,(double)2.2,24},
					{4,16,(double)2.7,36},					
					{2,8,(double)0.45,24},
					{4,16,(double)0.75,36},
					{8,32,3,(double)43.2},
					{8,32,1,50},
					{16,64,2,64}				
			};

            Container_model [] c = Container_model.values();
            
            for(int i=0; i < c.length; i++){
            	contTable.put(c[i], t2[i]);
            }
			
			
			flag = true;
		}
	}	
	

	public static void setRNG(SecureRandom rnd) {
		rng = rnd;
	}
	
	public static Server_model buyServer() {
		return server_catalog.get(rng.nextInt(server_catalog.size()));
	}
	
	public static Container_model buyContainer() {
		
		return container_catalog.get(rng.nextInt(container_catalog.size()));
	}
	
	public static double[]  getServSpecs(Server_model mod) {
		if (!(flag)) new Catalog();
		return servTable.get(mod);
	}
	
	public static double[] getContSpecs(Container_model mod) {
		if (!(flag)) new Catalog();
		return contTable.get(mod);
		
	}
}
