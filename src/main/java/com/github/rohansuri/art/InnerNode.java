package com.github.rohansuri.art;

// should be 16 bytes only

abstract class InnerNode extends Node {

	static final int PESSIMISTIC_PATH_COMPRESSION_LIMIT = 8;

	// max limit of 8 bytes (Pessimistic)
	final byte[] prefixKeys;

	// Optimistic
	int prefixLen; // 4 bytes

	// to decide to grow or not
	// TODO: we could save space by making this a short for Node256 and byte for other node types?
	// since noOfChildren will never be more than 256 and we don't seem to be
	// using it specifically on an AbstractNode level? (are we?)
	short noOfChildren; // 2 bytes

	final Node[] child;

	InnerNode(int size) {
		prefixKeys = new byte[PESSIMISTIC_PATH_COMPRESSION_LIMIT];
		child = new Node[size + 1];
	}

	// copy ctor. called when growing/shrinking
	InnerNode(InnerNode node, int size) {
		super(node);
		child = new Node[size + 1];
		// copy header
		this.noOfChildren = node.noOfChildren;
		this.prefixLen = node.prefixLen;
		this.prefixKeys = node.prefixKeys;

		// copy leaf & replace uplink
		child[size] = node.getLeaf();
		if (child[size] != null) {
			replaceUplink(this, child[size]);
		}
	}

	// CLEANUP: move to test utils
	byte[] getValidPrefixKey() {
		int limit = Math.min(PESSIMISTIC_PATH_COMPRESSION_LIMIT, prefixLen);
		byte[] valid = new byte[limit];
		System.arraycopy(prefixKeys, 0, valid, 0, limit);
		return valid;
	}

	public void setLeaf(LeafNode<?, ?> leaf) {
		child[child.length - 1] = leaf;
		createUplink(this, leaf);
	}

	public Node getLeaf() {
		return child[child.length - 1];
	}

}
