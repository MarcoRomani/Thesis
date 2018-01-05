package heuristics;

import java.util.Comparator;

import general.Container;

public class ContainerRamComparator implements Comparator<Container> {

	@Override
	public int compare(Container arg0, Container arg1) {
		
		return (int)Math.signum(arg1.getMem() - arg0.getMem());
	}

}
