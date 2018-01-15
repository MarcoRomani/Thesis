package cpp_heuristics;

public class TreeNodeExplorer {

	private TreeIndex tree;
	private TreeNode current;
	
	public TreeNodeExplorer(TreeIndex t) {
		tree = t;
		current = tree.root;
	}
	
	public boolean hasNext() {
		if(current.value == tree.maxvalue && current.getRight() == null) {
			return false;
		}
		return true;
	}
	
	public int next() {
		while(current.getRight() == null){
			double tmp = current.value;
			current = current.getParent();
			if(current.value >= tmp){
				return current.index;
			}
		}
		current = current.getRight();
		current = current.find(Double.NEGATIVE_INFINITY);
		return current.index;
	
	}
	
	public void setStart(double lower) {
		current = tree.root.find(lower);
	}
}
