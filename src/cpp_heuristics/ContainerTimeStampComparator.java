package cpp_heuristics;

import java.util.Comparator;

import general.Container;

public class ContainerTimeStampComparator implements Comparator<Container> {


	@Override
	public int compare(Container o1, Container o2) {
		
		return o2.getMy_customer() - o1.getMy_customer();
	}

}
