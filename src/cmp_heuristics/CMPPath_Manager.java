package cmp_heuristics;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


import cpp_heuristics.CPPSolution;
import cpp_heuristics.PathRel_manager;
import general.CMPDataCenter;
import ltCMP.CMPMain;

public class CMPPath_Manager extends PathRel_manager {

	protected Input input;

	public CMPPath_Manager(CMPDataCenter dc, int maxElite, List<CPPSolution> init_candidate, Input input,
			SecureRandom rng) {
		super(dc, maxElite, init_candidate, rng);
		this.input = input;

	}

	public CPPSolution path_relinking() {
		ConcurrentLinkedQueue<Sol_Couple> tasks = new ConcurrentLinkedQueue<Sol_Couple>();
		ArrayList<CMPPath_Relinking> algs = new ArrayList<CMPPath_Relinking>();

		for (int i = 0; i < parallelism; i++) {
			algs.add(new CMPPath_Relinking((CMPDataCenter) dc, alfa, inner_iter, beta, n_moves, input, rng));
			ArrayList<CMPNeighborhood> neighs = new ArrayList<CMPNeighborhood>();
			neighs.add(new CMPOneSwitchSmallIter());
			neighs.add(new CMPOneSwitchMediumIter());
		//	neighs.add(new CMPOneSwapSmallIter()); 
			      neighs.add(new CMPOneSwapSmallIterALT());
			
			algs.get(algs.size() - 1).setNeighborhoods(neighs);
		}

		int iter = 0;
		boolean flag = true;
		Date d1 = new Date();
		Date d2 = new Date();

		while (iter < maxIter && (d2.getTime() - d1.getTime()) < maxTime && flag) {

			iter++;
			if (CMPMain.display) {
				System.out.println("Path Relinking iteration: " + iter);
			}
			// SELECT UNEXPLORED PATHS
			tasks.clear();
			for (Sol_Couple sc : table.keySet()) {
				if (!table.get(sc).booleanValue()) {
					tasks.add(sc);
					table.replace(sc, new Boolean(true));
				}
			}

			// SET UP THREADS
			AtomicInteger semaforo = new AtomicInteger(0);
			ArrayList<CMPPRThread> threads = new ArrayList<CMPPRThread>();
			for (CMPPath_Relinking al : algs) {
				threads.add(new CMPPRThread(al, tasks, candidate, semaforo));
			}

			// LAUNCH THREADS
			for (CMPPRThread thread : threads) {
				thread.start();
			}

			try {
				synchronized (tasks) {
					while (semaforo.get() < threads.size()) {

						tasks.wait();

					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// THREADS FINISHED, UPDATE POOL
			flag = updatePool();
			if(CMPMain.display) {
				System.out.println("POOL: "+pool.size());
			}
			d2 = new Date();
		}
		
		execTime = d2.getTime()-d1.getTime();
		my_iter = iter;
		return best;
	}

	
	
	

}
