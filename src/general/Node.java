package general;

public class Node {

	public static int NODE_IDS = -1;
	protected int my_node_id;
	
	public Node() {
		my_node_id = NODE_IDS;
		NODE_IDS -= 1;
	}
	
	public int getId() {
		return my_node_id;
	}
	
	@Override
	public String toString() {
		return my_node_id+"";
	}
}
