package general;

import java.security.SecureRandom;
import java.util.*;
/**
 * Collection of specs for containers and servers
 * @author Marco
 *
 */
public class Catalog {

	private static boolean flag = false;
	/* available server types */
	private static List<Server_model> server_catalog = new ArrayList<Server_model>(Arrays.asList(Server_model.values()));
	/* available container types */
	private static List<Container_model> container_catalog = new ArrayList<Container_model>(Arrays.asList(Container_model.values()));
	/* servers' specs table */
	private static HashMap<Server_model,double[]> servTable = new HashMap<Server_model, double[]>();
	/* containers's specs table */
	private static HashMap<Container_model,double[]> contTable = new HashMap<Container_model, double []>();
	private static SecureRandom rng;
	
	private Catalog() {
	
		if(flag == false) {
			
	
			// pmax,pidle,core,freq,mem,iops,bdw, C_s
			double [][] t = {
					{245,(double)45.7,44,2200,128,128*2,10,8030},
					{419,(double)67.2,56,2500,192,192*2,10,13700},
					{469,55,56,2500,192,192*2,10,13800},
					{459,(double)58.1,56,2500,192,192*2,10,13665},
				//	{233,39,28,2500,48,48*2,10},
					{183,(double)45.6,28,2100,96,96*2,10,6907},
					{426,(double)39.5,28,2500,192,192*2,10,4567}
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
					{2,8,(double)0.45,24},
					{4,16,(double)2.7,36},			
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
	
	/**
	 * pick a random server type 
	 * @return type
	 */
	public static Server_model buyServer() {
		return server_catalog.get(rng.nextInt(server_catalog.size()));
	}
	
	/**
	 * pick a random container type between the two specified indexes
	 * @param indm min
	 * @param indM max
	 * @return type
	 */
	public static Container_model buyContainer(int indm, int indM) {
		
		return container_catalog.get(indm + rng.nextInt(indM - indm));
	}
	
	/**
	 * Retrieve the specs of a specific server type
	 * @param mod
	 * @return
	 */
	public static double[]  getServSpecs(Server_model mod) {
		if (!(flag)) new Catalog();
		return servTable.get(mod);
	}
	
	/**
	 * Retrieve the specs of a specific container type
	 * @param mod
	 * @return
	 */
	public static double[] getContSpecs(Container_model mod) {
		if (!(flag)) new Catalog();
		return contTable.get(mod);
		
	}
}
