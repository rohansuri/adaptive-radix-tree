package com.github.rohansuri.art;

abstract class AbstractNode extends Node {
	// for upwards traversal
	// dev note: wherever you setup downlinks, you setup uplinks as well
	private Node parent;
	private byte partialKey;

	AbstractNode() {
	}

	// copy ctor. called when growing/shrinking
	AbstractNode(AbstractNode node) {
		this.partialKey = node.partialKey;
		this.parent = node.parent;
	}

	static void createUplink(Node parent, Node child, byte partialKey) {
		AbstractNode c = (AbstractNode) child;
		c.parent = parent;
		c.partialKey = partialKey;
	}

	// called when growing/shrinking and all children now have a new parent
	static void replaceUplink(Node parent, Node child) {
		AbstractNode c = (AbstractNode) child;
		c.parent = parent;
	}

	static void removeUplink(Node child) {
		AbstractNode c = (AbstractNode) child;
		c.parent = null;
	}

	@Override
	public Node parent() {
		return parent;
	}

	@Override
	public byte uplinkKey() {
		return partialKey;
	}
}
