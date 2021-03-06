package general;

import java.util.*;

/**
 * Representation of a server, mostly parameters and allocated containers
 * */
public class Server extends Node implements Comparable<Server> {

	/** threshold for a server that can be still loaded*/
	public static double underUtilization_constant = 0.4;
	/** threshold for a server  that is above the desired load */
	public static double overUtilization_constant = 0.2;
	/** threshold for a server considered almost empty */
	public static double almostEmpty_constant = 0.90;
	public static double baseFrequency = 2500;
	public static int server_id = 0;
	private int id;
	private Server_model type;
	private double cpu;
	private double residual_cpu;
	private double mem;
	private double residual_mem;
	private double disk;
	private double residual_disk;
	private double bdw_out;
	private double residual_bdw_out;
	private double bdw_in;
	private double residual_bdw_in;

	/* maximum throughput */
	protected double C_s;

	private List<Container> containers = new ArrayList<Container>();

	protected double p_max;
	protected double p_idle;
	/* ON/OFF state */
	protected boolean state = false;

	protected Link in_connection;
	protected Link out_connection;
	

	public Server(Server_model sm) {
		
		super();
		
		this.type = sm;
		double[] specs = Catalog.getServSpecs(sm);
		cpu = specs[2];
		residual_cpu = cpu;
		mem = specs[4];
		residual_mem = mem;
		disk = specs[5];
		residual_disk = disk;
		bdw_out = specs[6];
		bdw_in = specs[6];
		residual_bdw_out = bdw_out;
		residual_bdw_in = bdw_in;
	//	frequency = specs[3];

		C_s = specs[7];
		p_max = specs[0];
		p_idle = specs[1];
		this.id = server_id;
		server_id += 1;
	}

	/**
	 * check if a container can be placed on this server without exceeding a specified threshold of every resource
	 * @param c
	 * @param tol
	 * @return
	 */
	public boolean canBePlaced(Container c, float tol) {

		if (tol < 0 || tol > 1)
			return false;
		float t = 1 - tol;

		return (this.residual_cpu - CPUcalculator.utilization(c, this) >= t * this.cpu && this.residual_mem - c.getMem() >= t * this.mem
				&& this.residual_disk - c.getDisk() >= t * this.disk
				&& this.residual_bdw_out - c.getBdw_out() >= t * this.bdw_out
				&& this.residual_bdw_in - c.getBdw_in() >= t * this.bdw_in);

	}

	/**
	 * Place a container on this server and update the capacities
	 * @param c
	 */
	public void allocateContainer(Container c) {
		this.containers.add(c);
		this.residual_cpu -= CPUcalculator.utilization(c, this);// * (float) (2500 / frequency);
		this.residual_mem -= c.getMem();
		this.residual_disk -= c.getDisk();
		this.residual_bdw_out -= c.getBdw_out();
		this.residual_bdw_in -= c.getBdw_in();
		state = true;
	}

	/**
	 * Remove a container from this server and update the capacities
	 * @param c
	 */
	public void deallocateContainer(Container c) {
		this.containers.remove(c);
		this.residual_cpu += CPUcalculator.utilization(c, this); // * (float) (2500 / frequency);
		this.residual_mem += c.getMem();
		this.residual_disk += c.getDisk();
		this.residual_bdw_out += c.getBdw_out();
		this.residual_bdw_in += c.getBdw_in();
		if (containers.isEmpty())
			state = false;
	}

	/**
	 * Carefully updates the bandwidth capacity
	 */
	public void updateBandwidth() {

		double usedBDWin = 0;
		double usedBDWout = 0;
		for (Container c1 : containers) {
			HashMap<C_Couple, Double> tr = Customer.custList.get(c1.getCust()).getTraffic();
			ArrayList<Container> list = Customer.custList.get(c1.getCust()).getContainers();
			list.add(Container.c_0);
			for (Container c2 : list) {
				if (!(this.isIn(c2)) && !(tr.get(new C_Couple(c1, c2)) == null)) {
					usedBDWout += tr.get(new C_Couple(c1, c2)).doubleValue();
					if (!(tr.get(new C_Couple(c2, c1)) == null)) {
						usedBDWin += tr.get(new C_Couple(c2, c1)).doubleValue();
					}
				}
			}
			list.remove(list.size() - 1); // togli c_0
		}

		this.residual_bdw_in = this.bdw_in - usedBDWin;
		this.residual_bdw_out = this.bdw_out - usedBDWout;
	}

	// GETTERS

	public Server_model getType() {
		return type;
	}

	public double getCpu() {
		return cpu;
	}

	public double getResidual_cpu() {
		return Math.max(0, residual_cpu);
	}

	public double getMem() {
		return mem;
	}

//	public double getFrequency() {
//		return frequency;
//	}

	public double getResidual_mem() {
		return Math.max(0, residual_mem);
	}

	public double getDisk() {
		return disk;
	}

	public double getResidual_disk() {
		return Math.max(0, residual_disk);
	}

	public double getBdw_out() {
		return bdw_out;
	}

	public double getResidual_bdw_out() {
		return Math.max(0, residual_bdw_out);
	}

	public double getBdw_in() {
		return bdw_in;
	}

	public double getResidual_bdw_in() {
		return Math.max(0, residual_bdw_in);
	}

	public List<Container> getContainers() {
		return containers;
	}

	@Override
	public int getId() {
		return id;
	}

	public boolean isIn(Container c) {
		return containers.contains(c);
	}

	@Override
	public String toString() {
		return Integer.toString(this.id); /* + ": [modello =" + type.toString() + "res_cpu =" + residual_cpu + ", res_mem ="
				+ residual_mem + ", res_disk =" + residual_disk + ",res_out =" + residual_bdw_out + ", res_in ="
				+ residual_bdw_in + "]";*/

	}

	/**
	 * Comparison with another server based on IDs
	 */
	@Override
	public int compareTo(Server o) {
		return this.id - o.getId();
	}

	public Link getIn_connection() {
		return in_connection;
	}

	public void setIn_connection(Link in_connection) {
		this.in_connection = in_connection;
	}

	public Link getOut_connection() {
		return out_connection;
	}

	public void setOut_connection(Link out_connection) {
		this.out_connection = out_connection;
	}

	public boolean isUnderUtilized() {
		return (residual_cpu > underUtilization_constant * cpu);
	}
	
	public boolean isOverUtilized() {
		return (residual_cpu < overUtilization_constant * cpu);
	}
	
	public double getCs() {
		return C_s;
	}

	public double getP_max() {
		return p_max;
	}

	public double getP_idle() {
		return p_idle;
	}

	public boolean isStateON() {
		return state;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Server other = (Server) obj;
		if (id != other.id)
			return false;
		return true;
	}

	
}
