package cmp_heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import cpp_heuristics.ServerStub;
import general.CMPDataCenter;
import general.Container;
import general.Customer;
import general.Node;

public class CMPOneSwapSmallIterALT extends CMPOneSwapSmallIter {

	protected HashMap<Customer, Integer> samplemap = new HashMap<Customer,Integer>();
	
	@Override
	public CMPSolution next() throws MyNoSuchElementException {
		index_two += 1;
		if (index_two >= conts.size()) {
			Container vm = conts.get(index_one);
			ServerStub st = stubs_after.get(sol.getTable().get(vm));
			if(st.getId() != dc.getPlacement().get(vm).getId()) {
				put(vm,st,copy,sol.getFlows().get(vm));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(vm));
				copy.getFlows().put(vm, n_ls);
			}else {
				List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
				put(vm,st,copy,ls);
				copy.getFlows().put(vm, new ArrayList<LinkFlow>());
			}

			index_one += 1;
			index_two = index_one + 1;

			if (index_one >= conts.size() - 1) {
				boolean stop = false;
				while(!stop) {
				   cust_index+=1;
				   
				   if(cust_index < custs.size() && samplemap.get(custs.get(cust_index)).intValue() > 0){
					   int tmp = samplemap.get(custs.get(cust_index)).intValue();
					   samplemap.replace(custs.get(cust_index), new Integer(tmp-1));
				   }else {
					   stop = true;
				   }
				}
				
				index_one = 0;
				index_two = 1;
				updateCustomer();
			}

			 vm = conts.get(index_one);
			 st = stubs_after.get(sol.getTable().get(vm));
			if(st.getId() != dc.getPlacement().get(vm).getId()) {
				togli(vm,st,copy,sol.getFlows().get(vm));
				copy.getFlows().remove(vm);
			}else {
				List<LinkFlow> ls = nonMigrate(vm,st,copy).getFlow();
				togli(vm,st,copy,ls);
				copy.getFlows().remove(vm);
			}
			deltacurrent = deltaObj(conts.get(index_one),
					stubs_after.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);

		}

