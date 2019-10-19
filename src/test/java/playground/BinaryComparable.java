package playground;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;

public interface BinaryComparable<K> {
	Slice get(K key);
}

interface Slice {
	byte get(int position);

	int length();
}

class SliceImpl implements Slice {
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

class StringBinaryComparable implements BinaryComparable<String> {
	private final CharsetEncoder enc;
	private ByteBuffer buffer;
	private final SliceImpl slice;
	private final int maxBytesPerChar;
	private static final int GROW_FACTOR = 2;
	private static final int DEFAULT_SIZE = 32;

	// TODO: option to give in initial expected size
	public StringBinaryComparable(Charset charset) {
		enc = charset.newEncoder();
		this.maxBytesPerChar = (int) Math.ceil(enc.maxBytesPerChar());
		buffer = ByteBuffer.allocate(DEFAULT_SIZE * Math.max(2, maxBytesPerChar));
		slice = new SliceImpl();
		slice.setB(buffer.array());
	}

	private void ensureCapacity(int maxLength) {
		if (buffer.capacity() < maxLength) {
			buffer = ByteBuffer.allocate(GROW_FACTOR * maxLength);
			slice.setB(buffer.array());
		}
	}

	@Override
	public Slice get(String key) {
		enc.reset();
		CharBuffer in = CharBuffer.wrap(key);
		buffer.clear();
		// no way to hook into source buffer or dest buffer
		// to know if a 0 byte was written
		// so that we can write a 1 byte.
		ensureCapacity((key.length() + 2) * Math.max(2, maxBytesPerChar));
		CoderResult result = enc.encode(in, buffer, true);
		if (result != CoderResult.UNDERFLOW) {
			throw new RuntimeException("unexpected encoding result " + result);
		}
		enc.flush(buffer);
		// mark terminal
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		slice.setLength(buffer.position());
		return slice;
	}
}
