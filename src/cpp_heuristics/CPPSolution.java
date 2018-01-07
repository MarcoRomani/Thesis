package cpp_heuristics;


import java.util.HashMap;


import general.Container;

public class CPPSolution implements Cloneable {
	
	private HashMap<Container, Integer> table = new HashMap<Container, Integer>();
	private Double value =Double.POSITIVE_INFINITY;

    public HashMap<Container,Integer> getTable(){
    	return table;
    }

	public double getValue() {
		return value.doubleValue();
	}

	public void setValue(double value) {
		this.value = new Double(value);
	}

	
	@Override
	public Object clone() {
		CPPSolution toReturn = new CPPSolution();
		toReturn.getTable().putAll(table);
		toReturn.setValue(value.doubleValue());
		return toReturn;
	}

	@Override
	public String toString() {
		return "CPPSolution [table=" + table.toString() + ", value=" + value + "size="+table.size()+"]";
	}
    
}
