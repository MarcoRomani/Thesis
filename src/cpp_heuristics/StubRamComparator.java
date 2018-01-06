package cpp_heuristics;
import java.util.*;

public class StubRamComparator implements Comparator<ServerStub>{

	@Override
	public int compare(ServerStub o1, ServerStub o2) {
		
		return (int) Math.signum(o2.getRes_mem() - o1.getRes_mem());
	}

	
}
