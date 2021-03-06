package general;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Representation of an application: containers grouped into tiers and a traffic matrix 
 * @author Marco
 *
 */
public class Configuration {
	private ArrayList<Container> c_ws = new ArrayList<Container> ();
    private	ArrayList<Container> c_as = new ArrayList<Container> ();
	private ArrayList<Container> c_dbms = new ArrayList<Container> ();
	private HashMap<C_Couple,Double> c_tr;
	
	public Configuration(ArrayList<Container> w, ArrayList<Container> a, ArrayList<Container> d,  HashMap<C_Couple,Double> t) {
		c_ws = w;
		c_as = a;
		c_dbms = d;
		c_tr = t;
	}

	public ArrayList<Container> getWs() {
		return c_ws;
	}

	public ArrayList<Container> getAs() {
		return c_as;
	}

	public ArrayList<Container> getDbms() {
		return c_dbms;
	}

	public  HashMap<C_Couple, Double> getTr() {
		return c_tr;
	}
}

