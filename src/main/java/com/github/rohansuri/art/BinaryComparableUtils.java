package com.github.rohansuri.art;

import java.io.ByteArrayOutputStream;

class BinaryComparableUtils {
	private BinaryComparableUtils() {
		// Effective Java Item 4
		throw new AssertionError();
	}

	// terminator should be the smallest allowed byte value to obey binary comparability of strings
	static final byte[] TERMINATOR = new byte[] {0, 0};
	// smallest allowed byte value 0 will have to be mapped to 01
	// see paper's null value idea
	static final byte[] ZERO = new byte[] {0, 1};

	private static int countZeroBytes(byte[] key) {
		int count = 0;
		for (int i = 0; i < key.length; i++) {
			count += key[i] == 0 ? 1 : 0;
		}
		return count;
	}

	// in UTF-8 encoding, other than the 0 byte from the ASCII subset
	// no codepoint ever will have a 0 byte!
	// because of the continuation bytes which always have a 1 MSB.
	// so this is safe to do
	static byte[] terminateUTF8(byte[] key) {
		int count = countZeroBytes(key);
		int end = key.length + count; // to add ZERO byte 1
		byte[] terminatedKey = new byte[end + 2];
		if (count == 0) {
			System.arraycopy(key, 0, terminatedKey, 0, key.length);
		}
		else {
			for (int i = 0, j = 0; i < key.length; i++, j++) {
				terminatedKey[j] = key[i];
				if (key[i] == 0) {
					terminatedKey[++j] = 1;
				}
			}
		}
		terminatedKey[end] = 0;
		terminatedKey[end + 1] = 0;
		return terminatedKey;
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