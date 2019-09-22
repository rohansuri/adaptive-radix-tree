package com.github.rohansuri.art;

class Node256 extends InnerNode {
	static final int NODE_SIZE = 256;
	private final Node child[] = new Node[NODE_SIZE]; // 256 * 8 bytes

	Node256(Node48 node) {
		super(node);
		assert node.isFull();

		byte[] keyIndex = node.getKeyIndex();
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

	@Override
	public Node findChild(byte partialKey) {
		// convert byte to 8 bit integer
		// and then index into that array position
		// We should treat the 8 bits as unsigned int
		// since we've got 256 slots, we need to go from 00000000 to 11111111
		int index = Byte.toUnsignedInt(partialKey);
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		// addChild would never be called on a full Node256
		// since the corresponding findChild for any byte key
		// would always find the byte since the Node is full!
		assert !isFull();
		// byte in Java is signed
		// but we want no interpretation of the partialKey
		// we just want to treat it as raw binary bits
		// but since byte is signed, numerically when we index using it
		// it can be negative once it goes over 127, therefore we need to
		// convert it to a bigger container type
		// or can we do something better?
		int index = Byte.toUnsignedInt(partialKey);
		assert this.child[index] == null;
		createUplink(this, child, partialKey);
		this.child[index] = child;
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Byte.toUnsignedInt(partialKey);
		assert child[index] != null;
		child[index] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		assert child[index] != null;
		removeUplink(child[index]);
		child[index] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		throw new UnsupportedOperationException("Span of ART is 8 bits, so Node256 is the largest node type.");
	}

	@Override
	public boolean shouldShrink() {
		return noOfChildren == Node48.NODE_SIZE;
	}

	@Override
	public Node shrink() {
		assert shouldShrink();
		Node48 node48 = new Node48(this);
		return node48;
	}

	@Override
	public Node first() {
		assert noOfChildren > Node48.NODE_SIZE;
		for (int i = 0; i < NODE_SIZE; i++) {
			if (child[i] != null) {
				return child[i];
			}
		}
		throw new IllegalStateException("Node256 should contain more than " + Node48.NODE_SIZE + " elements");
	}

	@Override
	public Node last() {
		assert noOfChildren > Node48.NODE_SIZE;
		for (int i = NODE_SIZE - 1; i >= 0; i--) {
			if (child[i] != null) {
				return child[i];
			}
		}
		throw new IllegalStateException("Node256 should contain more than " + Node48.NODE_SIZE + " elements");
	}

	@Override
	public Node greater(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) + 1; i < NODE_SIZE; i++) {
			if (child[i] != null) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public Node lesser(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) - 1; i >= 0; i--) {
			if (child[i] != null) {
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

	Node[] getChild() {
		return child;
	}

}
