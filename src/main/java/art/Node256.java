package art;

public class Node256 extends AbstractNode{
    private final Node child[] = new Node[256]; // 256 * 8 bytes

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
    public void addChild(byte partialKey, Node child) {

    }
}
