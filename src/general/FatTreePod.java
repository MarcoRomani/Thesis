package general;

import java.util.ArrayList;

public class FatTreePod extends Pod {

	public FatTreePod(ArrayList<Switch> c,int n_rack) {
		this.core = c;
		this.racks_number = n_rack;
		this.id = Pod.pod_id;
		Pod.pod_id += 1;
		this.build();
	}
	
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

	@Override
	public int compareTo(Pod arg0) {
		return this.id - arg0.getId();
	}

}
