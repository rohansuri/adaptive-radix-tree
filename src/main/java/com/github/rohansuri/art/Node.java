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
interface Node {
	/**
	 *
	 * @param partialKey search if this node has an entry for given partialKey
	 * @return if it does, then return the following child pointer.
	 * Returns null if there is no corresponding entry.
	 */
	Node findChild(byte partialKey);

	/**
	 * @param partialKey partialKey to be mapped
	 * @param child the child node to be added
	 * @return true if add succeeded, false if node size full (in the event of which you call grow)
	 */
	boolean addChild(byte partialKey, Node child);

	/**
	 * @param partialKey for which the child pointer mapping is to be updated
	 * @param newChild the new mapping to be added for given partialKey
	 */
	void replace(byte partialKey, Node newChild);

	/**
	 * @param partialKey for which the child pointer mapping is to be removed
	 */
	void removeChild(byte partialKey);

	/**
	 * creates and returns the next larger node type with the same mappings as this node
	 * @return a new node with the same mappings
	 */
	Node grow();

	boolean shouldShrink();

	/**
	 * creates and returns the a smaller node type with the same mappings as this node
	 * @return a smaller node with the same mappings
	 */
	Node shrink();

	/**
	 * @return child pointer for the smallest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	Node first();

	/**
	 * @return child pointer for the largest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	Node last();

	/**
	 * @return true if Node has reached it's capacity
	 */
	boolean isFull();

	/**
	 * @return the parent of this node. Returns null for root node.
	 */
	Node parent();

	/**
	 * @return the uplinking partial key to parent
	 */
	byte uplinkKey();

	/**
	 * @return returns the smallest child node for the partialKey strictly greater than the partialKey passed.
	 * Returns null if no such child.
	 */
	Node greater(byte partialKey);

	/**
	 * @return returns the greatest child node for the partialKey strictly lesser than the partialKey passed.
	 * Returns null if no such child.
	 */
	Node lesser(byte partialKey);

	/**
	 *
	 * @return no of children this Node has
	 */
	short size();
}
