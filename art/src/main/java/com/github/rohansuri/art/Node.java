package com.github.rohansuri.art;

abstract class Node {
	/**
	 * @return child pointer for the smallest cursor stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	abstract Node first();

	abstract Node firstOrLeaf();

	/**
	 * @return child pointer for the largest cursor stored in this Node.
	 * 			Returns null if this node has no children.
	 */
	abstract Node last();

	// for upwards traversal
	// dev note: wherever you setup downlinks, you setup uplinks as well
	private InnerNode parent;
	private byte cursor;

	Node(){}

	// copy ctor. called when growing/shrinking
	Node(Node node) {
		this.cursor = node.cursor;
		this.parent = node.parent;
	}

	// do we need partial key for leaf nodes? we'll find out
	static void createUplink(InnerNode parent, LeafNode<?, ?> child) {
		Node c = child;
		c.parent = parent;
	}

	static void createUplink(InnerNode parent, Node child, byte cursor) {
		child.parent = parent;
		child.cursor = cursor;
	}

	static void setCursor(Node node, byte cursor){
		node.cursor = cursor;
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
	public byte cursor() {
		return cursor;
	}

	byte uplinkKey(){
		if(parent instanceof Node4){
			return BinaryComparableUtils.signed(((Node4)parent).getKeys()[cursor]);
		} if(parent instanceof Node16) {
			return BinaryComparableUtils.signed(((Node16)parent).getKeys()[cursor]);
		}
		return cursor;
	}
}
