package art;

public class Node48 extends AbstractNode{
	/*
		48 * 8 (child pointers) + 256 = 640 bytes
	 */

    private final Node child[] = new Node[48];

    // for partial keys of one byte size, you index directly into this array to find the
	// array index of the child pointer array
	// the index value can only be between 0 to 47 (to index into the child pointer array)
    private final byte[] keyIndex = new byte[256];

	@Override
	public Node findChild(byte partialKey) {
		int index = Byte.toUnsignedInt(partialKey);
		byte intoChild = keyIndex[index];
		assert intoChild >= 0 && intoChild <= 47;
		return child[intoChild];
	}

	@Override
	public void addChild(byte partialKey, Node child) {

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
