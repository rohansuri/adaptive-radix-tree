package art;

// should be 16 bytes only

public abstract class AbstractNode implements Node {

	static final int PESSIMISTIC_PATH_COMPRESSION_LIMIT = 8;

	// max limit of 8 bytes (Pessimistic)
	final byte[] prefixKeys;

	// Optimistic
	int prefixLen; // 4 bytes

	// to decide to grow or not
	// TODO: we could save space by making this a short for Node256 and byte for other node types?
	// since noOfChildren will never be more than 256 and we don't seem to be
	// using it specifically on an AbstractNode level? (are we?)
	int noOfChildren; // 4 bytes

	void setPrefix(int prefixLen, byte[] key, int depth) {
		this.prefixLen = prefixLen;
		System.arraycopy(key, depth, this.prefixKeys, 0, Math.min(PESSIMISTIC_PATH_COMPRESSION_LIMIT, prefixLen));
	}

	public AbstractNode() {
		prefixKeys = new byte[PESSIMISTIC_PATH_COMPRESSION_LIMIT];

	}

	// copy ctor. called when growing
	public AbstractNode(AbstractNode node) {
		// copy header
		this.noOfChildren = node.noOfChildren;
		this.prefixLen = node.prefixLen;
		this.prefixKeys = node.prefixKeys;
	}
}

 /*
        path compressed keys

        would this array be fixed or dynamically growing?
        I think fixed again
        since when we path compress we know the number of chars to compress

        but what if lets say we have a path compressed
        then we add a new char to that

        F ---> path compressed ---> O
                               \---> Z

        I add a string with F"same chars as path compressed" then O then S?
        you go via the same compression

        essentially the case to think about is
        if the existing prefixKeys would be recreated (grown) by simply appending newer chars to it?

        F "same chars as path compressed" O S ?
        i.e.

        F ---> path compressed ---> O ---> S
                               \---> Z

        this would get lazy expanded actually

        F ---> path compressed ---> "OS"
                               \---> Z

        I guess this one is complex to "pre-think"
        Lets see when we code if such situations arise
     */

/*
    span is always 8 bits
    But nodes are adaptive
    An inner node should grow from 4 -> 16 -> 48 -> 256

    Node could be a LeafNode too having a pointer to value
    C has raw void pointers which could point to any pointer
    To know which to typecast into before usage we could use 16bits left from the 64bit pointers
    Since modern CPUs only addresses in 48bits range

    How would we fit this flag in Java?
    (we could using Unsafe allocateMemory and fool around there, but to keep things simplistic
    lets not go there right now. Maybe in future.)

    Anyways for now we treat leaves as a separate node
 */
