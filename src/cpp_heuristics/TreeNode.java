package cpp_heuristics;

public class TreeNode {

	double value;
	int index;
	
	private TreeNode parent;
	private TreeNode left;
	private TreeNode right;
	
	public TreeNode(TreeNode par,double val, int ind) {
		parent = par;
		value = val;
		index = ind;
	}
	
	public void addNode(double val,int i) {
		
		if(val <= value) {
			if(left == null) { left = new TreeNode(this,val,i); }
			else {
				left.addNode(val, i);
			}
			
		}else {
			if(right == null) { right = new TreeNode(this,val,i);}
			else {
				right.addNode(val, i);
			}
		}
		
		
	}
	
	public TreeNode find(double lowerbound) {
		if(lowerbound <= value) {
			if(left == null) { return this; }
			else {
				TreeNode l = left.find(lowerbound);
				if(l == null) return this;
				return l;
			}
		}else {
			if(right == null) {
				return null;
			}else {
				return right.find(lowerbound);
			}
			
		}
	}

	public TreeNode getLeft() {
		return left;
	}

	public TreeNode getRight() {
		return right;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public int getIndex() {
		return index;
	}
	
	public double getValue() {
		return value;
	}

	@Override
	public String toString() {
		String s1 = (left == null)? "" : left.toString();
		String s2 = (right == null)? "" : right.toString();
		return s1 + " [value= "+value+" id= "+index+"]"+s2;
	}
	
}
