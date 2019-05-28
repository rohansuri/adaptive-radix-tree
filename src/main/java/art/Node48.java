package art;

import java.util.Arrays;

class Node48 extends AbstractNode {
	/*
		48 * 8 (child pointers) + 256 = 640 bytes
	*/

	private static final int NODE_SIZE = 48;

	private final Node child[] = new Node[NODE_SIZE];

	// for partial keys of one byte size, you index directly into this array to find the
	// array index of the child pointer array
	// the index value can only be between 0 to 47 (to index into the child pointer array)
	private final byte[] keyIndex = new byte[256];

	// so that when you use the partial key to index into keyIndex
	// and you see a -1, you know there's no mapping for this key
	static final byte ABSENT = -1;

	Node48(Node16 node) {
		super(node);
		Arrays.fill(keyIndex, ABSENT);

		byte[] keys = node.getKeys();
		Node[] child = node.getChild();

		for (int i = 0; i < 16; i++) {
			byte key = keys[i];
			int index = Byte.toUnsignedInt(key);
			keyIndex[index] = (byte) i;
			this.child[i] = child[i];
		}
	}

	@Override
	public Node findChild(byte partialKey) {
		byte index = keyIndex[Byte.toUnsignedInt(partialKey)];
		if (index == ABSENT) {
			return null;
		}

		assert index >= 0 && index <= 47;
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		if (noOfChildren == NODE_SIZE) {
			return false;
		}
		int index = Byte.toUnsignedInt(partialKey);
		assert keyIndex[index] == -1;

		// find a null place, left fragmented by a removeChild or has always been null
		byte insertPosition = 0;
		for(;this.child[insertPosition] != null && insertPosition < NODE_SIZE; insertPosition++);
		assert insertPosition < NODE_SIZE;

		this.child[insertPosition] = child;
		keyIndex[index] = insertPosition;
		noOfChildren++;
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		byte index = keyIndex[Byte.toUnsignedInt(partialKey)];
		assert index >= 0 && index <= 47;
		child[index] = newChild;
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		int pos = keyIndex[index];
		assert pos != -1;

		child[pos] = null; // fragment
		keyIndex[index] = ABSENT;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		Node node = new Node256(this);
		return node;
	}


	byte[] getKeyIndex() {
		return keyIndex;
	}

	Node[] getChild() {
		return child;
	}
}

/*
    other nodes:
    key = [a, b, c, d]
    I'd look up by binary searching for them

    but with 48 my key size has grown, so rather than binary searching
    (log 48 base 2 = 5.58.. about 6 comparisons)
    I index into an array which will tell me the position in the child pointer array
    since as my child pointers grow (tree becomes fat) I don't want to spend time in searching
    for the right "next-child" pointer
 */
