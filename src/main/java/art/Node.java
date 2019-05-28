package art;

interface Node {
	/**
	 *
	 * @param partialKey search if this node has an entry for given partialKey
	 * @return if it does, then return the following child pointer
	 */
	Node findChild(byte partialKey);

	/**
	 * @param partialKey partialKey to be mapped
	 * @param child the child node to be added
	 * @return true if add succeeded, false if node size full (call grow)
	 */
	boolean addChild(byte partialKey, Node child);

	void replace(byte partialKey, Node newChild);

	void removeChild(byte partialKey);

	/**
	 * copies all mappings from given node
	 * @return a new node with the same mappings
	 */
	Node grow(); // TODO: put grow inside addChild itself?
}
