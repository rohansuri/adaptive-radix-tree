package art;

import java.util.Arrays;

class Node16 extends AbstractNode {
	private static final int NODE_SIZE = 16;
    private final Node[] child = new Node[NODE_SIZE];
    private final byte[] keys = new byte[NODE_SIZE];

    Node16(Node4 node){
    	super(node);
		byte[] keys = node.getKeys();
		Node[] child = node.getChild();
		System.arraycopy(keys, 0, this.keys, 0, keys.length);
		System.arraycopy(child, 0, this.child, 0, child.length);
	}

	@Override
	public Node findChild(byte partialKey) {
		// binary search for key
		// having the from and to gives us only a valid view into what are the
		// valid array elements that actually have keys and are not ABSENT
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		if (index < 0) {
			return null;
		}
		return child[index];
	}

	// TODO: unit test binary search insertion point edge cases (first, last)
	@Override
	public boolean addChild(byte partialKey, Node child) {
		if(noOfChildren == NODE_SIZE){
			return false;
		}
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		assert index < 0; // the partialKey should not exist
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint <= noOfChildren;
		for(int i = noOfChildren - 1; i > insertionPoint ; i--){
			keys[i] = keys[i-1];
			this.child[i] = this.child[i-1];
		}
		keys[insertionPoint] = partialKey;
		this.child[insertionPoint] = child;
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		if (index < 0) {
			// TODO: better flow/API design?
			// start with thinking what do we finally return/throw in such a state?
			throw new IllegalStateException("replace must be called from in a state where you know partialKey entry surely exists");
		}
		child[index] = newChild;
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		assert index >= 0;
		for(int i = index; i < noOfChildren - 1; i++){
			keys[i] = keys[i+1];
			child[i] = child[i+1];
		}
		child[noOfChildren - 1] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		Node node = new Node48(this);
		return node;
	}

	byte[] getKeys(){
    	return keys;
	}

	Node[] getChild(){
    	return child;
	}
}
