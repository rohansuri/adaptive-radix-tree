package art;

class Node256 extends AbstractNode {
	private final Node child[] = new Node[256]; // 256 * 8 bytes

	Node256(Node48 node) {
		super(node);

		byte[] keyIndex = node.getKeyIndex();
		Node[] child = node.getChild();

		for (int i = 0; i < 256; i++) {
			byte index = keyIndex[i];
			if (index == Node48.ABSENT) {
				continue;
			}
			assert index >= 0 && index <= 47;
			// index is byte, but gets type promoted
			// https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html#jls-10.4-120
			this.child[i] = child[index];
		}
	}

	@Override
	public Node findChild(byte partialKey) {
		// convert byte to 8 bit integer
		// and then index into that array position
		// I guess we should treat the 8 bits as unsigned int
		// since we've got 256 slots, we need to go from 00000000 to 11111111
		int index = Byte.toUnsignedInt(partialKey);
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (noOfChildren == 256) {
			throw new IllegalStateException("ART span is 8 bits, so node 256 is the largest possible number of children you can have.");
		}
		// byte in Java is signed
		// but we want no interpretation of the partialKey
		// we just want to treat it as raw binary bits
		// but since byte is signed, numerically when we index using it
		// it can be negative once it goes over 127, therefore we need to
		// convert it to a bigger container type
		// or can we do something better?
		int index = Byte.toUnsignedInt(partialKey);
		assert this.child[index] == null;
		this.child[index] = child;
		// TODO: write unit tests for each node types function, verifying invariants (like addChild should increase noOfChildren by 1)
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Byte.toUnsignedInt(partialKey);
		child[index] = newChild;
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		assert child[index] != null;
		child[index] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		throw new IllegalStateException("Span of ART is 8 bits, so Node256 is the largest node type.");
	}

}
