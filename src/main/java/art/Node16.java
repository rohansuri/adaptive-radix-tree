package art;

import java.util.Arrays;

public class Node16 extends AbstractNode {
    private final Node[] child = new Node[16];
    private final byte[] keys = new byte[16];

    public Node16(Node4 node){
    	super(node);
		byte[] keys = node.getKeys();
		Node[] child = node.getChild();
		System.arraycopy(keys, 0, this.keys, 0, keys.length);
		System.arraycopy(child, 0, this.child, 0, child.length);
	}

	@Override
	public Node findChild(byte partialKey) {
		// binary search for key
		// having the from and to gives us only a valid view into what are the
		// valid array elements that actually have keys and are not ABSENT
		int index = Arrays.binarySearch(keys, 0, noOfChildren, partialKey);
		if (index < 0) {
			return null;
		}
		return child[index];
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		return false;
	}

	@Override
	public Node grow() {
		Node node = new Node48(this);
		return node;
	}

	public byte[] getKeys(){
    	return keys;
	}

	public Node[] getChild(){
    	return child;
	}
}
