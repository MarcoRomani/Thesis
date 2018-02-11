package cmp_heuristics;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.*;

public abstract class GRASP_CMP_Scheme {

	public static double min_delta = 0.0000000001;
	public static double MIGR_TIME;
	public static int maxHops = 10;
	public static int k_paths = 3;
	protected SecureRandom rng;
	protected CMPDataCenter dc;
	protected List<Container> mandatory;
	protected List<Container> optional;
	protected List<LinkStub> stubs_migr;
	protected List<ServerStub> stubs_after;
	protected DefaultDirectedWeightedGraph<Node, LinkStub> graph;

	protected abstract CMPSolution greedy_rand_constr(List<Container> toPlace, double alfa);

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
