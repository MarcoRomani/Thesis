package cmp_heuristics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.*;
public class CMPOneSwitchIterALT extends CMPOneSwitchIter {

	protected HashMap<Container, Integer> samplemap = new HashMap<Container,Integer>();
	
	
	@Override
	public CMPSolution next() throws MyNoSuchElementException {

		serv_index += 1;
		if (serv_index >= servs.size()) {
			ServerStub st = stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue());
			Container vm = conts.get(cont_index);
			if (st.getId() != dc.getPlacement().get(vm).getId()) {
				put(vm, st, copy, sol.getFlows().get(vm));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(vm));
				copy.getFlows().put(vm, n_ls);
			} else {
				List<LinkFlow> ls = nonMigrate(vm, st, copy).getFlow();
				put(vm, st, copy, ls);
				copy.getFlows().put(vm, new ArrayList<LinkFlow>());
			}
			// copy.setValue(sol.getValue());

			serv_index = 0;
			boolean stop = false;
			while(!stop) {
			   cont_index += 1;
			   if(cont_index < conts.size() && samplemap.get(conts.get(cont_index)).intValue() > 0) {
				   int tmp =samplemap.get(conts.get(cont_index)).intValue();
				   samplemap.replace(conts.get(cont_index), new Integer(tmp-1));
			   }else {
				   stop = true;
			   }
			}
			if (cont_index < conts.size()) {

				st = stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue());
				vm = conts.get(cont_index);
				if (st.getId() != dc.getPlacement().get(vm).getId()) {
					togli(vm, st, copy, sol.getFlows().get(vm));
					copy.getFlows().remove(conts.get(cont_index));
				} else {
					List<LinkFlow> ls = nonMigrate(vm, st, copy).getFlow();
					togli(vm, st, copy, ls);
					copy.getFlows().remove(conts.get(cont_index));
				}

				deltacurrent = deltaObj(conts.get(cont_index),
						stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()), copy, false);
			}
		}
		if (cont_index >= conts.size()) {
			cont_index = 0;
			cust_index += 1;
			 if(cust_index < custs.size() && samplemap.get(conts.get(cont_index)).intValue() > 0) {
				   int tmp =samplemap.get(conts.get(cont_index)).intValue();
				   samplemap.replace(conts.get(cont_index), new Integer(tmp-1));
			  }
			if (cust_index >= custs.size()) {
				throw new MyNoSuchElementException();
			}

			updateCust();

			ServerStub st = stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue());
			Container vm = conts.get(cont_index);
			if (st.getId() != dc.getPlacement().get(vm).getId()) {
				togli(vm, st, copy, sol.getFlows().get(vm));
				copy.getFlows().remove(vm);
			} else {
				List<LinkFlow> ls = nonMigrate(vm, st, copy).getFlow();
				togli(vm, st, copy, ls);
				copy.getFlows().remove(conts.get(cont_index));
			}
			deltacurrent = deltaObj(conts.get(cont_index),
					stubs_after.get(sol.getTable().get(conts.get(cont_index)).intValue()), copy, false);

		}

		if (serv_index >= servs.size())
			return sol;
		
		
			return generateSolution();
	}
	@Override
	protected CMPSolution generateSolution() {	
		
		if(samplemap.get(conts.get(cont_index)).intValue() > 0) {
			return sol;
		}
		
		Integer tmp = new Integer(servs.get(serv_index).getId());
		Integer tmp2 = sol.getTable().get(conts.get(cont_index));

		if (tmp.intValue() == tmp2.intValue())
			return sol;

		double value = sol.getValue();

		Double deltanext = deltaObj(conts.get(cont_index), stubs_after.get(tmp.intValue()), copy, true);
		
		// SAMPLING
		if(deltanext.doubleValue() < deltacurrent.doubleValue() - 0.00001) {
			samplemap.replace(conts.get(cont_index),conts.size()/GRASP_CMP_Scheme.SAMPLING);
		}

		if (deltanext.doubleValue() < deltacurrent.doubleValue() - min_delta) {

			Server s = dc.getPlacement().get(conts.get(cont_index));
			Server t = servs.get(serv_index).getRealServ();
			Response resp = null;
			if (s != t) {
				resp = canMigrate(conts.get(cont_index), s, t);
				if (resp.getAnswer()) {
					CMPSolution nextSol = (CMPSolution) copy.clone();
					nextSol.getTable().put(conts.get(cont_index), tmp);
					nextSol.getFlows().put(conts.get(cont_index), resp.getFlow());
					nextSol.setValue(value - deltacurrent.doubleValue() + deltanext.doubleValue());

					return nextSol;
				}
			} else {
				resp = nonMigrate(conts.get(cont_index), stubs_after.get(s.getId()), copy);
				if (resp.getAnswer()) {
					CMPSolution nextSol = (CMPSolution) copy.clone();
					nextSol.getTable().put(conts.get(cont_index), tmp);
					nextSol.getFlows().put(conts.get(cont_index), new ArrayList<LinkFlow>());
					nextSol.setValue(value - deltacurrent.doubleValue() + deltanext.doubleValue());

					return nextSol;
				}
			}

		}

		return sol;

	}
	
	@Override
	public void setUp(CMPDataCenter dc, Map<Container, Boolean> t, List<ServerStub> stubs,
			DefaultDirectedWeightedGraph<Node, LinkStub> graph, CMPSolution sol) {
		super.setUp(dc, t, stubs, graph, sol);
		for(Customer cst : custs) {
			for(Container c : cst.getNewContainers()) {
				samplemap.put(c, new Integer(0));
			}
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		samplemap.clear();
	}
	
}
