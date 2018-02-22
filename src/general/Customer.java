package general;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Customer {

	public static double conversion = 1000000;
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
	
	
	
	private HashMap<C_Couple, Double> traffic;
	private double fromWAN; // input
	private double toWAN; // global output

	private double ws_as_coeff; // data/html(no image)
	private double as_dbms_coeff; // data filtering
	private double img_coeff; // img / whole page
	private double mult; // output(no image) / input

	protected double img_req_in;
	protected double html_req_in;
	protected double data_req_in;

	public Customer(double workload, Business type, SecureRandom rng) {

		id = cust_id;
		cust_id += 1;
		fromWAN = (workload / conversion) * 8;

		switch (type) {
		case Banking:
			mult = 30;
			ws_as_coeff = (double) 0.1 + (((rng.nextInt(100) + 1) / 100) * (double) 0.14 + (double) 0.01);
			as_dbms_coeff = (double) 10 * (rng.nextInt(100) + 1) / 100;
			img_coeff = (double) 0.3 + ((rng.nextInt(100) + 1) / 100) * (double) 0.2;

			break;
		case Ecommerce:
			mult = 140;
			ws_as_coeff = ((rng.nextInt(100) + 1) / 100) * (double) 0.14 + (double) 0.01;
			as_dbms_coeff = (double) 1 / (double) (rng.nextInt(20) + 1);
			img_coeff = (double) 0.5 + ((rng.nextInt(100) + 1) / 100) * (double) 0.15;

			break;

		}
		double tmp = (img_coeff / 535) * (mult / (1 - img_coeff));
		html_req_in = workload / (1 + tmp);
		img_req_in = tmp * html_req_in;
		toWAN = (html_req_in / conversion) * 8 * mult + (img_req_in / conversion) * 8 * 535;
		data_req_in = html_req_in * ws_as_coeff;

		custList.add(this);
		Configuration conf = RequestFactory.generateConfig(id);
		web_servers = conf.getWs();
		app_servers = conf.getAs();
		dbms = conf.getDbms();
		containers.addAll(web_servers);
		containers.addAll(app_servers);
		containers.addAll(dbms);
		traffic = conf.getTr();

		updateContainerTraffics();
	}

	public void addWS() {
		Container_model cm = (web_servers.size() != 0) ? web_servers.get(0).getType() : new_ws.get(0).getType();
		Container neW = new Container(cm, id);
		neW.setMu(RequestFactory.mu_web);
		new_containers.add(neW);
		new_ws.add(neW);

		ArrayList<Container> all_ws = new ArrayList<Container>();
		all_ws.addAll(web_servers);
		all_ws.addAll(new_ws);

		ArrayList<Container> all_as = new ArrayList<Container>();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);

		traffic.put(new C_Couple(Container.c_0, neW), new Double(0));
		traffic.put(new C_Couple(neW, Container.c_0), new Double(0));

		for (Container c : all_ws) {
			traffic.replace(new C_Couple(Container.c_0, c), new Double(fromWAN / all_ws.size()));
			traffic.replace(new C_Couple(c, Container.c_0), new Double(toWAN / all_ws.size()));
		}

		for (Container c : all_as) {
			traffic.put(new C_Couple(neW, c), new Double(0));
			traffic.put(new C_Couple(c, neW), new Double(0));
		}

		for (Container c1 : all_ws) {
			for (Container c2 : all_as) {
				traffic.replace(new C_Couple(c1, c2),
						new Double((((data_req_in / conversion) * 8) / all_ws.size()) / all_as.size()));

				traffic.replace(new C_Couple(c2, c1),
						new Double(((toWAN * (1 - img_coeff) * ws_as_coeff) / all_ws.size()) / all_as.size()));
			}
		}
		
		updateContainerTraffics();
	}

	public void addAS() {
		Container_model cm = (app_servers.size() != 0) ? app_servers.get(0).getType() : new_as.get(0).getType();
		Container neW = new Container(cm, id);
		neW.setMu(RequestFactory.mu_app);

		new_containers.add(neW);
		new_as.add(neW);

		ArrayList<Container> all_ws = new ArrayList<Container>();
		all_ws.addAll(web_servers);
		all_ws.addAll(new_ws);

		ArrayList<Container> all_as = new ArrayList<Container>();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);

		ArrayList<Container> all_dbms = new ArrayList<Container>();
		all_dbms.addAll(dbms);
		all_dbms.addAll(new_dbms);

		for (Container c : all_ws) {
			traffic.put(new C_Couple(c, neW), new Double(0));
			traffic.put(new C_Couple(neW, c), new Double(0));
		}
		for (Container c : all_dbms) {
			traffic.put(new C_Couple(neW, c), new Double(0));
			traffic.put(new C_Couple(c, neW), new Double(0));
		}

		for (Container c1 : all_ws) {
			for (Container c2 : all_as) {
				traffic.replace(new C_Couple(c1, c2),
						new Double((((data_req_in / conversion) * 8) / all_ws.size()) / all_as.size()));

				traffic.replace(new C_Couple(c2, c1),
						new Double(((toWAN * (1 - img_coeff) * ws_as_coeff) / all_ws.size()) / all_as.size()));
			}
		}

		for (Container c1 : all_as) {
			for (Container c2 : all_dbms) {
				traffic.replace(new C_Couple(c1, c2), new Double(
						(((data_req_in / conversion) * 8 * as_dbms_coeff) / all_as.size()) / all_dbms.size()));
				traffic.replace(new C_Couple(c2, c1), new Double(
						((toWAN * (1 - img_coeff) * ws_as_coeff * as_dbms_coeff) / all_as.size()) / all_dbms.size()));
			}
		}

		updateContainerTraffics();

	}

	public void addDBMS() {
		Container_model cm = (dbms.size() != 0) ? dbms.get(0).getType() : new_dbms.get(0).getType();
		Container neW = new Container(cm, id);
		neW.setMu(RequestFactory.mu_db);

		new_containers.add(neW);
		new_dbms.add(neW);

		ArrayList<Container> all_dbms = new ArrayList<Container>();
		all_dbms.addAll(dbms);
		all_dbms.addAll(new_dbms);

		ArrayList<Container> all_as = new ArrayList<Container>();
		all_as.addAll(app_servers);
		all_as.addAll(new_as);

		for (Container c : all_as) {
			traffic.put(new C_Couple(c, neW), new Double(0));
			traffic.put(new C_Couple(neW, c), new Double(0));
		}

		for (Container c1 : all_as) {
			for (Container c2 : all_dbms) {
				traffic.replace(new C_Couple(c1, c2), new Double(
						(((data_req_in / conversion) * 8 * as_dbms_coeff) / all_as.size()) / all_dbms.size()));
				traffic.replace(new C_Couple(c2, c1), new Double(
						((toWAN * (1 - img_coeff) * ws_as_coeff * as_dbms_coeff) / all_as.size()) / all_dbms.size()));
			}
		}
		updateContainerTraffics();

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

	public ArrayList<Container> getNewWS() {
		return this.new_ws;
	}

	public ArrayList<Container> getNewAS() {
		return this.new_as;
	}

	public ArrayList<Container> getNewDBMS() {
		return this.new_dbms;
	}

	public ArrayList<Container> getNewContainers() {
		return this.new_containers;
	}

	
	
	public HashMap<C_Couple, Double> getTraffic() {
		return traffic;
	}

	public double getFromWAN() {
		return fromWAN;
	}

	public double getToWAN() {
		return toWAN;
	}

	public static int getCust_id() {
		return cust_id;
	}

	public static ArrayList<Customer> getCustList() {
		return custList;
	}

	public double getWs_as_coeff() {
		return ws_as_coeff;
	}

	public double getAs_dbms_coeff() {
		return as_dbms_coeff;
	}

	public double getImg_coeff() {
		return img_coeff;
	}

	public double getMult() {
		return mult;
	}

	public double getDataReq() {
		return data_req_in;
	}

	public void updateContainerTraffics() {
		
		// CALCOLA IN_REQUESTS
		
	    for(Container c: web_servers) {
	    	double workload = 0;
	    	
	    	workload += (traffic.get(new C_Couple(Container.c_0,c)) == null)? 0 : (traffic.get(new C_Couple(Container.c_0,c))*Customer.conversion)/8;
	    	c.setIn_req(workload);
	    }
	    
	    
	    for(Container c: new_ws) {
	    	double workload = 0;
	    	
	    	workload += (traffic.get(new C_Couple(Container.c_0,c)) == null)? 0 : (traffic.get(new C_Couple(Container.c_0,c))*Customer.conversion)/8;
	    	c.setIn_req(workload);
	    }

	    for(Container c: app_servers) {
	    	double workload = 0;
	    	for(Container v: web_servers) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	for(Container v: new_ws) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	c.setIn_req(workload);
	    }
	    for(Container c: new_as) {
	    	double workload = 0;
	    	for(Container v: web_servers) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	for(Container v: new_ws) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	c.setIn_req(workload);
	    }
	    for(Container c: dbms) {
	    	double workload = 0;
	    	for(Container v: app_servers) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	for(Container v: new_as) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	c.setIn_req(workload);
	    }
	    for(Container c: new_dbms) {
	    	double workload = 0;
	    	for(Container v: app_servers) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	for(Container v: new_as) {
	    		workload += (traffic.get(new C_Couple(v,c)) == null)? 0 : (traffic.get(new C_Couple(v,c))*Customer.conversion)/8;
	    	}
	    	c.setIn_req(workload);
	    }
	}

}
