package cpp_heuristics;
import java.util.*;
/**
 * 
 * @author Marco
 * Ordering of server copies based on descending residual ram order
 */
public class StubRamComparator implements Comparator<ServerStub>{

	@Override
	public int compare(ServerStub o1, ServerStub o2) {
		
		return (int) Math.signum(o2.getRes_mem() - o1.getRes_mem());
	}

	
}
