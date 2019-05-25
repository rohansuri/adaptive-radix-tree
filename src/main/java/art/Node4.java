package art;

import java.util.Arrays;

class Node4 extends AbstractNode {
    private final Node[] child = new Node[4];

    // each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
    private final byte[] keys = new byte[4];

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

	@Override
	public boolean addChild(byte partialKey, Node child) {
		return false;
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
	public Node grow(){
		// grow from Node4 to Node16
		Node node = new Node16(this);
		return node;
	}

	public byte[] getKeys(){
		return keys;
	}

	public Node[] getChild(){
		return child;
	}
}

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
