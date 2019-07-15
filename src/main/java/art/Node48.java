package art;

import java.util.Arrays;

class Node48 extends InnerNode {
	/*
		48 * 8 (child pointers) + 256 = 640 bytes
	*/

	static final int NODE_SIZE = 48;
	static final int KEY_INDEX_SIZE = 256;

	private final Node child[] = new Node[NODE_SIZE];

	// for partial keys of one byte size, you index directly into this array to find the
	// array index of the child pointer array
	// the index value can only be between 0 to 47 (to index into the child pointer array)
	private final byte[] keyIndex = new byte[KEY_INDEX_SIZE];

	// so that when you use the partial key to index into keyIndex
	// and you see a -1, you know there's no mapping for this key
	static final byte ABSENT = -1;

	Node48(Node16 node) {
		super(node);
		if (!node.isFull()) {
			throw new IllegalArgumentException("Given Node16 still has capacity, cannot grow into Node48.");
		}

		Arrays.fill(keyIndex, ABSENT);

		byte[] keys = node.getKeys();
		Node[] child = node.getChild();

		for (int i = 0; i < Node16.NODE_SIZE; i++) {
			byte key = BinaryComparableUtils.signed(keys[i]);
			int index = Byte.toUnsignedInt(key);
			keyIndex[index] = (byte) i;
			this.child[i] = child[i];
			// update up link
			replaceUplink(this, this.child[i]);
		}
	}

	Node48(Node256 node256) {
		super(node256);
		if (!node256.shouldShrink()) {
			throw new IllegalArgumentException("Given Node256 hasn't crossed shrinking threshold yet");
		}
		Arrays.fill(keyIndex, ABSENT);

		Node[] children = node256.getChild();
		byte j = 0;
		for (int i = 0; i < Node256.NODE_SIZE; i++) {
			if (children[i] != null) {
				keyIndex[i] = j;
				child[j] = children[i];
				replaceUplink(this, child[j]);
				j++;
			}
		}
		assert j == NODE_SIZE;
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
		if (isFull()) {
			return false;
		}
		int index = Byte.toUnsignedInt(partialKey);
		if (keyIndex[index] != ABSENT) {
			throw new IllegalArgumentException("Cannot insert partial key " + partialKey + " that already exists in Node. "
					+ "If you want to replace the associated child pointer, use Node#replace(byte, Node)");

		}
		// find a null place, left fragmented by a removeChild or has always been null
		byte insertPosition = 0;
		for (; this.child[insertPosition] != null && insertPosition < NODE_SIZE; insertPosition++) ;
		assert insertPosition < NODE_SIZE;

		this.child[insertPosition] = child;
		keyIndex[index] = insertPosition;
		noOfChildren++;
		createUplink(this, child, partialKey);
		return true;
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		byte index = keyIndex[Byte.toUnsignedInt(partialKey)];
		if (!(index >= 0 && index <= 47)) {
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		removeUplink(child[index]);
		child[index] = newChild;
		createUplink(this, newChild, partialKey);
	}

	@Override
	public void removeChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		int pos = keyIndex[index];
		if (pos == -1) {
			throw new IllegalArgumentException("Partial key " + partialKey + " does not exist in this Node.");
		}
		removeUplink(child[pos]);
		child[pos] = null; // fragment
		keyIndex[index] = ABSENT;
		noOfChildren--;
	}

	@Override
	public Node grow() {
		if (!isFull()) {
			throw new IllegalStateException("Grow should be called only when you reach a node's full capacity");
		}
		Node node = new Node256(this);
		return node;
	}

	@Override
	public boolean shouldShrink() {
		return noOfChildren == Node16.NODE_SIZE;
	}

	@Override
	public Node shrink() {
		if (!shouldShrink()) {
			throw new IllegalStateException("Haven't crossed shrinking threshold yet");
		}
		Node16 node16 = new Node16(this);
		return node16;
	}

	@Override
	public Node first() {
		if (noOfChildren == 0) {
			return null;
		}
		for (int i = 0; i < KEY_INDEX_SIZE; i++) {
			byte index = keyIndex[i];
			if (index != ABSENT) {
				return child[index];
			}
		}
		return null;
	}

	@Override
	public Node last() {
		if (noOfChildren == 0) {
			return null;
		}
		for (int i = KEY_INDEX_SIZE - 1; i >= 0; i--) {
			byte index = keyIndex[i];
			if (index != ABSENT) {
				return child[index];
			}
		}
		return null;
	}

	@Override
	public boolean isFull() {
		return noOfChildren == NODE_SIZE;
	}

	@Override
	public Node greater(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) + 1; i < KEY_INDEX_SIZE; i++) {
			if (keyIndex[i] != ABSENT) {
				return child[keyIndex[i]];
			}
		}
		return null;
	}

	@Override
	public Node lesser(byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) - 1; i >= 0; i--) {
			if (keyIndex[i] != ABSENT) {
				return child[keyIndex[i]];
			}
		}
		return null;
	}


	byte[] getKeyIndex() {
		return keyIndex;
	}

	Node[] getChild() {
		return child;
	}
}