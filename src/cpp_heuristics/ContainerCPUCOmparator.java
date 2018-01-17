package cpp_heuristics;

import java.util.Comparator;

import general.Container;

public class ContainerCPUCOmparator implements Comparator<Container> {

	@Override
	public int compare(Container o1, Container o2) {
		// TODO Auto-generated method stub
		return (int) Math.signum(o2.getCpu()- o1.getCpu());
	}

}
