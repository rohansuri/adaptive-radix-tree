package art;

// these are internal contracts/interfaces
// they've been written with only what they're used for internally
// for example Node#remove could have returned a false indicative of a failed remove
// due to partialKey entry not actually existing, but the return value is of no use in code till now
// and is sure to be called from places where it'll surely exist.
// since they're internal, we could change them later if a better contract makes more sense.

/*
	TODO: explain why we throw runtime exceptions everywhere (IllegalArgumentException, IllegalStateException)
 	In short, because we don't want the code to keep continuing by catching exceptions around.
 	If you got one of these, that means you aren't using the interface correctly.
 	There needs to be proper sequencing of the right calls, or you already must make the calls when
 	you're sure you meet the condition.
 	(This may change?)

 	These exceptions are for the programmer (who is working on this code) to know that the APIs are not being used correctly.
 	All of them indicate programmer errors.
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
	 * @throws IllegalArgumentException if you try to add a partialKey which already exists
	 */
	boolean addChild(byte partialKey, Node child);

	/**
	 * @param partialKey for which the child pointer mapping is to be updated
	 * @param newChild the new mapping to be added for given partialKey
	 * @throws IllegalArgumentException if no entry exists for given partialKey
	 */
	void replace(byte partialKey, Node newChild);

	/**
	 * @param partialKey for which the child pointer mapping is to be removed
	 * @throws IllegalArgumentException if mapping for given partialKey does not exist
	 */
	void removeChild(byte partialKey);

	/**
	 * creates and returns the next larger node type with the same mappings as this node
	 * @return a new node with the same mappings
	 * @throws IllegalStateException if current node hasn't reached it's size yet
	 */
	Node grow(); // TODO: put grow inside addChild itself?

	boolean shouldShrink();

	Node shrink();

	/**
	 * @return child pointer for the smallest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	// TODO: add Node level tests
	Node first();

	/**
	 * @return child pointer for the largest partialKey stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	// TODO: add Node level tests
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
	 * @return returns the smallest child node for the partialKey strictly greater than the partialKey passed.
	 * Returns null if no such child.
	 */
	// TODO: add Node level tests
	Node greater(byte partialKey);

	/**
	 * @return returns the greatest child node for the partialKey strictly lesser than the partialKey passed.
	 * Returns null if no such child.
	 */
	// TODO: add Node level tests
	Node lesser(byte partialKey);
}
