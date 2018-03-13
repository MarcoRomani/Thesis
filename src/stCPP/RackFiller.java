package stCPP;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import general.*;
import general.DataCenter;
import general.Pod;
import general.Rack;
import general.Server;

public class RackFiller implements DC_filler {

	protected SecureRandom rng;
	
	public RackFiller(SecureRandom rng) {
		this.rng = rng;
	}
	
	@Override
	public void populate(DataCenter dc, List<Customer> req, float tolerance) throws PopulateException {

		ArrayList<Rack> racks = new ArrayList<Rack>();
		for(Pod p: dc.getPods()) {
			racks.addAll(p.getRacks());
		}
		
		RackRamComparator comp = new RackRamComparator();
		racks.sort(comp);
		boolean flag = true;
		ArrayList<Container> ws_rest = new ArrayList<Container>();
		ArrayList<Container> as_rest = new ArrayList<Container>();
		ArrayList<Container> dbms_rest = new ArrayList<Container>();
		Date d1 = null;
		Date d2 = null;
		for(int n=0;n < req.size();n++) {
		
			if(Main.display) {
				System.out.println("TRY TO ALLOCATE CUSTOMER: \t"+req.get(n).getId());
			}
			 ArrayList<Container> ws = new ArrayList<Container>();
			 ArrayList<Container> as = new ArrayList<Container>();
			 ArrayList<Container> dbms = new ArrayList<Container>();
			 
		  if(flag) {
		  
		   d1 = new Date();
		   ws.addAll(req.get(n).getWeb_servers());		 
		   as.addAll(req.get(n).getApp_servers());		
		   dbms.addAll(req.get(n).getDbms());
		   
		  }else {
			  d2 = new Date();
			  if(d2.getTime() - d1.getTime() > 10000) {
				  throw new PopulateException();
			  }
			  ws.addAll(ws_rest);
			  ws_rest.clear();
			  as.addAll(as_rest);
			  as_rest.clear();
			  dbms.addAll(dbms_rest);
			  dbms_rest.clear();
			  
			  flag = true;
		  }
		    int j= 0;
			int k= 0;
			ArrayList<Server> list = racks.get(rng.nextInt(racks.size()/2)).getHosts();
			// fill the servers with ws,as and dbms alternated
			while(ws.size()+as.size()+dbms.size() > 0 && j < list.size() ) {
				if(ws.size() > 0 && list.get(j).canBePlaced(ws.get(0), tolerance)) {
					
					list.get(j).allocateContainer(ws.get(0));
					dc.getPlacement().put(ws.remove(0), list.get(j));
					list.get(j).updateBandwidth();
					
				}else { k+=1; }
				if(as.size() > 0 && list.get(j).canBePlaced(as.get(0), tolerance)) {


					list.get(j).allocateContainer(as.get(0));
					dc.getPlacement().put(as.remove(0), list.get(j));
					list.get(j).updateBandwidth();

				}else { k+= 1;}
				if(dbms.size() > 0 && list.get(j).canBePlaced(dbms.get(0), tolerance)) {
					

					list.get(j).allocateContainer(dbms.get(0));
					dc.getPlacement().put(dbms.remove(0), list.get(j));
					list.get(j).updateBandwidth();

				}else { k+=1; }
				
				if(k > 2) {
					j+=1;
					k=0;
				}
				
			}
			
			if(ws.size() + as.size() +dbms.size() == 0) {
				flag = true;
			}else {
				ws_rest.addAll(ws);
				as_rest.addAll(as);
				dbms_rest.addAll(dbms);
				flag = false;
				n -= 1;
			}
		
		  racks.sort(comp);
		}
		
		for(Rack r: racks) {
			for(Server s: r.getHosts()) {
				s.updateBandwidth();
			}
		}
	}

}
