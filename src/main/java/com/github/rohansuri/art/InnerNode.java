package com.github.rohansuri.art;

// should be 16 bytes only

class InnerNode extends Node {

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
	final Node findChild(byte partialKey) {
		if (this instanceof Node4) {
			return Node4.findChild((Node4) this, partialKey);
		}
		if (this instanceof Node16) {
			return Node16.findChild((Node16) this, partialKey);
		}
		if (this instanceof Node48) {
			return Node48.findChild((Node48) this, partialKey);
		}
		return Node256.findChild((Node256) this, partialKey);
	}

	/**
	 * @param partialKey partialKey to be mapped
	 * @param child the child node to be added
	 * @return true if add succeeded, false if node size full (in the event of which you call grow)
	 */
	final boolean addChild(byte partialKey, Node child) {
		if (this instanceof Node4) {
			return Node4.addChild((Node4) this, partialKey, child);
		}
		if (this instanceof Node16) {
			return Node16.addChild((Node16) this, partialKey, child);
		}
		if (this instanceof Node48) {
			return Node48.addChild((Node48) this, partialKey, child);
		}
		return Node256.addChild((Node256) this, partialKey, child);
	}

	/**
	 * @param partialKey for which the child pointer mapping is to be updated
	 * @param newChild the new mapping to be added for given partialKey
	 */
	final void replace(byte partialKey, Node newChild) {
		if (this instanceof Node4) {
			Node4.replace((Node4) this, partialKey, newChild);
		}
		else if (this instanceof Node16) {
			Node16.replace((Node16) this, partialKey, newChild);
		}
		else if (this instanceof Node48) {
			Node48.replace((Node48) this, partialKey, newChild);
		}
		else {
			Node256.replace((Node256) this, partialKey, newChild);
		}
	}

	/**
	 * @param partialKey for which the child pointer mapping is to be removed
	 */
	final void removeChild(byte partialKey) {
		if (this instanceof Node4) {
			Node4.removeChild((Node4) this, partialKey);
		}
		else if (this instanceof Node16) {
			Node16.removeChild((Node16) this, partialKey);
		}
		else if (this instanceof Node48) {
			Node48.removeChild((Node48) this, partialKey);
		}
		else {
			Node256.removeChild((Node256) this, partialKey);
		}
	}

	/**
	 * creates and returns the next larger node type with the same mappings as this node
	 * @return a new node with the same mappings
	 */
	final InnerNode grow() {
		if (this instanceof Node4) {
			return Node4.grow((Node4) this);
		}
		if (this instanceof Node16) {
			return Node16.grow((Node16) this);
		}
		return Node48.grow((Node48) this);
	}

	final boolean shouldShrink() {
		if (this instanceof Node4) {
			return false;
		}
		if (this instanceof Node16) {
			return Node16.shouldShrink((Node16) this);
		}
		if (this instanceof Node48) {
			return Node48.shouldShrink((Node48) this);
		}
		return Node256.shouldShrink((Node256) this);
	}

	/**
	 * creates and returns the a smaller node type with the same mappings as this node
	 * @return a smaller node with the same mappings
	 */
	final InnerNode shrink() {
		if (this instanceof Node16) {
			return Node16.shrink((Node16) this);
		}
		if (this instanceof Node48) {
			return Node48.shrink((Node48) this);
		}
		return Node256.shrink((Node256) this);
	}

	/**
	 * @return true if Node has reached it's capacity
	 */
	final boolean isFull() {
		if (this instanceof Node4) {
			return Node4.isFull((Node4) this);
		}
		if (this instanceof Node16) {
			return Node16.isFull((Node16) this);
		}
		if (this instanceof Node48) {
			return Node48.isFull((Node48) this);
		}
		return Node256.isFull((Node256) this);
	}

	/**
	 * @return returns the smallest child node for the partialKey strictly greater than the partialKey passed.
	 * Returns null if no such child.
	 */
	final Node greater(byte partialKey) {
		if (this instanceof Node4) {
			return Node4.greater((Node4) this, partialKey);
		}
		if (this instanceof Node16) {
			return Node16.greater((Node16) this, partialKey);
		}
		if (this instanceof Node48) {
			return Node48.greater((Node48) this, partialKey);
		}
		return Node256.greater((Node256) this, partialKey);
	}

	/**
	 * @return returns the greatest child node for the partialKey strictly lesser than the partialKey passed.
	 * Returns null if no such child.
	 */
	final Node lesser(byte partialKey) {
		if (this instanceof Node4) {
			return Node4.lesser((Node4) this, partialKey);
		}
		if (this instanceof Node16) {
			return Node16.lesser((Node16) this, partialKey);
		}
		if (this instanceof Node48) {
			return Node48.lesser((Node48) this, partialKey);
		}
		return Node256.lesser((Node256) this, partialKey);
	}

	final Node first() {
		if (this instanceof Node4) {
			return Node4.first((Node4) this);
		}
		if (this instanceof Node16) {
			return Node16.first((Node16) this);
		}
		if (this instanceof Node48) {
			return Node48.first((Node48) this);
		}
		return Node256.first((Node256) this);
	}

	final Node last() {
		if (this instanceof Node4) {
			return Node4.last((Node4) this);
		}
		if (this instanceof Node16) {
			return Node16.last((Node16) this);
		}
		if (this instanceof Node48) {
			return Node48.last((Node48) this);
		}
		return Node256.last((Node256) this);
	}
}
