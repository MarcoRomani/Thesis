package cmp_heuristics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cpp_heuristics.CPPSolution;
import general.*;

public class CMPSolution extends CPPSolution {

	
	protected HashMap<Container, ArrayList<LinkFlow>> flows = new HashMap<Container, ArrayList<LinkFlow>>();
	
	
	
	public Map<Container, ArrayList<LinkFlow>> getFlows(){
		return flows;
	}
	
	@Override
	public Object clone()  {
		CPPSolution toReturn = new CMPSolution();
		toReturn.getTable().putAll(table);
		toReturn.setValue(value);
		return toReturn;
	}

	@Override
	public String toString() {
		return "CPPSolution [table=" + table.toString() + ", value=" + value + "size="+table.size()+"]";
	}
	
}
