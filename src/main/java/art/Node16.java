package art;

import java.util.Arrays;

class Node16 extends InnerNode {
	static final int NODE_SIZE = 16;
	private final Node[] child = new Node[NODE_SIZE];
	private final byte[] keys = new byte[NODE_SIZE];

	Node16(Node4 node) {
		super(node);
		if (!node.isFull()) {
			throw new IllegalArgumentException("Given Node4 still has capacity, cannot grow into Node16.");
		}
		byte[] keys = node.getKeys();
		Node[] child = node.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node.noOfChildren);

		// update up links
		for (int i = 0; i < noOfChildren; i++) {
			replaceUplink(this, this.child[i]);
		}
	}

	Node16(Node48 node48) {
		super(node48);
		if (!node48.shouldShrink()) {
			throw new IllegalArgumentException("Given Node48 hasn't crossed shrinking threshold yet");
		}
		byte[] keyIndex = node48.getKeyIndex();
		Node[] children = node48.getChild();

		// keyIndex by virtue of being "array indexed" is already sorted
		// so we can iterate and keep adding into Node16
		for (int i = 0, j = 0; i < Node48.KEY_INDEX_SIZE; i++) {
			if (keyIndex[i] != Node48.ABSENT) {
				child[j] = children[keyIndex[i]];
				keys[j] = BinaryComparableUtils.unsigned(child[j].uplinkKey());
				replaceUplink(this, child[j]);
				j++;
			}
		}
	}

	@Override
	public Node findChild(byte partialKey) {
		// TODO: use simple loop to see if -XX:+SuperWord applies SIMD JVM instrinsics
		partialKey = BinaryComparableUtils.unsigned(partialKey);
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
		if (isFull()) {
			return false;
		}
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
		// the partialKey should not exist
		if (index >= 0) {
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
		noOfChildren++;
		createUplink(this, child, partialKey);
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
		if (index < 0) {
			throw new IllegalArgumentException("Partial key " + unsignedPartialKey + " does not exist in this Node.");
		}
		child[index] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public void removeChild(byte partialKey) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		if (index < 0) {
			throw new IllegalArgumentException("Partial key " + unsignedPartialKey + " does not exist in this Node.");
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
		Node node = new Node48(this);
		return node;
	}

	@Override
	public boolean shouldShrink() {
		return noOfChildren == Node4.NODE_SIZE;
	}

	@Override
	public Node shrink() {
		if (!shouldShrink()) {
			throw new IllegalStateException("Haven't crossed shrinking threshold yet");
		}
		Node4 node4 = new Node4(this);
		return node4;
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
	public Node greater(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// TODO: consider using binary search here
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] > partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public Node lesser(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// TODO: consider using binary search here
		for (int i = noOfChildren - 1; i >= 0; i--) {
			if (keys[i] < partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public short size() {
		return noOfChildren;
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
}
