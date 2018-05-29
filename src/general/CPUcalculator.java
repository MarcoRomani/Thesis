package general;

/**
 * Utilities used to compute CPU utilization
 * 
 * @author Marco
 *
 */
public abstract class CPUcalculator {

	/**
	 * Utilization as defined in the MILP formulation
	 * 
	 * @param c   container
	 * @param s   server
	 * @return CPU utilization
	 */
	public static double utilization(Container c, Server s) {

		return s.getCpu() * fractionalUtilization(c, s);

	}

	/**
	 * Quantity depending only on the related VM/container
	 * @param c container
	 * @return utilization
	 */
	public static double unnorm_utilization(Container c) {
		return c.getIn_req() / c.getMu();
	}

	/**
	 * Utilization normalized between 0 and 1
	 * @param c container
	 * @param s server
	 * @return utilization
	 */
	public static double fractionalUtilization(Container c, Server s) {
		double c_s = s.getCs();
		double mu = c.getMu();
		double X = c.getIn_req();
		return (X / (mu * c_s));
	}
}
