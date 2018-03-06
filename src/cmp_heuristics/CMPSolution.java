package cmp_heuristics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		toReturn.setValue(value.doubleValue());
		return toReturn;
	}

	@Override
	public String toString() {
		return "CMPSolution [table=" + table.toString() + ", value=" + value + "sizeT="+table.size()+ "sizeF="+flows.size()+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
	//	result = prime * result + ((flows == null) ? 0 : flows.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CMPSolution other = (CMPSolution) obj;
		if (flows == null) {
			if (other.flows != null)
				return false;
		} else if (!flows.equals(other.flows))
			return false;
		return true;
	}
	
}
