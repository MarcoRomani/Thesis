package general;

/**
 * Representation of a VM/container, mostly a collection of parameters
 * @author Marco
 *
 */
public class Container implements Comparable<Container>{


	/**	  constant for bytes to bits conversion */
	public static double BIT_CONVERSION = 8;
	public static int container_id = 0;
	/** dummy container representing Internet */
	public static final Container c_0 = new Container();
	private int my_id;
	private Container_model type;
	private double cpu;
	private double mem;
	private double disk;
	private double bdw_out;
	private double bdw_in;
	private int my_customer;

	/* coefficient used to compute utilization */
	protected double mu;
	/*  number of incoming requests */
	protected double in_req;
	/* size of VM used for migration */
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
		state = mem*BIT_CONVERSION;
		
		this.my_id = container_id;
		container_id += 1;
	}

	public int getId() {
		return my_id;
	}
	
	/**
	 * get identifier of the related customer
	 * @return identifier of the related customer
	 */
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

	/**
	 * comparison based on identifier
	 */
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
