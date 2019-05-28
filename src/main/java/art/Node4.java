package art;

import java.util.Arrays;

class Node4 extends AbstractNode {

	private static final int NODE_SIZE = 4;

	private final Node[] child = new Node[NODE_SIZE];

	// each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
	private final byte[] keys = new byte[NODE_SIZE];

	@Override
	public Node findChild(byte partialKey) {
		// TODO: consider linear loop over search vs binary search?
		// paper does simple loop over probably because it's a tiny array (size 4)

		// binary search for key
		// having the from and to gives us only a valid view into what are the
		// valid array elements that actually have keys and are not ABSENT
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		if (index < 0) {
			return null;
		}
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (noOfChildren == NODE_SIZE) {
			return false;
		}
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		assert index < 0; // the partialKey should not exist
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint < NODE_SIZE;
		for (int i = NODE_SIZE - 1; i > insertionPoint; i--) {
			keys[i] = keys[i - 1];
			this.child[i] = this.child[i - 1];
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
	public Node grow() {
		// grow from Node4 to Node16
		Node node = new Node16(this);
		return node;
	}

	public byte[] getKeys() {
		return keys;
	}

	public Node[] getChild() {
		return child;
	}
}

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
