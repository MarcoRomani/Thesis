package general;

public class Container implements Comparable<Container>{

	
	public static int container_id = 0;
	public static final Container c_0 = new Container();
	private int my_id;
	private Container_model type;
	private double cpu;
	private double mem;
	private double disk;
	private double bdw_out;
	private double bdw_in;
	private int my_customer;

	protected double mu;
	protected double in_req;
	
	protected double state;
	
	private Container() {
		this.my_id = container_id;
		container_id += 1;
	}
	
	public Container(Container_model cm, int customer) {
		my_customer = customer;
		this.type = cm;
		type = cm;
		double [] specs = Catalog.getContSpecs(cm);
		cpu = specs[0];
		mem = specs[1];
		disk = specs[3];
		bdw_out = specs[2];
		bdw_in = specs[2];
		state = mem;
		
		this.my_id = container_id;
		container_id += 1;
	}

	public int getId() {
		return my_id;
	}
	
	public int getCust() {
		return my_customer;
	}

	public Container_model getType() {
		return type;
	}

//	public double getCpu() {
//		return cpu;
//	}

	public double getMem() {
		return mem;
	}

	public double getState() {
		return state;
	}
	public double getDisk() {
		return disk;
	}

	public double getBdw_out() {
		return bdw_out;
	}

	public double getBdw_in() {
		return bdw_in;
	}

	@Override
	public int compareTo(Container arg0) {
		int tmp =(arg0).getId();
		return my_id - tmp;
	}

	public int getMy_customer() {
		return my_customer;
	}

	@Override
	public String toString() {
		return "Container [my_id=" + my_id + "]";
	}
	
	public double getMu() {
		return mu;
	}

	public double getIn_req() {
		return in_req;
	}

	public void setIn_req(double in_req) {
		this.in_req = in_req;
	}

	

	public void setMu(double mu) {
		this.mu = mu;
	}
	
}
