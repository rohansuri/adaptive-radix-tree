package art;

abstract class AbstractNode implements Node {
	// for upwards traversal
	// dev note: wherever you setup downlinks, you setup uplinks as well
	Node parent;
	byte partialKey;
}
