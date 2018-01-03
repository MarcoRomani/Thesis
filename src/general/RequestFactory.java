package general;

import java.util.ArrayList;
import java.util.HashMap;

public class RequestFactory {

	private RequestFactory() {};
	
	public static Configuration generateConfig( int customer) {
		/*
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
		*/
		Customer r = Customer.custList.get(customer);
		Container_model t1 = Catalog.buyContainer();
		int M = (int)(r.getToWAN()/Catalog.getContSpecs(t1)[2]); // 2 is the position of bdw info
		ArrayList<Container> ws = new ArrayList<Container> ();
		for(int i=0;i < M+1; i++) {
			ws.add(new Container(t1,customer));
		}
		
		Container_model t2 = Catalog.buyContainer();
		double tmp = r.getToWAN()*(1-r.getImg_coeff())*r.getWs_as_coeff();
		double tmp2 = tmp*r.getAs_dbms_coeff();
		
		int M2 = (int)(tmp2/Catalog.getContSpecs(t2)[2]);
		ArrayList<Container> as = new ArrayList<Container> ();
		for(int i=0;i < M2+1; i++) {
			as.add(new Container(t2,customer));
		}
		
		Container_model t3 = Catalog.buyContainer();
		
		

		int M3 = (int)(tmp2/Catalog.getContSpecs(t3)[2]);
		ArrayList<Container> dbms = new ArrayList<Container> ();
		for(int i=0;i < M3+1; i++) {
			dbms.add(new Container(t3,customer));
		}
		
		 HashMap<C_Couple,Double> traffic = new HashMap<C_Couple,Double>();
		 
		 for(Container c: ws) {
			 traffic.put(new C_Couple(Container.c_0,c), new Double(r.getFromWAN()/(M+1)));
			 traffic.put(new C_Couple(c,Container.c_0), new Double(r.getToWAN()/(M+1)));
		 }
		 
		// System.out.println("\n toWan = "+((float)r.getToWAN()/(M+1)));
		for(Container c1 : ws) {
			for(Container c2 : as) {
				traffic.put(new C_Couple(c1,c2), new Double((r.getFromWAN()/(M+1))/(M2+1)) );
				traffic.put(new C_Couple(c2,c1), new Double((tmp/(M+1))/(M2+1)));
			}
		}
		// System.out.println("\n tmp = "+((float)(tmp/(M+1))/(M2+1)));
		for(Container c1: as) {
			for(Container c2: dbms) {
				traffic.put(new C_Couple(c1,c2), new Double((r.getFromWAN()/(M2+1))/(M3+1)) );
				traffic.put(new C_Couple(c2,c1), new Double((tmp2/(M2+1))/(M3+1)));
			}
		}
		//System.out.println("\n tmp2 = "+((float)(tmp2/(M2+1))/(M3+1)));
		return new Configuration(ws,as,dbms,traffic);
	}
	
	
		
	}