		return generateSolution();
	}
	
	@Override
	protected CMPSolution generateSolution() {

		Container c1 = conts.get(index_one);
		Container c2 = conts.get(index_two);
		Integer s1 = sol.getTable().get(c1);
		Integer s2 = sol.getTable().get(c2);

		if (c1 == c2 || s1.intValue() == s2.intValue()) {
			return sol;
		}

		
		
		ServerStub st = stubs_after.get(s2.intValue());
		
		List<LinkFlow> ls_init = null;
		if(st.getId() != dc.getPlacement().get(c2).getId()) {
			togli(c2,st,copy,sol.getFlows().get(c2));
			ls_init = copy.getFlows().remove(c2);
		}else {
			List<LinkFlow> ls = nonMigrate(c2,st,copy).getFlow();
			togli(c2,st,copy,ls);
			copy.getFlows().remove(c2);
			ls_init = ls;
		}

		Double deltacurrent_2 = deltaObj(c2, stubs_after.get(s2.intValue()), copy, false);
		Double deltanext_2 = deltaObj(c2, stubs_after.get(s1.intValue()), copy, true);
		
		if (deltanext_2.doubleValue() < Double.POSITIVE_INFINITY) {
			stubs_after.get(s1.intValue()).forceAllocation(c2, stubs_after, copy, dc);
			copy.getTable().put(c2, s1);

			Double deltanext = deltaObj(c1, stubs_after.get(s2.intValue()), copy, true);
			
			// SAMPLING
			if(deltanext.doubleValue()
					+ deltanext_2.doubleValue() < deltacurrent.doubleValue() + deltacurrent_2.doubleValue() - 0.0001) {
				samplemap.replace(custs.get(cust_index), custs.size()/GRASP_CMP_Scheme.SAMPLING);
			}
			
			if(deltanext.doubleValue()
					+ deltanext_2.doubleValue() < deltacurrent.doubleValue() + deltacurrent_2.doubleValue() - min_delta) {
			// CAN MIGRATE c2 IN S1
			int old2 = dc.getPlacement().get(c2).getId();
			Response resp2 = null;
			if (old2 != s1.intValue()) {
				resp2 = canMigrate(c2, dc.getPlacement().get(c2), stubs_after.get(s1.intValue()).getRealServ());
			} else {
				resp2 = nonMigrate(c2, stubs_after.get(s1.intValue()), copy);
			}

			List<LinkFlow> ls = resp2.getFlow();
			updateLinks(ls,true);

		//	Double deltanext = deltaObj(c1, stubs_after.get(s2.intValue()), copy, true);
			// CAN MIGRATE c1 in S2
			int old1 = dc.getPlacement().get(c1).getId();
			Response resp1 = null;
			if (old1 != s2.intValue()) {
				resp1 = canMigrate(c1, dc.getPlacement().get(c1), stubs_after.get(s2.intValue()).getRealServ());
			} else {
				resp1 = nonMigrate(c1, stubs_after.get(s2.intValue()), copy);
			}

			stubs_after.get(s1.intValue()).remove(c2, stubs_after, copy, dc);
			copy.getTable().remove(c2);
			updateLinks(ls,false);

			stubs_after.get(s2.intValue()).forceAllocation(c2, stubs_after, copy, dc);
			copy.getTable().put(c2, s2);

			ls = sol.getFlows().get(c2);
			updateLinks(ls_init,true);
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>(ls);
		
			copy.getFlows().put(c2, neWls);

			if (resp1.getAnswer() && resp2.getAnswer() ) {
				CMPSolution nextSol = (CMPSolution) copy.clone();
				nextSol.getTable().remove(c2);
				nextSol.getFlows().remove(c2);
				nextSol.getTable().put(c1, s2);
				if (old1 != s2.intValue()) {
				nextSol.getFlows().put(c1, resp1.getFlow());
				}else {
					nextSol.getFlows().put(c1, new ArrayList<LinkFlow>());
				}
				nextSol.getTable().put(c2, s1);
				if (old2 != s1.intValue()) {
			     	nextSol.getFlows().put(c2, resp2.getFlow());
				}
				else{
					nextSol.getFlows().put(c2, new ArrayList<LinkFlow>());
				}
				
				nextSol.setValue(sol.getValue() - deltacurrent.doubleValue() - deltacurrent_2.doubleValue()
						+ deltanext.doubleValue() + deltanext_2.doubleValue());
				return nextSol;
			}
			}else {
				stubs_after.get(s1.intValue()).remove(c2, stubs_after, copy, dc);
				copy.getTable().remove(c2);
				
				stubs_after.get(s2.intValue()).forceAllocation(c2, stubs_after, copy, dc);
				copy.getTable().put(c2, s2);
				updateLinks(ls_init,true);
				ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>(sol.getFlows().get(c2));
				copy.getFlows().put(c2, neWls);
			}
		} 
		else {
			
			stubs_after.get(s2.intValue()).forceAllocation(c2, stubs_after, copy, dc);
			copy.getTable().put(c2, s2);
			updateLinks(ls_init,true);
			ArrayList<LinkFlow> neWls = new ArrayList<LinkFlow>(sol.getFlows().get(c2));
			copy.getFlows().put(c2, neWls);
			
			/*
			if(st.getId() != dc.getPlacement().get(c2).getId()) {
				put(c2,st,copy,sol.getFlows().get(c2));
				ArrayList<LinkFlow> n_ls = new ArrayList<LinkFlow>(sol.getFlows().get(c2));
				copy.getFlows().put(c2, n_ls);
			}else {
				List<LinkFlow> ls = nonMigrate(c2,st,copy).getFlow();
				put(c2,st,copy,ls);
				copy.getFlows().put(c2, new ArrayList<LinkFlow>());
			}*/

		}

		return sol;

	}
	
	@Override
	public void setUp(CMPDataCenter dc, Map<Container, Boolean> t, List<ServerStub> stubs,
			DefaultDirectedWeightedGraph<Node, LinkStub> graph, CMPSolution sol) {
		super.setUp(dc, t, stubs, graph, sol);
		for(Customer cst : custs) {
			samplemap.put(cst, new Integer(0));
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		samplemap.clear();
	}
}
