package art;

class BinaryComparableUtils {

	// https://stackoverflow.com/a/8916809/3804127
	// an invalid byte for UTF-8, ASCII
	private static final byte TERMINATOR = (byte) 0xff;

	static byte[] terminated(byte[] key){
		byte[] terminated = new byte[key.length + 1];
		System.arraycopy(key, 0, terminated, 0, key.length);
		terminated[key.length] = TERMINATOR;
		return terminated;
	}

	// 2^7 = 128
	private static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

	static byte[] unsigned(byte[] key) {
		key[0] = unsigned(key[0]);
		return key;
	}

	// For Node4, Node16
	static byte unsigned(byte b) {
		return (byte) (b ^ BYTE_SHIFT);
	}

	static byte signed(byte b) {
		return unsigned(b);
	}
}
