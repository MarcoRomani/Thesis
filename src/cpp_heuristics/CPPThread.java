package cpp_heuristics;

/**
 * 
 * @author Marco Thread that calls a grasp execution with some parameters
 */
public class CPPThread extends Thread {

	protected int seed;
	protected float rcl_param;
	protected String option;
	protected int param;
	protected GRASP_CPP_Scheme alg;

	public CPPThread(String option, int param, int seed, float rcl_param, GRASP_CPP_Scheme alg) {
		this.option = option;
		this.param = param;
		this.seed = seed;
		this.rcl_param = rcl_param;
		this.alg = alg;
	}

	@Override
	public void run() {

		alg.grasp(option, param, rcl_param, seed);

	}
}
