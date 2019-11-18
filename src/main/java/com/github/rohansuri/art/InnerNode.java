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

	public void removeLeaf() {
		removeUplink(child[child.length - 1]);
		child[child.length - 1] = null;
	}

	public boolean hasLeaf() {
		return child[child.length - 1] != null;
	}

	public LeafNode<?, ?> getLeaf() {
		return (LeafNode<?, ?>) child[child.length - 1];
	}

	@Override
	public Node firstOrLeaf() {
		if (hasLeaf()) {
			return getLeaf();
		}
		return first();
	}

	Node[] getChild() {
		return child;
	}

	/**
	 *
	 * @return no of children this Node has
	 */
	public short size() {
		return noOfChildren;
	}

	/**
	 *
	 * @param partialKey search if this node has an entry for given partialKey
	 * @return if it does, then return the following child pointer.
	 * Returns null if there is no corresponding entry.
	 */
	abstract Node findChild(byte partialKey);

	/**
	 * @param partialKey partialKey to be mapped
	 * @param child the child node to be added
	 * @return true if add succeeded, false if node size full (in the event of which you call grow)
	 */
	abstract boolean addChild(byte partialKey, Node child);

	/**
	 * @param partialKey for which the child pointer mapping is to be updated
	 * @param newChild the new mapping to be added for given partialKey
	 */
	abstract void replace(byte partialKey, Node newChild);

	/**
	 * @param partialKey for which the child pointer mapping is to be removed
	 */
	abstract void removeChild(byte partialKey);

	/**
	 * creates and returns the next larger node type with the same mappings as this node
	 * @return a new node with the same mappings
	 */
	abstract InnerNode grow();

	abstract boolean shouldShrink();

	/**
	 * creates and returns the a smaller node type with the same mappings as this node
	 * @return a smaller node with the same mappings
	 */
	abstract InnerNode shrink();

	/**
	 * @return true if Node has reached it's capacity
	 */
	abstract boolean isFull();

	/**
	 * @return returns the smallest child node for the partialKey strictly greater than the partialKey passed.
	 * Returns null if no such child.
	 */
	abstract Node greater(byte partialKey);

	/**
	 * @return returns the greatest child node for the partialKey strictly lesser than the partialKey passed.
	 * Returns null if no such child.
	 */
	abstract Node lesser(byte partialKey);
}
