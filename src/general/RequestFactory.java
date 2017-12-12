package general;

import java.util.ArrayList;
import java.util.HashMap;

public class RequestFactory {

	private RequestFactory() {};
	
	public static Configuration generateConfig(float in, float out, int customer) {
		int M=0;
		float R=1000;
		Container_model t=null;
		for(Container_model cm: Container_model.values()) {
			float [] spec = Catalog.getContSpecs(cm);
			int m = (int)(out/spec[2]);
			float r = out%spec[2];
			if(m > M || (m == M && r < R)) 
			     {
				    t = cm;
				    M = m;
				    R = r;
				 }
			
		}
		
		ArrayList<Container> ws = new ArrayList<Container> ();
		for(int i=0;i < M+1; i++) {
			ws.add(new Container(t,customer));
		}
		
		Container_model t2 = Catalog.buyContainer();
		int M2 = (int)(out/Catalog.getContSpecs(t2)[2]);
		ArrayList<Container> as = new ArrayList<Container> ();
		for(int i=0;i < M2+1; i++) {
			as.add(new Container(t2,customer));
		}
		
		Container_model t3 = Catalog.buyContainer();
		int M3 = (int)(Catalog.getContSpecs(t3)[2]);
		ArrayList<Container> dbms = new ArrayList<Container> ();
		for(int i=0;i < M3+1; i++) {
			dbms.add(new Container(t3,customer));
		}
		
		// float [][] traffic = new float[M+M2+M3+3][M+M2+M3+3];
		 HashMap<C_Couple,Float> traffic = new HashMap<C_Couple,Float>();
		 
		 for(Container c: ws) {
			 traffic.put(new C_Couple(Container.c_0,c), new Float(in/(M+1)));
			 traffic.put(new C_Couple(c,Container.c_0), new Float(out/(M+1)));
		 }
	
		for(Container c1 : ws) {
			for(Container c2 : as) {
				traffic.put(new C_Couple(c1,c2), new Float((in/(M+1))/(M2+1)) );
				traffic.put(new C_Couple(c2,c1), new Float((out/(M+1))/(M2+1)));
			}
		}
	
		for(Container c1: as) {
			for(Container c2: dbms) {
				traffic.put(new C_Couple(c1,c2), new Float((in/(M2+1))/(M3+1)) );
				traffic.put(new C_Couple(c2,c1), new Float((out/(M2+1))/(M3+1)));
			}
		}
		return new Configuration(ws,as,dbms,traffic);
	}
	
	
		
	}
