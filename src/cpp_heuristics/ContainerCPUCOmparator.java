package cpp_heuristics;

import java.util.Comparator;

import general.CPUcalculator;
import general.Container;

public class ContainerCPUComparator implements Comparator<Container> {

	@Override
	public int compare(Container o1, Container o2) {
		// TODO Auto-generated method stub
		return (int) Math.signum(CPUcalculator.unnorm_utilization(o2)- CPUcalculator.unnorm_utilization(o1));
	}

}
