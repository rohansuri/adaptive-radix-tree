package art;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Node4 extends AbstractNode {

	private static final Logger log = LoggerFactory.getLogger(Node4.class);

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
		// FIXME: partialKeys greater than 127 would be wrongly stored in sorted order
		// this hasn't been a problem yet because it's just simply follow ons
		// but range scan/iteration would be erroneous!
		if (index < 0) {
			return null;
		}
		return child[index];
	}

	// TODO: unit test this binary search inserts into correct position
	// along with edge cases (where partialKey is found to be first, last element in array)
	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (noOfChildren == NODE_SIZE) {
			return false;
		}
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		assert index < 0; // the partialKey should not exist
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint <= noOfChildren;
		for (int i = noOfChildren; i > insertionPoint; i--) {
			keys[i] = keys[i - 1];
			this.child[i] = this.child[i - 1];
		}
		keys[insertionPoint] = partialKey;
		this.child[insertionPoint] = child;
		log.trace("partialKey {} added at {}", partialKey, insertionPoint);
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		// replace must be called from in a state where you know partialKey entry surely exists
		assert index >= 0;
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
