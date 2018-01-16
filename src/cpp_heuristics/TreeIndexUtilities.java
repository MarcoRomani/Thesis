package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.*;

public abstract class TreeIndexUtilities {

	public static List<Server> feasibleServersApprox(List<Container> requests, List<Server> machines) {

		ArrayList<Server> feasib= new ArrayList<Server>();
		ArrayList<Container> ramorder = new ArrayList<Container>();
		ramorder.addAll(requests);
		ramorder.sort(new ContainerRAMComparator());
		ArrayList<Container> diskorder = new ArrayList<Container>();
		diskorder.addAll(requests);
		diskorder.sort(new ContainerDISKComparator());

		for (Server s : machines) {
			if(s.isUnderUtilized() && s.getResidual_mem() >= ramorder.get(ramorder.size()-1).getMem() &&
					s.getResidual_disk() >= diskorder.get(diskorder.size()-1).getDisk()) {
				feasib.add(s);
			}
		}
		return feasib;
	}

	public static TreeIndex createCPUIndex(List<Server> machines) {

		TreeIndex tree = new TreeIndex();
		for(Server s: machines) {
			tree.insert(s.getResidual_cpu(), s.getId());
		}
		
		return tree;
	}
	
	public static TreeIndex createRAMIndex(List<Server> machines) {
		TreeIndex tree = new TreeIndex(); 
		for(Server s:machines) {
			tree.insert(s.getResidual_mem(), s.getId());
		}
		return tree;
	}
	
	public static TreeIndex createDISKIndex(List<Server> machines) {
		TreeIndex tree = new TreeIndex();
		for(Server s: machines) {
			tree.insert(s.getResidual_disk(), s.getId());
		}
		return tree;
	}
}
