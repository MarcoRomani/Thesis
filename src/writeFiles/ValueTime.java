package writeFiles;

public class ValueTime {
	protected double value;
	protected double time;
	
	public ValueTime(double v, double t) {
		value = v;
		time = t;
	}
	
	public double getValue() {
		if(value == Double.POSITIVE_INFINITY) {
			return 999999;
		}
		return Math.abs(value);
	}
	
	public double getTime() {
		return time;
	}
}
