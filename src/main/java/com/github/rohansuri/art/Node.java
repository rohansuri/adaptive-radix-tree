package com.github.rohansuri.art;

/*
	These are internal contracts/interfaces
 	They've been written with only what they're used for internally
 	For example Node#remove could have returned a false indicative of a failed remove
 	due to partialKey entry not actually existing, but the return value is of no use in code till now
 	and is sure to be called from places where it'll surely exist.
 	since they're internal, we could change them later if a better contract makes more sense.

	The impls have assert conditions all around to make sure the methods are called being in the right
	state. For example you should not call shrink() if the Node is not ready to shrink, etc.
	Or for example when calling last() on Node16 or higher, we're sure we'll have at least
	X amount of children hence safe to return child[noOfChildren-1], without worrying about bounds.

 */
abstract class Node {
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
	abstract Node grow();

	abstract boolean shouldShrink();

	/**
	 * creates and returns the a smaller node type with the same mappings as this node
	 * @return a smaller node with the same mappings
	 */
	abstract Node shrink();

	/**
	 * @return child pointer for the smallest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	abstract Node first();

	abstract Node firstOrLeaf();

	/**
	 * @return child pointer for the largest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	abstract Node last();

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

	/**
	 *
	 * @return no of children this Node has
	 */
	abstract short size();

	// for upwards traversal
	// dev note: wherever you setup downlinks, you setup uplinks as well
	private InnerNode parent;
	private byte partialKey;

	Node(){}

	// copy ctor. called when growing/shrinking
	Node(Node node) {
		this.partialKey = node.partialKey;
		this.parent = node.parent;
	}

	// do we need partial key for leaf nodes? we'll find out
	static void createUplink(InnerNode parent, LeafNode<?, ?> child) {
		Node c = child;
		c.parent = parent;
	}

	static void createUplink(InnerNode parent, Node child, byte partialKey) {
		child.parent = parent;
		child.partialKey = partialKey;
	}

	// called when growing/shrinking and all children now have a new parent
	static void replaceUplink(InnerNode parent, Node child) {
		child.parent = parent;
	}

	static void removeUplink(Node child) {
		child.parent = null;
	}

	/**
	 * @return the parent of this node. Returns null for root node.
	 */
	public InnerNode parent() {
		return parent;
	}

	/**
	 * @return the uplinking partial key to parent
	 */
	public byte uplinkKey() {
		return partialKey;
	}
}
