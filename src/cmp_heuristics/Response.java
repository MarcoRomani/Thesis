package cmp_heuristics;

import java.util.ArrayList;
import java.util.List;

public class Response {

	protected boolean answer;
	protected ArrayList<LinkFlow> flow = new ArrayList<LinkFlow>();
	
	public Response(boolean ans, List<LinkFlow> fl) {
		this.answer = ans;
		this.flow.addAll(fl);
	}
	public boolean getAnswer() {
		return answer;
	}
	
	public ArrayList<LinkFlow> getFlow() {
		return flow;
	}
	
	
}
