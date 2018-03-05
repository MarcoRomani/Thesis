package cmp_heuristics;


public class CMPThread extends Thread{

	protected int seed;
	protected double rcl_param;
	protected String option;
	protected int param;
	protected GRASP_CMP_Scheme alg;
	
	public CMPThread(String option, int param, int seed, double rcl_param, GRASP_CMP_Scheme alg) {
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
