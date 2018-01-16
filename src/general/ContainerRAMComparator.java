package general;

import java.util.Comparator;

public class ContainerRAMComparator implements Comparator<Container>{

	@Override
	public int compare(Container arg0, Container arg1) {
		return (int) Math.signum(arg1.getMem() - arg0.getMem());
	}

}
