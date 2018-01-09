package ltCMP;

import java.util.ArrayList;
import java.util.List;

import general.*;

public class GreedyInverseKnapsack {

	protected Server s;
	protected List<Container> mandatory;
	protected List<Container> optional;
	protected List<Container> toProcess;
	protected double partial;

	public GreedyInverseKnapsack() {
		mandatory = new ArrayList<Container>();
		optional = new ArrayList<Container>();
		toProcess = new ArrayList<Container>();
	}

	public void setServer(Server serv) {
		mandatory.clear();
		optional.clear();
		toProcess.clear();
		s = serv;
		toProcess.addAll(s.getContainers());
		partial = s.getResidual_cpu();

	}

	public void process() {

		selectMandatory();
		selectOptional();

	}

	public List<Container> getMandatory() {
		return mandatory;
	}

	public List<Container> getOptional() {
		return optional;
	}

	protected void selectMandatory() {
		ArrayList<Double> gains = new ArrayList<Double>();
		double threshold = 1 - Server.overUtilization_constant;

		while (partial > threshold * s.getCpu()) {
			gains.clear();
			int i_max = -1;
			double max = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < toProcess.size(); j++) {

				gains.add(immediateGain(toProcess.get(j)));
				if (gains.get(j).doubleValue() > max) {
					i_max = j;
					max = gains.get(j).doubleValue();
				}
			}
			
			Container tmp = toProcess.remove(i_max);
			mandatory.add(tmp);
			partial -= tmp.getCpu()*(Server.baseFrequency/s.getFrequency()) ;

		}
	}

	protected Double immediateGain(Container v) {


		// SE VA SOTTO LA TRESHOLD -> DISTANZA
		// SE RIMANE SOPRA -> DISTANZA + best DISTANZA(SOLO POSITIVA) CHE AVREBBE IL NEXT
		return null;
	}

	protected void selectOptional() {

	}
}
