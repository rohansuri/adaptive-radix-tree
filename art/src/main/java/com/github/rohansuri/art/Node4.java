package com.github.rohansuri.art;

class Node4 extends InnerNode {

	static final int NODE_SIZE = 4;

	// each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
	private final byte[] keys = new byte[NODE_SIZE];

	Node4() {
		super(NODE_SIZE);
	}

	Node4(Node16 node16) {
		super(node16, NODE_SIZE);
		assert node16.shouldShrink();
		byte[] keys = node16.getKeys();
		Node[] child = node16.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node16.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node16.noOfChildren);

		// cursor position doesn't change
		for (int i = 0; i < noOfChildren; i++) {
			replaceUplink(this, this.child[i]);
		}
	}

	@Override
	public Node findChild(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// paper does simple loop over because it's a tiny array of size 4
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] == partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public void addChild(byte partialKey, Node child) {
		assert !isFull();
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		// shift elements from this point to right by one place
		// noOfChildren here would never be == Node_SIZE (since we have isFull() check)
		int i = noOfChildren;
		for (; i > 0 && unsignedPartialKey < keys[i - 1]; i--) {
			keys[i] = keys[i - 1];
			this.child[i] = this.child[i - 1];
			setCursor(this.child[i], (byte)i);
		}
		keys[i] = unsignedPartialKey;
		this.child[i] = child;
		noOfChildren++;
		createUplink(this, child, (byte)i);
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = 0;
		for (; index < noOfChildren; index++) {
			if (keys[index] == unsignedPartialKey) {
				break;
			}
		}
		// replace will be called from in a state where you know partialKey entry surely exists
		assert index < noOfChildren : "Partial key does not exist";
		child[index] = newChild;
		createUplink(this, newChild, (byte)index);
	}

	@Override
	public void replaceOn(byte cursor, Node newChild) {
		assert cursor >= 0 && cursor < noOfChildren;
		child[cursor] = newChild;
		createUplink(this, newChild, cursor);
	}

	@Override
	public void removeAt(byte cursor) {
		assert cursor >= 0 && cursor < noOfChildren;
		removeUplink(child[cursor]);
		for (int i = cursor; i < noOfChildren - 1; i++) {
			keys[i] = keys[i + 1];
			child[i] = child[i + 1];
			setCursor(child[i], (byte)i);
		}
		child[noOfChildren - 1] = null;
		noOfChildren--;
	}

	@Override
	public InnerNode grow() {
		assert isFull();
		// grow from Node4 to Node16
		return new Node16(this);
	}

	@Override
	public boolean shouldShrink() {
		return false;
	}

	@Override
	public InnerNode shrink() {
		throw new UnsupportedOperationException("Node4 is smallest node type");
	}

	@Override
	public Node first() {
		return child[0];
	}

	@Override
	public Node last() {
		return child[Math.max(0, noOfChildren - 1)];
	}

	@Override
	public Node ceil(byte partialKey){
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] >= partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public Node greater(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] > partialKey) {
				return child[i];
			}
		}
		return null;
	}

	@Override
	public Node next(byte cursor){
		cursor++;
		if(cursor < noOfChildren){
			return child[cursor];
		}
		return null;
	}

	@Override
	public Node lesser(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = noOfChildren - 1; i >= 0; i--) {
			if (keys[i] < partialKey) {
				return child[i];
			}
		}
		return null;
	}


	@Override
	public Node previous(byte cursor){
		cursor--;
		if(cursor >= 0){
			return child[cursor];
		}
		return null;
	}

	@Override
	public Node floor(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = noOfChildren - 1; i >= 0; i--) {
			if (keys[i] <= partialKey) {
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

	byte getOnlyChildKey() {
		assert noOfChildren == 1;
		return BinaryComparableUtils.signed(keys[0]);
	}
}
