package cmp_heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import cpp_heuristics.ServerStub;
import general.*;

public abstract class GRASP_CMP_Scheme {

	protected SecureRandom rng;
	protected CMPDataCenter dc;
	protected List<Container> mandatory;
	protected List<Container> optional;
	protected List<ServerStub> stubs_migr;
	protected List<ServerStub> stubs_after;

	protected abstract CMPSolution greedy_rand_constr(List<Container> toPlace, float alfa);

	protected abstract double incrementalCost(Container c, ServerStub s);

	protected abstract void changeNeighborhood();

	public CMPSolution grasp(int maxIter, int seed, float alfa) {
		// TODO
		rng = new SecureRandom(BigInteger.valueOf(seed).toByteArray());
		CMPSolution best = new CMPSolution();

		for (int iter = 0; iter < maxIter; iter++) {

			System.out.println("\n iter:" + iter);
			CMPSolution incumbent = new CMPSolution();

			incumbent = greedy_rand_constr(toPlace,alfa);

			evaluate(incumbent);

			incumbent = localSearch(incumbent);

			if (incumbent.getValue() < best.getValue()) {
				best = (CMPSolution) incumbent.clone();
			}

		}
		return best;
	}

	protected CMPSolution localSearch(CMPSolution sol) {
		// TODO
		return null;
	}

	protected double evaluate(CMPSolution sol) {
		// TODO
		return 0;
	}
}
