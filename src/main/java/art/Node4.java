package art;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Node4 extends InnerNode {

	private static final Logger log = LoggerFactory.getLogger(Node4.class);

	static final int NODE_SIZE = 4;

	private final Node[] child = new Node[NODE_SIZE];

	// each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
	private final byte[] keys = new byte[NODE_SIZE];

	Node4() {
	}

	Node4(Node16 node16) {
		super(node16);
		if (!node16.shouldShrink()) {
			throw new IllegalArgumentException("Given Node16 hasn't crossed shrinking threshold yet");
		}
		byte[] keys = node16.getKeys();
		Node[] child = node16.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node16.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node16.noOfChildren);
	}

	@Override
	public Node findChild(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
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

	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (isFull()) {
			return false;
		}
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
		if (index >= 0) { // the partialKey should not exist
			throw new IllegalArgumentException("Cannot insert partial key " + BinaryComparableUtils
					.signed(unsignedPartialKey) + " that already exists in Node. "
					+ "If you want to replace the associated child pointer, use Node#replace(byte, Node)");
		}
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint <= noOfChildren;
		for (int i = noOfChildren; i > insertionPoint; i--) {
			keys[i] = keys[i - 1];
			this.child[i] = this.child[i - 1];
		}
		keys[insertionPoint] = unsignedPartialKey;
		this.child[insertionPoint] = child;
		log.trace("partialKey {} added at {}", unsignedPartialKey, insertionPoint);
		noOfChildren++;
		createUplink(this, child, partialKey);
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
		// replace must be called from in a state where you know partialKey entry surely exists
		if (index < 0) {
			throw new IllegalArgumentException("Partial key " + unsignedPartialKey + " does not exist in this Node.");
		}
		removeUplink(child[index]);
		child[index] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public void removeChild(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		if (index < 0) {
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		removeUplink(child[index]);
		for (int i = index; i < noOfChildren - 1; i++) {
			keys[i] = keys[i + 1];
			child[i] = child[i + 1];
		}
		child[noOfChildren - 1] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		if (!isFull()) {
			throw new IllegalStateException("Grow should be called only when you reach a node's full capacity");
		}
		// grow from Node4 to Node16
		Node node = new Node16(this);
		return node;
	}

	@Override
	public boolean shouldShrink() {
		return false; // can't shrink less than node4
	}

	@Override
	public Node shrink() {
		throw new IllegalStateException("Node4 is smallest node type, can't shrink further");
	}

	@Override
	public Node first() {
		return child[0];
	}

	@Override
	public Node last() {
		if (noOfChildren == 0) {
			return null;
		}
		return child[noOfChildren - 1];
	}

	@Override
	public Node next(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// TODO: use binary search here
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] > partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public boolean isFull() {
		return noOfChildren == NODE_SIZE;
	}

	byte[] getKeys() {
		return keys;
	}

	Node[] getChild() {
		return child;
	}

	byte getOnlyChild() {
		if (noOfChildren != 1) {
			throw new IllegalStateException("more than one children");
		}
		return BinaryComparableUtils.signed(keys[0]);
	}
}

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
