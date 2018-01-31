package cpp_heuristics;


import java.util.HashMap;


import general.Container;
/**
 * 
 * @author Marco
 * Implementation of a cpp solution through an hashmap that stores the assignments
 * of containers to servers' IDs. The value of the solution is also stored.
 *
 */
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CPPSolution other = (CPPSolution) obj;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
    
}
