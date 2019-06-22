package art;

class Node256 extends InnerNode {
	static final int NODE_SIZE = 256;
	private final Node child[] = new Node[NODE_SIZE]; // 256 * 8 bytes

	Node256(Node48 node) {
		super(node);
		if (!node.isFull()) {
			throw new IllegalArgumentException("Given Node48 still has capacity, cannot grow into Node256.");
		}

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
		// We should treat the 8 bits as unsigned int
		// since we've got 256 slots, we need to go from 00000000 to 11111111
		int index = Byte.toUnsignedInt(partialKey);
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (isFull()) {
			return false;
		}
		// byte in Java is signed
		// but we want no interpretation of the partialKey
		// we just want to treat it as raw binary bits
		// but since byte is signed, numerically when we index using it
		// it can be negative once it goes over 127, therefore we need to
		// convert it to a bigger container type
		// or can we do something better?
		int index = Byte.toUnsignedInt(partialKey);
		if(this.child[index] != null) {
			throw new IllegalArgumentException("Cannot insert partial key " + partialKey + " that already exists in Node. "
					+ "If you want to replace the associated child pointer, use Node#replace(byte, Node)");

		}
		this.child[index] = child;
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		int index = Byte.toUnsignedInt(partialKey);
		if(child[index] == null) {
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		child[index] = newChild;
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		if(child[index] == null){
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		child[index] = null;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		throw new IllegalStateException("Span of ART is 8 bits, so Node256 is the largest node type.");
	}

	@Override
	public boolean shouldShrink() {
		return noOfChildren == Node48.NODE_SIZE;
	}

	@Override
	public Node shrink() {
		if(!shouldShrink()){
			throw new IllegalStateException("Haven't crossed shrinking threshold yet");
		}
		Node48 node48 = new Node48(this);
		return node48;
	}

	@Override
	public Node first() {
		if(noOfChildren == 0){
			return null;
		}
		for(int i = 0; i < NODE_SIZE; i++){
			if(child[i] != null){
				return child[i];
			}
		}
		return null;
	}

	@Override
	public Node last() {
		if(noOfChildren == 0){
			return null;
		}
		for(int i = NODE_SIZE - 1; i >= 0; i--){
			if(child[i] != null){
				return child[i];
			}
		}
		return null;
	}

	@Override
	public boolean isFull() {
		return noOfChildren == NODE_SIZE;
	}

	Node[] getChild() {
		return child;
	}

}
