package general;

public abstract class CPUcalculator {

	
	public static double utilization(Container c, Server s) {
		double c_s = s.getCs();
		double mu = c.getMu();
		double X = c.getIn_req();
		
		return s.getCpu()*(X / (mu*c_s));
		
	}
	
	
	public static double unnorm_utilization(Container c) {
		return c.getIn_req() / c.getMu();
	}
}