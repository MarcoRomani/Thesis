package general;

public class Container implements Comparable<Container>{

	
	public static int container_id = 0;
	public static final Container c_0 = new Container();
	private int my_id;
	private Container_model type;
	private float cpu;
	private float mem;
	private float disk;
	private float bdw_out;
	private float bdw_in;
	private int my_customer;
	//private float frequency;
	
	private Container() {
		this.my_id = container_id;
		container_id += 1;
	}
	
	public Container(Container_model cm, int customer) {
		my_customer = customer;
		this.type = cm;
		type = cm;
		float [] specs = Catalog.getContSpecs(cm);
		cpu = specs[0];
		mem = specs[1];
		disk = specs[3];
		bdw_out = specs[2];
		bdw_in = specs[2];
		
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

	public float getCpu() {
		return cpu;
	}

	public float getMem() {
		return mem;
	}

	public float getDisk() {
		return disk;
	}

	public float getBdw_out() {
		return bdw_out;
	}

	public float getBdw_in() {
		return bdw_in;
	}

	@Override
	public int compareTo(Container arg0) {
		// TODO Auto-generated method stub
		int tmp =(arg0).getId();
		return my_id - tmp;
	}
	
	
}
