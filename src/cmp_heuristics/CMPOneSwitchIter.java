package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.CPPSolution;
import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Customer;
import general.Node;

public class CMPOneSwitchIter implements CMPNeighborhood {

	public static double inv_offset = GRASP_CMP_Scheme.inv_offset;
	protected CMPSolution sol = new CMPSolution();
	protected CMPDataCenter dc;
	protected List<ServerStub> stubs_after;
	protected int cust_index = 0;
	protected int cont_index = 0;
	protected int serv_index = 0;
	protected List<Container> conts = new ArrayList<Container>();
	protected List<ServerStub> servs = new ArrayList<ServerStub>();
	protected List<Customer> custs = new ArrayList<Customer>();
	protected CMPSolution copy;
	protected Double deltacurrent;
	protected  DefaultDirectedWeightedGraph<Node, LinkStub> graph;

	public CMPOneSwitchIter() {
		for (Customer c : Customer.custList) {
			if (c.getNewContainers().size() > 0) {
				custs.add(c);
			}
		}
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CMPSolution next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUp(CMPDataCenter dc, List<ServerStub> stubs,  DefaultDirectedWeightedGraph<Node, LinkStub> graph , CMPSolution sol) {
		this.dc = dc;
		this.stubs_after = stubs;
		this.graph = graph;

		cust_index = 0;
		cont_index = 0;
		serv_index = -1;

		ArrayList<Container> toSwitch = new ArrayList<Container>();
		for (Container vm : this.sol.getTable().keySet()) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {
				toSwitch.add(vm);			
				
				// System.out.println("da correggere");
			}
			List<LinkFlow> ls = this.sol.getFlows().get(vm);
			for(LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				l.setResCapacity(l.getResCapacity() + lf.getFlow());
				graph.setEdgeWeight(l, 1/(l.getResCapacity() + inv_offset));
			}
			this.sol.getFlows().remove(vm);
			ls  = sol.getFlows().get(vm);
			for(LinkFlow lf : ls) {
				LinkStub l = lf.getLink();
				l.setResCapacity(l.getResCapacity() - lf.getFlow());
				graph.setEdgeWeight(l, 1/(l.getResCapacity() - inv_offset));	
			}
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>();
			neWls.addAll(ls);
			this.sol.getFlows().put(vm, neWls );
		}

		// remove phase
		for (Container v : toSwitch) {
			stubs_after.get(this.sol.getTable().get(v).intValue()).remove(v, stubs_after, this.sol, dc);
			this.sol.getTable().remove(v);
			
		}// allocate phase
		for(Container v: toSwitch) {
			int tmp = sol.getTable().get(v).intValue();
			stubs_after.get(tmp).forceAllocation(v, stubs_after, this.sol, dc);
			this.sol.getTable().put(v, new Integer(tmp));
		}
		this.sol.setValue(sol.getValue());
		
		updateCust();
		
		//PREPARE THE COPY
		copy = (CMPSolution) this.sol.clone();
		
		stubs_after.get(this.sol.getTable().get(conts.get(cont_index)).intValue()).remove(conts.get(cont_index), stubs_after, copy, dc);
		copy.getTable().remove(conts.get(cont_index));
		List<LinkFlow> ls = this.sol.getFlows().get(conts.get(cont_index));
		for(LinkFlow lf : ls) {
			LinkStub l = lf.getLink();
			l.setResCapacity(l.getResCapacity() + lf.getFlow());
			graph.setEdgeWeight(l, 1/(l.getResCapacity() + inv_offset));
		}
		this.sol.getFlows().remove(conts.get(cont_index));
		deltacurrent = deltaObj(conts.get(cont_index), stubs_after.get(this.sol.getTable().get(conts.get(cont_index)).intValue()),
				copy, false);
		
	}

	private Double deltaObj(Container container, ServerStub serverStub, CMPSolution copy2, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	private void updateCust() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		conts = new ArrayList<Container>();
		servs = new ArrayList<ServerStub>();
		this.sol.getTable().clear();
		this.sol.getFlows().clear();
		this.sol.setValue(Double.POSITIVE_INFINITY);
	}

}
