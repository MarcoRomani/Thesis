package cmp_heuristics;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cpp_heuristics.CPPSolution;
import cpp_heuristics.PathRel_manager.Sol_Couple;

public class CMPPRThread extends Thread {
	protected CMPPath_Relinking alg;
	protected ConcurrentLinkedQueue<Sol_Couple> tasks;
	protected ConcurrentLinkedQueue<CPPSolution> candidate;
	protected AtomicInteger semaforo;
	
	public CMPPRThread(CMPPath_Relinking alg, ConcurrentLinkedQueue<Sol_Couple> queue, ConcurrentLinkedQueue<CPPSolution> cand,AtomicInteger sem) {
		this.alg = alg;
		tasks = queue;
		candidate = cand;
		this.semaforo = sem;
	}
	
	@Override
	public void run() {
		while (!tasks.isEmpty()) {
			Object sc = tasks.poll();
			if (sc != null) {
				Sol_Couple pair = (Sol_Couple) sc;
				CMPSolution sol = alg.relink((CMPSolution)pair.getS(),(CMPSolution) pair.getT());
				
			//	if(!sol.equals(pair.getS())) {
					synchronized(candidate) {
						candidate.add(sol);
					}
			//	}
			}
		}
		
		synchronized(tasks) {
			semaforo.incrementAndGet();
			tasks.notifyAll();
		}
	}
}
