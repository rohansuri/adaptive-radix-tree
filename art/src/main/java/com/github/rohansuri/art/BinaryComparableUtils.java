package com.github.rohansuri.art;

/**
 * Contains utilities to help with {@link BinaryComparable} key transformation.
 */
class BinaryComparableUtils {
	private BinaryComparableUtils() {
		// Effective Java Item 4
		throw new AssertionError();
	}

	private static int countZeroBytes(byte[] key) {
		int count = 0;
		for (byte b : key) {
			count += b == 0 ? 1 : 0;
		}
		return count;
	}

	// in UTF-8 encoding, other than the 0 byte from the ASCII subset
	// no codepoint ever will have a 0 byte
	// because of the continuation bytes which always have a 1 MSB.
	static byte[] terminate(byte[] key) {
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
		// terminator should be the smallest allowed byte value to obey binary comparability
		// of variable length keys
		terminatedKey[end] = 0;
		terminatedKey[end + 1] = 0;
		return terminatedKey;
	}

	// 2^7 = 128
	private static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

	/**
	 * For signed types to be interpreted as unsigned
	 */
	static byte[] unsigned(byte[] key) {
		key[0] = unsigned(key[0]);
		return key;
	}

	/**
	 * For Node4, Node16 to interpret every byte as unsigned when storing partial keys.
	 * Node 48, Node256 simply use {@link Byte#toUnsignedInt(byte)}
	 * to index into their key arrays.
	 */
	static byte unsigned(byte b) {
		return (byte) (b ^ BYTE_SHIFT);
	}

	// passed b must have been interpreted as unsigned already
	// this is the reverse of unsigned
	static byte signed(byte b) {
		return unsigned(b);
	}
}