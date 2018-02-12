package general;

public abstract class CPUcalculator {

	
	public static double utilization(Container c, Server s) {
				
		return s.getCpu()* fractionalUtilization(c,s);
		
	}
	
	
	public static double unnorm_utilization(Container c) {
		return c.getIn_req() / c.getMu();
	}
	
	public static double fractionalUtilization(Container c, Server s) {
		double c_s = s.getCs();
		double mu = c.getMu();
		double X = c.getIn_req();
		return (X / (mu*c_s));
	}
}
