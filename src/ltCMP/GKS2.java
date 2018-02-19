package ltCMP;

import java.util.ArrayList;
import java.util.List;

import general.CPUcalculator;
import general.C_Couple;
import general.Container;
import general.Customer;

public class GKS2 extends GreedyInverseKnapsack {

	@Override
	protected List<Container> selectContainers(double threshold) {
		
		List<Container> results = new ArrayList<Container>();
		
		while (partial < threshold * s.getCpu()) {
			
			Container chosen = null;
			double best_gain = Double.NEGATIVE_INFINITY;
			for(Container v: toProcess) {
				double tmp =gain(v,results);
				if( tmp > best_gain) {
					chosen = v;
					best_gain = tmp;
				}
			}
			
			toProcess.remove(chosen);
			results.add(chosen);
			partial += CPUcalculator.utilization(chosen, s);
			
		}
		return results;
	}
	
	
	protected double gain(Container vm, List<Container> group) {
		Customer r = Customer.custList.get(vm.getMy_customer());
		double gain = 0;
		
		for(Container c2 : group) {
			if(vm.getMy_customer() != c2.getMy_customer()) continue;
			
			Double t1 = r.getTraffic().get(new C_Couple(vm,c2));
			Double t2 = r.getTraffic().get(new C_Couple(c2,vm));
			
			gain += (t1 == null)? 0 : t1.doubleValue();
			gain += (t2 == null)? 0 :t2.doubleValue();
			
		}
		
		return gain;
		
	}
}
	
