package cpp_heuristics;

public class TreeNodeExplorer {

	private TreeIndex tree;
	private TreeNode current;
	
	public TreeNodeExplorer(TreeIndex t) {
		tree = t;
		current = tree.root;
	}
	
	public boolean hasNext() {
		if(current.value >= tree.maxvalue && climb(current) == null ) {
			return false;
		}
		return true;
	}
	
	public int next() {
		
		if(current.getRight() != null) {
			current = current.getRight().find(Double.NEGATIVE_INFINITY);
		}else {
			current = climb(current);
		}
		return current.index;
	
	}
	
	protected TreeNode climb(TreeNode n) {
		double tmp = n.value;
		TreeNode t = n;
		do {
			t = t.getParent();
		}while(t != null && t.value < tmp);
        return t; 
         
		
		
	}
	
	public void setStart(double lower) {
		current = tree.root.find(lower);
	}
}
