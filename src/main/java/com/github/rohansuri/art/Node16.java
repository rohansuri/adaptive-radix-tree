package com.github.rohansuri.art;

import java.util.Arrays;

class Node16 extends InnerNode {
	static final int NODE_SIZE = 16;
	final byte[] keys = new byte[NODE_SIZE];

	Node16(Node4 node) {
		super(node, NODE_SIZE);
		assert Node4.isFull(node);
		byte[] keys = Node4.getKeys(node);
		Node[] child = node.getChild();
		System.arraycopy(keys, 0, this.keys, 0, node.noOfChildren);
		System.arraycopy(child, 0, this.child, 0, node.noOfChildren);

		// update up links
		for (int i = 0; i < noOfChildren; i++) {
			replaceUplink(this, this.child[i]);
		}
	}

	Node16(Node48 node48) {
		super(node48, NODE_SIZE);
		assert Node48.shouldShrink(node48);
		byte[] keyIndex = Node48.getKeyIndex(node48);
		Node[] children = node48.getChild();

		// keyIndex by virtue of being "array indexed" is already sorted
		// so we can iterate and keep adding into Node16
		for (int i = 0, j = 0; i < Node48.KEY_INDEX_SIZE; i++) {
			if (keyIndex[i] != Node48.ABSENT) {
				child[j] = children[keyIndex[i]];
				keys[j] = BinaryComparableUtils.unsigned(child[j].uplinkKey());
				replaceUplink(this, child[j]);
				j++;
			}
		}
	}

	public static Node findChild(Node16 node16, byte partialKey) {
		// TODO: use simple loop to see if -XX:+SuperWord applies SIMD JVM instrinsics
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		for(int i = 0; i < node16.noOfChildren; i++){
			if(node16.keys[i] == partialKey){
				return node16.child[i];
			}
		}
		return null;
	}

	public static boolean addChild(Node16 node16, byte partialKey, Node child) {
		if (Node16.isFull(node16)) {
			return false;
		}
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);

		int index = Arrays.binarySearch(node16.keys, 0, node16.noOfChildren, unsignedPartialKey);
		// the partialKey should not exist
		assert index < 0;
		int insertionPoint = -(index + 1);
		// shift elements from this point to right by one place
		assert insertionPoint <= node16.noOfChildren;
		for (int i = node16.noOfChildren; i > insertionPoint; i--) {
			node16.keys[i] = node16.keys[i - 1];
			node16.child[i] = node16.child[i - 1];
		}
		node16.keys[insertionPoint] = unsignedPartialKey;
		node16.child[insertionPoint] = child;
		node16.noOfChildren++;
		createUplink(node16, child, partialKey);
		return true;
	}

	public static void replace(Node16 node16, byte partialKey, Node newChild) {
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = Arrays.binarySearch(node16.keys, 0, node16.noOfChildren, unsignedPartialKey);
		assert index >= 0;
		node16.child[index] = newChild;
		createUplink(node16, newChild, partialKey);
	}

	public static void removeChild(Node16 node16, byte partialKey) {
		assert !Node16.shouldShrink(node16);
		byte unsignedPartialKey = BinaryComparableUtils.unsigned(partialKey);
		int index = Arrays.binarySearch(node16.keys, 0, node16.noOfChildren, unsignedPartialKey);
		// if this fails, the question is, how could you reach the leaf node?
		// this node must've been your follow on pointer holding the partialKey
		assert index >= 0;
		removeUplink(node16.child[index]);
		for (int i = index; i < node16.noOfChildren - 1; i++) {
			node16.keys[i] = node16.keys[i + 1];
			node16.child[i] = node16.child[i + 1];
		}
		node16.child[node16.noOfChildren - 1] = null;
		node16.noOfChildren--;
	}

	public static InnerNode grow(Node16 node16) {
		assert Node16.isFull(node16);
		return new Node48(node16);
	}

	public static boolean shouldShrink(Node16 node16) {
		return node16.noOfChildren == Node4.NODE_SIZE;
	}

	public static InnerNode shrink(Node16 node16) {
		assert shouldShrink(node16) : "Haven't crossed shrinking threshold yet";
		return new Node4(node16);
	}

	public static Node first(Node16 node16) {
		assert node16.noOfChildren > Node4.NODE_SIZE;
		return node16.child[0];
	}

	public static Node last(Node16 node16) {
		assert node16.noOfChildren > Node4.NODE_SIZE;
		return node16.child[node16.noOfChildren - 1];
	}

	public static Node greater(Node16 node16, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// TODO: consider using binary search here
		for (int i = 0; i < node16.noOfChildren; i++) {
			if (node16.keys[i] > partialKey) {
				return node16.child[i];
			}
		}
		return null;
	}

	public static Node lesser(Node16 node16, byte partialKey) {
		partialKey = BinaryComparableUtils.unsigned(partialKey);
		// TODO: consider using binary search here
		for (int i = node16.noOfChildren - 1; i >= 0; i--) {
			if (node16.keys[i] < partialKey) {
				return node16.child[i];
			}
		}
		return null;
	}

	public static boolean isFull(Node16 node16) {
		return node16.noOfChildren == NODE_SIZE;
	}

	static byte[] getKeys(Node16 node16) {
		return node16.keys;
	}
}
