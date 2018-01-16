package cpp_heuristics;

public class TreeIndex {

	double maxvalue = 0;
	double minvalue = Double.POSITIVE_INFINITY;

	TreeNode root;

	public void insert(double val, int i) {
		if (val < minvalue)
			minvalue = val;
		if (val > maxvalue)
			maxvalue = val;
		if (root != null) {
			root.addNode(val, i);
		} else {

			root = new TreeNode(null, val, i);
		}
	}

	public TreeNode find(double lower) {
		return root.find(lower);
	}

	@Override
	public String toString() {
		if(root == null) return "empty tree";
		return root.toString()+" "+maxvalue+" "+minvalue;
	}
	
	
}
