package com.github.rohansuri.art;

import java.util.Arrays;

class Node48 extends InnerNode {
	/*
		48 * 8 (child pointers) + 256 = 640 bytes
	*/

	static final int NODE_SIZE = 48;
	static final int KEY_INDEX_SIZE = 256;

	// for partial keys of one byte size, you index directly into this array to find the
	// array index of the child pointer array
	// the index value can only be between 0 to 47 (to index into the child pointer array)
	final byte[] keyIndex = new byte[KEY_INDEX_SIZE];

	// so that when you use the partial key to index into keyIndex
	// and you see a -1, you know there's no mapping for this key
	static final byte ABSENT = -1;

	Node48(Node16 node) {
		super(node, NODE_SIZE);
		assert Node16.isFull(node);

		Arrays.fill(keyIndex, ABSENT);

		byte[] keys = Node16.getKeys(node);
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
		super(node256, NODE_SIZE);
		assert Node256.shouldShrink(node256);
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

	public static Node findChild(Node48 node48, byte partialKey) {
		byte index = node48.keyIndex[Byte.toUnsignedInt(partialKey)];
		if (index == ABSENT) {
			return null;
		}

		assert index >= 0 && index <= 47;
		return node48.child[index];
	}

	public static boolean addChild(Node48 node48, byte partialKey, Node child) {
		if (isFull(node48)) {
			return false;
		}
		int index = Byte.toUnsignedInt(partialKey);
		assert node48.keyIndex[index] == ABSENT;
		// find a null place, left fragmented by a removeChild or has always been null
		byte insertPosition = 0;
		for (; node48.child[insertPosition] != null && insertPosition < NODE_SIZE; insertPosition++) ;

		node48.child[insertPosition] = child;
		node48.keyIndex[index] = insertPosition;
		node48.noOfChildren++;
		createUplink(node48, child, partialKey);
		return true;
	}

	public static void replace(Node48 node48, byte partialKey, Node newChild) {
		byte index = node48.keyIndex[Byte.toUnsignedInt(partialKey)];
		assert index >= 0 && index <= 47;
		node48.child[index] = newChild;
		createUplink(node48, newChild, partialKey);
	}

	public static void removeChild(Node48 node48, byte partialKey) {
		assert !shouldShrink(node48);
		int index = Byte.toUnsignedInt(partialKey);
		int pos = node48.keyIndex[index];
		assert pos != ABSENT;
		removeUplink(node48.child[pos]);
		node48.child[pos] = null; // fragment
		node48.keyIndex[index] = ABSENT;
		node48.noOfChildren--;
	}

	public static InnerNode grow(Node48 node48) {
		assert isFull(node48);
		return new Node256(node48);
	}

	public static boolean shouldShrink(Node48 node48) {
		return node48.noOfChildren == Node16.NODE_SIZE;
	}

	public static InnerNode shrink(Node48 node48) {
		assert Node48.shouldShrink(node48);
		return new Node16(node48);
	}

	public static Node first(Node48 node48) {
		assert node48.noOfChildren > Node16.NODE_SIZE;
		for (int i = 0; i < KEY_INDEX_SIZE; i++) {
			byte index = node48.keyIndex[i];
			if (index != ABSENT) {
				return node48.child[index];
			}
		}
		throw new IllegalStateException("Node48 should contain more than " + Node16.NODE_SIZE + " elements");
	}

	public static Node last(Node48 node48) {
		assert node48.noOfChildren > Node16.NODE_SIZE;
		for (int i = KEY_INDEX_SIZE - 1; i >= 0; i--) {
			byte index = node48.keyIndex[i];
			if (index != ABSENT) {
				return node48.child[index];
			}
		}
		throw new IllegalStateException("Node48 should contain more than " + Node16.NODE_SIZE + " elements");
	}

	public static boolean isFull(Node48 node48) {
		return node48.noOfChildren == NODE_SIZE;
	}

	public static Node greater(Node48 node48, byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) + 1; i < KEY_INDEX_SIZE; i++) {
			if (node48.keyIndex[i] != ABSENT) {
				return node48.child[node48.keyIndex[i]];
			}
		}
		return null;
	}

	public static Node lesser(Node48 node48, byte partialKey) {
		for (int i = Byte.toUnsignedInt(partialKey) - 1; i >= 0; i--) {
			if (node48.keyIndex[i] != ABSENT) {
				return node48.child[node48.keyIndex[i]];
			}
		}
		return null;
	}


	static byte[] getKeyIndex(Node48 node48) {
		return node48.keyIndex;
	}
}