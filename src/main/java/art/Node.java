package art;

public interface Node {
	/*
		what are the operations to be done on a Node?
		you can construct them
		search if a partial key exists in them or not, if it does then give back the next child pointer
		...
	 */

	Node findChild(byte partialKey);
	void addChild(byte partialKey, Node child);
}
