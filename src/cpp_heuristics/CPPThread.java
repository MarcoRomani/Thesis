package cpp_heuristics;
/**
 * 
 * @author Marco
 * Thread that calls a grasp execution with some parameters
 */
public class CPPThread extends Thread {

	protected int seed;
	protected float rcl_param;
	protected int iterations;
	protected GRASP_CPP_Scheme alg;

	public CPPThread(int iter, int seed, float rcl_param, GRASP_CPP_Scheme alg) {
		iterations = iter;
		this.seed = seed;
		this.rcl_param = rcl_param;
		this.alg = alg;
	}

	@Override
	public void run() {
		if(seed == 0) {
			alg.grasp(iterations, rcl_param);
		}else {
			alg.grasp(iterations, seed, rcl_param);
		}
	}
}
