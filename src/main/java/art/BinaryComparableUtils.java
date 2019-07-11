package art;

import java.io.ByteArrayOutputStream;

class BinaryComparableUtils {
	// terminator should be the smallest allowed byte value to obey binary comparability of strings
	static final byte[] TERMINATOR = new byte[] {0, 0};
	// smallest allowed byte value 0 will have to be mapped to 01
	// see paper's null value idea
	static final byte[] ZERO = new byte[] {0, 1};

	// in UTF-8 encoding, other than the 0 byte from the ASCII subset
	// no codepoint ever will have a 0 byte!
	// because of the continuation bytes which always have a 1 MSB.
	// so this is safe to do
	static byte[] terminateUTF8(byte[] key) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(key.length + 2);
		int i = 0;
		while (true) {
			// find a 0 byte
			int prev = i;
			for (; i < key.length && key[i] != 0; i++) ;
			if (i < key.length) { // found a 0 byte at position i
				bytes.write(key, prev, i - prev);
				bytes.write(ZERO, 0, ZERO.length);
				i++;
			}
			else {
				// last portion to copy
				bytes.write(key, prev, i - prev);
				break;
			}
		}
		// add terminator
		bytes.write(TERMINATOR, 0, TERMINATOR.length);
		return bytes.toByteArray();
	}

	// 2^7 = 128
	private static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

	static byte[] unsigned(byte[] key) {
		key[0] = unsigned(key[0]);
		return key;
	}

	// For Node4, Node16 to interpret every byte as unsigned
	static byte unsigned(byte b) {
		return (byte) (b ^ BYTE_SHIFT);
	}

	// passed b must have been interpreted as unsigned already
	// this is the reverse of unsigned
	static byte signed(byte b) {
		return unsigned(b);
	}
}
