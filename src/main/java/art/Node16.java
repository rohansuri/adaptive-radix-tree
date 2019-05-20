package art;

import java.util.Arrays;

public class Node16 extends AbstractNode {
    private final Node[] child = new Node[16];
    private final byte[] keys = new byte[16];

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
	public void addChild(byte partialKey, Node child) {

	}
}
