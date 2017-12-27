package general;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

public class Customer {

	public static int cust_id = 0;
	public static ArrayList<Customer> custList = new ArrayList<Customer>();
	private int id;
	
	private ArrayList<Container> containers = new ArrayList<Container>();
	private ArrayList<Container> web_servers;
	private ArrayList<Container> app_servers;
	private ArrayList<Container> dbms;
	private ArrayList<Container> new_containers = new ArrayList<Container>();
	private ArrayList<Container> new_ws = new ArrayList<Container>();
	private ArrayList<Container> new_as = new ArrayList<Container>();
	private ArrayList<Container> new_dbms = new ArrayList<Container>();

	
	private HashMap<C_Couple,Float> traffic;
	// private float[][] traffic;
	private float fromWAN;
	private float toWAN;
	
	private float ws_as_coeff;
	private float as_dbms_coeff;
	private float img_coeff;
	private float mult;
	
	public Customer(float workload, Business type, SecureRandom rng) {
		
		id = cust_id;
		cust_id += 1;
		fromWAN = workload;
		
		
		switch (type) {
			case Banking:
				mult = 30;
				ws_as_coeff = (float)0.1+rng.nextFloat()*(float)0.15;
				as_dbms_coeff = (float)10*rng.nextFloat();
				img_coeff =(float)0.3+ rng.nextFloat()*(float)0.2;
			case Ecommerce:
				mult = 140;
				ws_as_coeff = rng.nextFloat()*(float)0.15;
				as_dbms_coeff = (float)10*rng.nextFloat();
				img_coeff = (float)0.5+ rng.nextFloat()*(float)0.15;
			case Support:
				mult = 530;
		}	
		toWAN = (mult*fromWAN)/(1-img_coeff);
		
		Configuration conf = RequestFactory.generateConfig( id);
		web_servers = conf.getWs();
		app_servers = conf.getAs();
		dbms = conf.getDbms();
		containers.addAll(web_servers);
		containers.addAll(app_servers);
		containers.addAll(dbms);
		traffic = conf.getTr();
		
		custList.add(this);
	}
	
	
	public void addWS() {
		Container_model cm = web_servers.get(0).getType();
		Container neW = new Container(cm,id);
		new_containers.add(neW);
		new_ws.add(neW);
		
		ArrayList<Container> all_ws = new ArrayList<Container> ();
		all_ws.addAll(web_servers);
		all_ws.addAll(new_ws);
		
		ArrayList<Container> all_as = new ArrayList<Container> ();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);
		
		traffic.put(new C_Couple(Container.c_0,neW), new Float(0));
		traffic.put(new C_Couple(neW,Container.c_0), new Float(0));
		
		for(Container c: all_ws) {
			traffic.replace(new C_Couple(Container.c_0,c),new Float(fromWAN/all_ws.size()));
			traffic.replace(new C_Couple(c,Container.c_0), new Float(toWAN/all_ws.size()));
		}
		
		for(Container c: all_as) {
			traffic.put(new C_Couple(neW,c), new Float(0));
			traffic.put(new C_Couple(c,neW), new Float(0));
		}
		
