package general;

import java.util.ArrayList;

public class FatTreeRack extends Rack {

	public FatTreeRack(Switch edge, int num_server, Server_model s_type) {
		this.servers_number = num_server;
		this.servers_type = s_type;
		this.hosts = new ArrayList<Server>();
		this.switches = new ArrayList<Switch> ();
		this.switches.add(edge);
		this.id = Rack.rack_id;
		Rack.rack_id += 1;
		this.build();
	}

	@Override
	protected void build() {
		
		for( int i=0; i < this.servers_number ; i++){
			this.hosts.add(new Server(servers_type));
		}
		
		// TODO links connection
	}
	
	
}
