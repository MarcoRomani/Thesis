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
		
		for(Server s: hosts) {
			s.setIn_connection(new Link(s, switches.get(0), s.getBdw_in()));
			s.setOut_connection(new Link(switches.get(0), s, s.getBdw_out()));
		}
	}

	@Override
	public int compareTo(Rack o) {
	
		return this.id - o.getId();
	}
	
	
}
