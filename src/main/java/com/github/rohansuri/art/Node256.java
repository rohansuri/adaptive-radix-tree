package com.github.rohansuri.art;

class Node256 extends InnerNode {
	static final int NODE_SIZE = 256;

	Node256(Node48 node) {
		super(node, NODE_SIZE);
		assert Node48.isFull(node);

		byte[] keyIndex = Node48.getKeyIndex(node);
		Node[] child = node.getChild();

		for (int i = 0; i < Node48.KEY_INDEX_SIZE; i++) {
			byte index = keyIndex[i];
			if (index == Node48.ABSENT) {
				continue;
			}
			assert index >= 0 && index <= 47;
			// index is byte, but gets type promoted
			// https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html#jls-10.4-120
			this.child[i] = child[index];
			// update up link
			replaceUplink(this, this.child[i]);
		}
	}

	public static Node findChild(Node256 node256, byte partialKey) {
		// convert byte to 8 bit integer
		// and then index into that array position
		// We should treat the 8 bits as unsigned int
		// since we've got 256 slots, we need to go from 00000000 to 11111111
		int index = Byte.toUnsignedInt(partialKey);
		return node256.child[index];
	}

	public static boolean addChild(Node256 node256, byte partialKey, Node child) {
		// addChild would never be called on a full Node256
		// since the corresponding findChild for any byte key
		// would always find the byte since the Node is full!
		assert !isFull(node256);
		// byte in Java is signed
		// but we want no interpretation of the partialKey
		// we just want to treat it as raw binary bits
		// but since byte is signed, numerically when we index using it
		// it can be negative once it goes over 127, therefore we need to
		// convert it to a bigger container type
		// or can we do something better?
		int index = Byte.toUnsignedInt(partialKey);
		assert node256.child[index] == null;
		createUplink(node256, child, partialKey);
		node256.child[index] = child;
		node256.noOfChildren++;
		return true;
	}

	public static void replace(Node256 node256, byte partialKey, Node newChild) {
		int index = Byte.toUnsignedInt(partialKey);
		assert node256.child[index] != null;
		node256.child[index] = newChild;
		createUplink(node256, newChild, partialKey);
	}

	public static void removeChild(Node256 node256, byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		assert node256.child[index] != null;
		removeUplink(node256.child[index]);
		node256.child[index] = null;
		node256.noOfChildren--;
	}


	public static boolean shouldShrink(Node256 node256) {
		return node256.noOfChildren == Node48.NODE_SIZE;
	}

	public static InnerNode shrink(Node256 node256) {
		assert Node256.shouldShrink(node256);
		return new Node48(node256);
	}

	public static Node first(Node256 node256) {
		assert node256.noOfChildren > Node48.NODE_SIZE;
		for (int i = 0; i < NODE_SIZE; i++) {
			if (node256.child[i] != null) {
				return node256.child[i];
			}
		}
		throw new IllegalStateException("Node256 should contain more than " + Node48.NODE_SIZE + " elements");
	}

	public static Node last(Node256 node256) {
		assert node256.noOfChildren > Node48.NODE_SIZE;
		for (int i = NODE_SIZE - 1; i >= 0; i--) {
			if (node256.child[i] != null) {
				return node256.child[i];
			}
		}
		throw new IllegalStateException("Node256 should contain more than " + Node48.NODE_SIZE + " elements");
	}

	public static Node greater(Node256 node256, byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) + 1; i < NODE_SIZE; i++) {
			if (node256.child[i] != null) {
				return node256.child[i];
			}
		}
		return null;
	}

	public static Node lesser(Node256 node256, byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) - 1; i >= 0; i--) {
			if (node256.child[i] != null) {
				return node256.child[i];
			}
		}
		return null;
	}

	public static boolean isFull(Node256 node256) {
		return node256.noOfChildren == NODE_SIZE;
	}
}
