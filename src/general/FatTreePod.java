package general;

import java.util.ArrayList;
/**
 * Representation of a pod in a fat tree data center.
 * @author Marco
 *
 */
public class FatTreePod extends Pod {

	/**
	 * Generates a pod starting from a specified number of racks. Every pod has the reference to all core nodes.
	 * @param c
	 * @param n_rack
	 */
	public FatTreePod(ArrayList<Switch> c,int n_rack) {
		this.core = c;
		this.racks_number = n_rack;
		this.id = Pod.pod_id;
		Pod.pod_id += 1;
		this.build();
	}
	
	/* generate the inner racks*/
	@Override
	protected void build() {
		for(int i=0; i<racks_number ; i++) {
			this.edge.add(new Switch());
			this.aggregation.add(new Switch());
		}

		for(int i=0; i<racks_number ; i++) {
			
			this.racks.add(new FatTreeRack(this.edge.get(i), this.racks_number, Catalog.buyServer() ));
		}

		lower_index = racks.get(0).getLower_index();
		upper_index = racks.get(racks.size()-1).getUpper_index();
	}

	/**
	 * Compare two pods based on their IDs
	 */
	@Override
	public int compareTo(Pod arg0) {
		return this.id - arg0.getId();
	}

}
