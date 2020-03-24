package com.github.rohansuri.art;

class Node4 extends InnerNode {

	static final int NODE_SIZE = 4;

	// each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
	protected byte[] keys = new byte[NODE_SIZE];

	Node4() {
		super(NODE_SIZE);
	}

	// For Node16.
	Node4(InnerNode node, int size){
		super(node, size);
		keys = new byte[size];
	}

	Node4(Node16 node16) {
		super(node16, NODE_SIZE);
		assert node16.shouldShrink();
		byte[] keys = node16.getKeys();
		Node[] child = node16.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node16.noOfChildren);
		System.arraycopy(child, 1, this.child, 1, node16.noOfChildren);

		// update up links
		for (int i = 0; i < noOfChildren; i++) {
			replaceUplink(this, this.child[i+1]);
		}
	}

	@Override
	public final Node findChild(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// paper does simple loop over because it's a tiny array of size 4
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] == partialKey) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public final void addChild(byte partialKey, Node child) {
		assert !isFull();
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		// shift elements from this point to right by one place
		// noOfChildren here would never be == Node_SIZE (since we have isFull() check)
		int i = noOfChildren;
		for (; i > 0 && unsignedPartialKey < keys[i - 1]; i--) {
			keys[i] = keys[i - 1];
			this.child[i+1] = this.child[i];
		}
		keys[i] = unsignedPartialKey;
		this.child[i+1] = child;
		noOfChildren++;
		createUplink(this, child, partialKey);
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
		child[index+1] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public final void removeChild(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = 0;
		for (; index < noOfChildren; index++) {
			if (keys[index] == partialKey) {
				break;
			}
		}
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		assert index < noOfChildren : "Partial key does not exist";
		removeUplink(child[index+1]);
		for (int i = index; i < noOfChildren - 1; i++) {
			keys[i] = keys[i + 1];
			child[i+1] = child[i + 2];
		}
		child[noOfChildren] = null;
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
	public final Node first() {
		return child[1];
	}

	@Override
	public final Node last() {
		if(noOfChildren == 0){
			return null;
		}
		return child[noOfChildren];
	}

	@Override
	public final Node ceil(byte partialKey){
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] >= partialKey) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public final Node greater(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = 0; i < noOfChildren; i++) {
			if (keys[i] > partialKey) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public final Node lesser(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = noOfChildren - 1; i >= 0; i--) {
			if (keys[i] < partialKey) {
				return child[i+1];
			}
		}
		return null;
	}

	@Override
	public final Node floor(byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for (int i = noOfChildren - 1; i >= 0; i--) {
			if (keys[i] <= partialKey) {
				return child[i+1];
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
