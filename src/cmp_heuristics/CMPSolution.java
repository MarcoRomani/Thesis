package cmp_heuristics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import cpp_heuristics.CPPSolution;
import general.*;

public class CMPSolution implements Cloneable {

	protected HashMap<Container, Integer> assignment = new HashMap<Container, Integer>();
	protected HashMap<Container, ArrayList<LinkFlow>> flows = new HashMap<Container, ArrayList<LinkFlow>>();
	protected double value = Double.POSITIVE_INFINITY;
	
	protected Integer getAssignment(Container v) {
		return assignment.get(v);
	}
	
	protected Integer removeAssignment(Container v) {
		return assignment.remove(v);
	}
	
	public Set<Container> getAllContainers(){
		   return assignment.keySet();
	}
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	protected Object clone()  {
		CPPSolution toReturn = new CPPSolution();
		toReturn.getTable().putAll(assignment);
		toReturn.setValue(value);
		return toReturn;
	}

	@Override
	public String toString() {
		return "CPPSolution [table=" + assignment.toString() + ", value=" + value + "size="+assignment.size()+"]";
	}
	
}
