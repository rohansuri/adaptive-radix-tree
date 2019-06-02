package art;

import java.util.Arrays;

class Node16 extends AbstractNode {
	static final int NODE_SIZE = 16;
    private final Node[] child = new Node[NODE_SIZE];
    private final byte[] keys = new byte[NODE_SIZE];

    Node16(Node4 node){
		super(node);
    	if(node.noOfChildren != Node4.NODE_SIZE){
    		throw new IllegalArgumentException("Given Node4 still has capacity, cannot grow into Node16.");
		}
		byte[] keys = node.getKeys();
		Node[] child = node.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node.noOfChildren);
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
		// the partialKey should not exist
		if(index >= 0){
			throw new IllegalArgumentException("Cannot insert partial key " + partialKey + " that already exists in Node."
					+ "If you want to replace the associated child pointer, use Node#replace(byte, Node)");
		}
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint <= noOfChildren;
		for(int i = noOfChildren; i > insertionPoint ; i--){
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
		if(index < 0) {
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		child[index] = newChild;
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		if(index < 0){
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		for(int i = index; i < noOfChildren - 1; i++){
			keys[i] = keys[i+1];
			child[i] = child[i+1];
		}
		child[noOfChildren - 1] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		if(noOfChildren != NODE_SIZE){
			throw new IllegalStateException("Grow should be called only when you reach a node's full capacity");
		}
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
