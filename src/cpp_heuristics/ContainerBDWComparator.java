package cpp_heuristics;

import java.util.Comparator;

import general.Container;

public class ContainerBDWComparator implements Comparator<Container> {

	@Override
	public int compare(Container arg0, Container arg1) {
		return (int) Math.signum(Math.max(arg1.getBdw_in(), arg1.getBdw_out()) - Math.max(arg0.getBdw_in(), arg0.getBdw_out()));
	}

}