		for(Container c1 : all_ws) {
			for(Container c2 : all_as) {
				traffic.replace(new C_Couple(c1,c2), new Float((fromWAN/all_ws.size())/
						all_as.size()));
				
				traffic.replace(new C_Couple(c2,c1), new Float(((toWAN*(1-img_coeff)*ws_as_coeff)/all_ws.size())/
						all_as.size()));
			}
		}
	}
	
	
	public void addAS() {
		Container_model cm = app_servers.get(0).getType();
		Container neW = new Container(cm,id);
		
		new_containers.add(neW);
		new_as.add(neW);
		
		ArrayList<Container> all_ws = new ArrayList<Container> ();
		all_ws.addAll(web_servers);
		all_ws.addAll(new_ws);
		
		ArrayList<Container> all_as = new ArrayList<Container> ();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);
		
		ArrayList<Container> all_dbms = new ArrayList<Container> ();
		all_dbms.addAll(dbms);
		all_dbms.addAll(new_dbms);
		
		for(Container c: all_ws) {
			traffic.put(new C_Couple(c,neW), new Float(0));
			traffic.put(new C_Couple(neW,c), new Float(0));
		}
		for(Container c: all_dbms) {
			traffic.put(new C_Couple(neW,c), new Float(0));
			traffic.put(new C_Couple(c,neW), new Float(0));
		}
		
		for(Container c1 : all_ws) {
			for(Container c2 : all_as) {
				traffic.replace(new C_Couple(c1,c2), new Float((fromWAN/all_ws.size())/
						all_as.size()));
				
				traffic.replace(new C_Couple(c2,c1), new Float(((toWAN*(1-img_coeff)*ws_as_coeff)/all_ws.size())/
						all_as.size()));
			}
		}
		
		for(Container c1 : all_as) {
			for(Container c2 : all_dbms) {
				traffic.replace(new C_Couple(c1,c2), new Float((fromWAN/all_as.size())/all_dbms.size()));
				traffic.replace(new C_Couple(c2,c1), new Float(((toWAN*(1-img_coeff)*ws_as_coeff*as_dbms_coeff)/all_as.size())/all_dbms.size()));
			}
		}
		
		
	}
	
	public void addDBMS() {
		Container_model cm = dbms.get(0).getType();
		Container neW = new Container(cm,id);
		
		new_containers.add(neW);
		new_dbms.add(neW);
		
		ArrayList<Container> all_dbms = new ArrayList<Container> ();
		all_dbms.addAll(dbms);
		all_dbms.addAll(new_dbms);
		
		ArrayList<Container> all_as = new ArrayList<Container> ();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);
		
		for(Container c : all_as) {
			traffic.put(new C_Couple(c,neW), new Float(0));
			traffic.put(new C_Couple(neW,c), new Float(0));
		}
		
		for(Container c1 : all_as) {
			for(Container c2 : all_dbms) {
				traffic.replace(new C_Couple(c1,c2), new Float((fromWAN/all_as.size())/all_dbms.size()));
				traffic.replace(new C_Couple(c2,c1), new Float(((toWAN*(1-img_coeff)*ws_as_coeff*as_dbms_coeff)/all_as.size())/all_dbms.size()));
			}
		}
	}
	
	public void transformIntoNew() {
		new_containers.addAll(containers);
		new_ws.addAll(web_servers);
		new_as.addAll(app_servers);
		new_dbms.addAll(dbms);
		
		containers.clear();
		web_servers.clear();
		app_servers.clear();
		dbms.clear();
	}

	public int getId() {
		return id;
	}

	public ArrayList<Container> getContainers() {
		return containers;
	}

	public ArrayList<Container> getWeb_servers() {
		return web_servers;
	}

	public ArrayList<Container> getApp_servers() {
		return app_servers;
	}

	public ArrayList<Container> getDbms() {
		return dbms;
	}

	public ArrayList<Container> getNewWS(){
		return this.new_ws;
	}
	
	public ArrayList<Container> getNewAS(){
		return this.new_as;
	}
	
	public ArrayList<Container> getNewDBMS(){
		return this.new_dbms;
	}
	
	public ArrayList<Container> getNewContainers(){
		return this.new_containers;
	}
	public HashMap<C_Couple,Float> getTraffic() {
		return traffic;
	}

	public float getFromWAN() {
		return fromWAN;
	}

	public float getToWAN() {
		return toWAN;
	}


	public static int getCust_id() {
		return cust_id;
	}


	public static ArrayList<Customer> getCustList() {
		return custList;
	}


	public float getWs_as_coeff() {
		return ws_as_coeff;
	}


	public float getAs_dbms_coeff() {
		return as_dbms_coeff;
	}


	public float getImg_coeff() {
		return img_coeff;
	}


	public float getMult() {
		return mult;
	}
}
