package heuristics;

import java.util.HashMap;

import general.Container;

public class CPPSolution implements Cloneable {
	
	private HashMap<Container, Integer> table = new HashMap<Container, Integer>();
	private float value =Float.POSITIVE_INFINITY;

    public HashMap<Container,Integer> getTable(){
    	return table;
    }

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	@Override
	public Object clone() {
		CPPSolution toReturn = new CPPSolution();
		toReturn.getTable().putAll(this.getTable());
		return toReturn;
	}
    
}
