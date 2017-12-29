package stCPP;

import java.util.Comparator;

import general.Rack;
import general.Server;

public class RackRamComparator implements Comparator<Rack> {

	@Override
	public int compare(Rack arg0, Rack arg1) {
		// TODO Auto-generated method stub
	   double ram0 = 0;
	   double ram1 = 0;
	   for(Server s: arg0.getHosts()) {
		   ram0 += s.getResidual_mem();
	   }
	   for(Server s: arg1.getHosts()) {
		   ram1 += s.getResidual_mem();
	   }


	   return (int)Math.signum(ram1 - ram0);
	   
	}

}
