package com.github.rohansuri.art;

class Node256 extends InnerNode {
	static final int NODE_SIZE = 256;

	Node256(Node48 node) {
		super(node, NODE_SIZE);
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
			this.child[i+1] = child[index+1];
			// update up link
			replaceUplink(this, this.child[i+1]);
		}
	}

	@Override
	public Node findChild(byte partialKey) {
		// We treat the 8 bits as unsigned int since we've got 256 slots
		int index = Byte.toUnsignedInt(partialKey);
		return child[index+1];
	}

    @Override
    public void addChild(byte partialKey, Node child) {
        // addChild would never be called on a full Node256
        // since the corresponding findChild for any byte key
        // would always find the byte since the Node is full.
        assert !isFull();
        int index = Byte.toUnsignedInt(partialKey);
        assert this.child[index+1] == null;
        createUplink(this, child, partialKey);
        this.child[index+1] = child;
        noOfChildren++;
    }

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Byte.toUnsignedInt(partialKey);
		assert child[index+1] != null;
		child[index+1] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		assert child[index+1] != null;
		removeUplink(child[index+1]);
		child[index+1] = null;
		noOfChildren--;
	}

	@Override
	public InnerNode grow() {
		throw new UnsupportedOperationException("Span of ART is 8 bits, so Node256 is the largest node type.");
	}

	@Override
	public boolean shouldShrink() {
		return noOfChildren == Node48.NODE_SIZE;
	}

	@Override
	public InnerNode shrink() {
		assert shouldShrink();
		return new Node48(this);
	}

	@Override
	public Node first() {
		assert noOfChildren > Node48.NODE_SIZE;
		int i = 0;
		while(child[i+1] == null)i++;
		return child[i+1];
	}

	@Override
	public Node last() {
		assert noOfChildren > Node48.NODE_SIZE;
		int i = NODE_SIZE - 1;
		while(child[i+1] == null)i--;
		return child[i+1];
	}

	@Override
	public Node ceil(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey); i < NODE_SIZE; i++) {
			if (child[i+1] != null) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public Node greater(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) + 1; i < NODE_SIZE; i++) {
			if (child[i+1] != null) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public Node lesser(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) - 1; i >= 0; i--) {
			if (child[i+1] != null) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public Node floor(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey); i >= 0; i--) {
			if (child[i+1] != null) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public boolean isFull() {
		return noOfChildren == NODE_SIZE;
	}
}
