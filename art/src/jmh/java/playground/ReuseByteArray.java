package playground;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

interface BinaryComparable<K> {
	ByteSlice get(K key);
}

interface ByteSlice {
	byte get(int position);

	int length();
}

class ByteSliceImpl implements ByteSlice {
	private byte[] b;
	private int length;

	public void setB(byte[] b) {
		this.b = b;
	}

	@Override
	public byte get(int position) {
		return b[position];
	}

	@Override
	public int length() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
}

// stateful, hence unsafe for multi threaded use.
// each thread must use it's own instance.
class StringBinaryComparable implements BinaryComparable<String> {
	private final CharsetEncoder enc;
	private ByteBuffer buffer;
	private final ByteSliceImpl slice;
	private final int maxBytesPerChar;
	private static final int GROW_FACTOR = 2;
	private static final int DEFAULT_SIZE = 32;

	// TODO: option to give in initial expected size
	public StringBinaryComparable(Charset charset) {
		enc = charset.newEncoder();
		this.maxBytesPerChar = (int) Math.ceil(enc.maxBytesPerChar());
		buffer = ByteBuffer.allocate(DEFAULT_SIZE * maxBytesPerChar);
		slice = new ByteSliceImpl();
		slice.setB(buffer.array());
	}

	private void ensureCapacity(int maxLength) {
		if (buffer.capacity() < maxLength) {
			buffer = ByteBuffer.allocate(GROW_FACTOR * maxLength);
			slice.setB(buffer.array());
		}
	}

	@Override
	public ByteSlice get(String key) {
		enc.reset();
		CharBuffer in = CharBuffer.wrap(key);
		buffer.clear();
		ensureCapacity(key.length() * maxBytesPerChar);
		CoderResult result = enc.encode(in, buffer, true);
		if (result != CoderResult.UNDERFLOW) {
			throw new RuntimeException("unexpected encoding result " + result);
		}
		enc.flush(buffer);
		slice.setLength(buffer.position());
		return slice;
	}
}

class StringBinaryComparable2 implements BinaryComparable<String> {
	private final CharsetEncoder enc;
	private ByteBuffer buffer;
	private CharBuffer charBuffer;
	private final ByteSliceImpl slice;
	private final int maxBytesPerChar;
	private static final int GROW_FACTOR = 2;
	private static final int DEFAULT_SIZE = 64;

	// TODO: option to give in initial expected size
	public StringBinaryComparable2(Charset charset) {
		enc = charset.newEncoder();
		this.maxBytesPerChar = (int) Math.ceil(enc.maxBytesPerChar());
		buffer = ByteBuffer.allocate(DEFAULT_SIZE * maxBytesPerChar);
		charBuffer = CharBuffer.allocate(DEFAULT_SIZE);
		slice = new ByteSliceImpl();
		slice.setB(buffer.array());
	}

	private void ensureCapacity(int maxLength) {
		if (buffer.capacity() < maxLength) {
			buffer = ByteBuffer.allocate(GROW_FACTOR * maxLength);
			slice.setB(buffer.array());
		}
	}

	private void ensureCapacityCharBuffer(int length) {
		if (charBuffer.capacity() < length) {
			charBuffer = CharBuffer.allocate(GROW_FACTOR * length);
		}
	}

	@Override
	public ByteSlice get(String key) {
		enc.reset();
		ensureCapacityCharBuffer(key.length());
		key.getChars(0, key.length(), charBuffer.array(), 0);
		buffer.clear();
		ensureCapacity(key.length() * maxBytesPerChar);
		CoderResult result = enc.encode(charBuffer, buffer, true);
		if (result != CoderResult.UNDERFLOW) {
			throw new RuntimeException("unexpected encoding result " + result);
		}
		enc.flush(buffer);
		slice.setLength(buffer.position());
		return slice;
	}
}
