package cmp_heuristics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpp_heuristics.CPPSolution;
import general.*;

public class CMPSolution extends CPPSolution {

	
	protected Map<Container, List<LinkFlow>> flows = new HashMap<Container, List<LinkFlow>>();
	
	
	
	public Map<Container, List<LinkFlow>> getFlows(){
		return flows;
	}
	
	@Override
	public Object clone()  {
		CMPSolution toReturn = new CMPSolution();
		toReturn.getTable().putAll(table);
		toReturn.getFlows().putAll(flows);
		toReturn.setValue(value);
		return toReturn;
	}

	@Override
	public String toString() {
		return "CPPSolution [table=" + table.toString() + ", value=" + value + "size="+table.size()+"]";
	}
	
}
