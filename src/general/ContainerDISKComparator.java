package general;

import java.util.Comparator;

public class ContainerDISKComparator implements Comparator<Container> {

	@Override
	public int compare(Container o1, Container o2) {
		return (int)Math.signum(o2.getDisk() - o1.getDisk());
	}

}
