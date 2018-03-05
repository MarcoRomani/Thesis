package ltCMP;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import general.*;

public class GreedyInverseKnapsack {

	protected Server s;
	protected List<Container> mandatory;
	protected List<Container> optional;
	protected List<Container> toProcess;
	protected double partial;
	protected int depth = 2;

	public GreedyInverseKnapsack() {
		mandatory = new ArrayList<Container>();
		optional = new ArrayList<Container>();
		toProcess = new LinkedList<Container>();
	}

	public void setServer(Server serv) {
		mandatory.clear();
		optional.clear();
		toProcess.clear();
		s = serv;
		toProcess.addAll(s.getContainers());
		partial = s.getResidual_cpu();

	}

	public void setDepth(int d) {
		depth = d;
	}

	public void process() {

		if(s.getResidual_cpu() >= Server.almostEmpty_constant*s.getCpu()) {
			optional.addAll(toProcess);
			partial = 0;
			return;
			
		}
		mandatory = selectContainers(Server.overUtilization_constant);
		optional = selectContainers(Server.underUtilization_constant);
		
	

	}

	public List<Container> getMandatory() {
		return mandatory;
	}

	public List<Container> getOptional() {
		return optional;
	}

	protected List<Container> selectContainers(double threshold) {

		List<Container> results = new ArrayList<Container>();
		ArrayList<Double> distances = new ArrayList<Double>();

		while (partial < threshold * s.getCpu()) {
			distances.clear();
			int i_min = -1;
			double min = Double.POSITIVE_INFINITY;
			for (int j = 0; j < toProcess.size(); j++) {

				distances.add(distance(toProcess.get(j), threshold, depth));
				if (distances.get(j).doubleValue() < min) {
					i_min = j;
					min = distances.get(j).doubleValue();
				}
			}

			Container tmp = toProcess.remove(i_min);
			results.add(tmp);
			partial += CPUcalculator.utilization(tmp, s); //tmp.getCpu() * (Server.baseFrequency / s.getFrequency());

		}
		return results;
	}

	protected Double distance(Container v, double threshold, int depth) {

		// SE VA SOPRA LA TRESHOLD -> DISTANZA
		double tmp = partial + CPUcalculator.utilization(v, s); //v.getCpu() * (Server.baseFrequency / s.getFrequency());
		double distance = tmp - threshold * s.getCpu();
		if (distance >= 0) {
			return new Double(distance);
		}
		// Depth esaurita -> + infinito
		if (depth <= 1) {
			return new Double(Double.POSITIVE_INFINITY);
		}

		// SE RIMANE SOTTO -> best DISTANZA(depth -1)
		int index = toProcess.indexOf(v);
		toProcess.remove(v);
		partial += CPUcalculator.utilization(v, s); // v.getCpu() * (Server.baseFrequency / s.getFrequency());
		ArrayList<Double> costs = new ArrayList<Double>();

		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < toProcess.size(); i++) {
			costs.add(distance(toProcess.get(i), threshold, depth - 1));
			if (costs.get(i).doubleValue() < min) {
				min = costs.get(i).doubleValue();
			}
		}

		toProcess.add(index, v);
		partial -= CPUcalculator.utilization(v, s); //v.getCpu() * (Server.baseFrequency / s.getFrequency());
		return new Double(min);
	}

	
}
