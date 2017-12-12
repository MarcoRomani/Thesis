package general;

import java.util.ArrayList;

public abstract class Rack {

	public static int rack_id = 0;
	protected int id;
	protected int servers_number;
	protected Server_model servers_type;
	protected ArrayList<Server> hosts;
	protected ArrayList<Switch> switches;
	
	
	protected abstract void build();


	public int getId() {
		return id;
	}


	public int getServers_number() {
		return servers_number;
	}


	public Server_model getServers_type() {
		return servers_type;
	}


	public ArrayList<Server> getHosts() {
		return hosts;
	}


	public ArrayList<Switch> getSwitches() {
		return switches;
	}
}
