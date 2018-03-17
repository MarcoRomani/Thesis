package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

import cpp_heuristics.InfeasibilityException;
import general.CMPDataCenter;
import general.Container;
import ltCMP.CMPMain;

public class GRASP_CMP_Type2 extends GRASP_CMP_Type1 {

	public GRASP_CMP_Type2(CMPDataCenter dc, Input input) {
		super(dc, input);
	}

	@Override
	protected CMPSolution greedy_rand_constr(Input input, double alfa) throws InfeasibilityException {
		CMPSolution sol = new CMPSolution();

		List<Container> singles = new ArrayList<Container>();
		singles.addAll(input.getSinglesOBL());

		List<List<Container>> clusters = new ArrayList<List<Container>>();
		for (List<Container> ls : input.getClustersOBL()) {
			List<Container> clust_copy = new ArrayList<Container>();
			clust_copy.addAll(ls);
			clust_copy = randomizeList(clust_copy);
			clusters.add(clust_copy);
		}
		List<List<Container>> cs_copy = new ArrayList<List<Container>>();
		sortByFirst(clusters, cs_copy);

		List<Container> rest = new ArrayList<Container>();
		for (List<Container> cluster : cs_copy) {
			if(CMPMain.display) {
			System.out.println("DOING NEW OBL CLUSTER");
			}
			sol = cluster_rand_constr(sol, cluster, alfa, rest);
		}

		singles.addAll(rest);
		singles.sort(comp);
		if(CMPMain.display) {
		System.out.println("DOING OBL SINGLES");
		}
		sol = single_rand_constr(sol, singles, alfa);

		clusters.clear();
		cs_copy.clear();
		singles.clear();
		rest.clear();
		
		// OPTIONALS

		singles.addAll(input.getSinglesOPT());
		for (List<Container> ls : input.getClustersOPT()) {
			List<Container> clust_copy = new ArrayList<Container>();
			clust_copy.addAll(ls);
			clust_copy = randomizeList(clust_copy);
			clusters.add(clust_copy);
		}
		
		sortByFirst(clusters, cs_copy);

		for (List<Container> cluster : cs_copy) {
			if(CMPMain.display) {	
			System.out.println("DOING NEW OPT CLUSTER");
			}
			sol = cluster_rand_constr(sol, cluster, alfa, rest);
		}
		singles.addAll(rest);
		if(CMPMain.display) {
		System.out.println("DOING OPT SINGLES");
		}
		singles.sort(comp);
		sol = single_rand_constr(sol, singles, alfa);

		return sol;

	}

	protected List<Container> randomizeList(List<Container> list) {
		List<Container> copyRandom = new ArrayList<Container>();
		while (!list.isEmpty()) {
			int tmp = rng.nextInt(list.size());
			Container c = list.remove(tmp);
			copyRandom.add(c);
		}
		return copyRandom;
	}

	@Override
	protected void sortByFirst(List<List<Container>> clusters, List<List<Container>> ordered) {
		while (!clusters.isEmpty()) {
			ordered.add(clusters.remove(rng.nextInt(clusters.size())));
		}

	}
}
