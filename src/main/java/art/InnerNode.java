package art;

// should be 16 bytes only

abstract class InnerNode extends AbstractNode {

	static final int PESSIMISTIC_PATH_COMPRESSION_LIMIT = 8;

	// max limit of 8 bytes (Pessimistic)
	final byte[] prefixKeys;

	// Optimistic
	int prefixLen; // 4 bytes

	// to decide to grow or not
	// TODO: we could save space by making this a short for Node256 and byte for other node types?
	// since noOfChildren will never be more than 256 and we don't seem to be
	// using it specifically on an AbstractNode level? (are we?)
	short noOfChildren; // 2 bytes

	InnerNode() {
		prefixKeys = new byte[PESSIMISTIC_PATH_COMPRESSION_LIMIT];
	}

	// copy ctor. called when growing
	InnerNode(InnerNode node) {
		// copy header
		this.noOfChildren = node.noOfChildren;
		this.prefixLen = node.prefixLen;
		this.prefixKeys = node.prefixKeys;
	}

	byte[] getValidPrefixKey() {
		int limit = Math.min(PESSIMISTIC_PATH_COMPRESSION_LIMIT, prefixLen);
		byte[] valid = new byte[limit];
		System.arraycopy(prefixKeys, 0, valid, 0, limit);
		return valid;
	}

}
