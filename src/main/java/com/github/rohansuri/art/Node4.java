package com.github.rohansuri.art;

class Node4 extends InnerNode {

	static final int NODE_SIZE = 4;

	// each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
	final byte[] keys = new byte[NODE_SIZE];

	Node4() {
		super(NODE_SIZE);
	}

	Node4(Node16 node16) {
		super(node16, NODE_SIZE);
		assert Node16.shouldShrink(node16);
		byte[] keys = Node16.getKeys(node16);
		Node[] child = node16.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node16.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node16.noOfChildren);

		// update up links
		for (int i = 0; i < noOfChildren; i++) {
			replaceUplink(this, this.child[i]);
		}
	}

	public static Node findChild(Node4 node4, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// paper does simple loop over because it's a tiny array of size 4
		for (int i = 0; i < node4.noOfChildren; i++) {
			if (node4.keys[i] == partialKey) {
				return node4.child[i];
			}
		}
		return null;
	}

	public static boolean addChild(Node4 node4, byte partialKey, Node child) {
		if (Node4.isFull(node4)) {
			return false;
		}
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		// shift elements from this point to right by one place
		// noOfChildren here would never be == Node_SIZE (since we have isFull() check)
		int i = node4.noOfChildren;
		for (; i > 0 && unsignedPartialKey < node4.keys[i - 1]; i--) {
			node4.keys[i] = node4.keys[i - 1];
			node4.child[i] = node4.child[i - 1];
		}
		node4.keys[i] = unsignedPartialKey;
		node4.child[i] = child;
		node4.noOfChildren++;
		createUplink(node4, child, partialKey);
		return true;
	}

	public static void replace(Node4 node4, byte partialKey, Node newChild) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = 0;
		for (; index < node4.noOfChildren; index++) {
			if (node4.keys[index] == unsignedPartialKey) {
				break;
			}
		}
		// replace will be called from in a state where you know partialKey entry surely exists
		assert index < node4.noOfChildren : "Partial key does not exist";
		node4.child[index] = newChild;
		createUplink(node4, newChild, partialKey);
	}

	public static void removeChild(Node4 node4, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = 0;
		for (; index < node4.noOfChildren; index++) {
			if (node4.keys[index] == partialKey) {
				break;
			}
		}
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		assert index < node4.noOfChildren : "Partial key does not exist";
		removeUplink(node4.child[index]);
		for (int i = index; i < node4.noOfChildren - 1; i++) {
			node4.keys[i] = node4.keys[i + 1];
			node4.child[i] = node4.child[i + 1];
		}
		node4.child[node4.noOfChildren - 1] = null;
		node4.noOfChildren--;
	}

	public static InnerNode grow(Node4 node4) {
		assert Node4.isFull(node4);
		// grow from Node4 to Node16
		return new Node16(node4);
	}

	public static Node first(Node4 node4) {
		return node4.child[0];
	}

	public static Node last(Node4 node4) {
		return node4.child[Math.max(0, node4.noOfChildren - 1)];
	}

	public static Node greater(Node4 node4, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = 0; i < node4.noOfChildren; i++) {
			if (node4.keys[i] > partialKey) {
				return node4.child[i];
			}
		}
		return null;
	}

	public static Node lesser(Node4 node4, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = node4.noOfChildren - 1; i >= 0; i--) {
			if (node4.keys[i] < partialKey) {
				return node4.child[i];
			}
		}
		return null;
	}

	public static boolean isFull(Node4 node4) {
		return node4.noOfChildren == NODE_SIZE;
	}

	static byte[] getKeys(Node4 node4) {
		return node4.keys;
	}

	static byte getOnlyChildKey(Node4 node4) {
		assert node4.noOfChildren == 1;
		return BinaryComparableUtils.signed(node4.keys[0]);
	}
}

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
