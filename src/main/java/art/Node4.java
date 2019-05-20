package art;

import java.util.Arrays;

public class Node4 extends AbstractNode {
    private final Node[] child = new Node[4];

    // each array element would contain the partial byte key to match
	// if key matches then take up the same index from the child pointer array
    private final byte[] keys = new byte[4];

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

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
