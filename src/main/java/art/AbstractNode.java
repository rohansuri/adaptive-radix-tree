package art;

abstract class AbstractNode implements Node {
	// for upwards traversal
	// dev note: wherever you setup downlinks, you setup uplinks as well
	Node parent;
	byte partialKey;

	AbstractNode(){}

	// copy ctor. called when growing/shrinking
	AbstractNode(AbstractNode node){
		this.partialKey = node.partialKey;
		this.parent = node.parent;
	}

	static void createUplink(Node parent, Node child, byte partialKey) {
		AbstractNode c = (AbstractNode) child;
		c.parent = parent;
		c.partialKey = partialKey;
	}

	static void removeUplink(Node child) {
		AbstractNode c = (AbstractNode) child;
		c.parent = null;
	}
}
