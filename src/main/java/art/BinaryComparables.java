package art;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static art.BinaryComparableUtils.terminateUTF8;
import static art.BinaryComparableUtils.unsigned;

public class BinaryComparables {

	private static final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);

	private static ByteBuffer getBuffer(){
		buffer.clear();
		return buffer;
	}

	public static final BinaryComparable<Integer> INTEGER = (key) -> unsigned(getBuffer().putInt(key).array());
	public static final BinaryComparable<Long> LONG = (key) -> unsigned(ByteBuffer.allocate(Long.BYTES).putLong(key).array());
	public static final BinaryComparable<Short> SHORT = (key) -> unsigned(ByteBuffer.allocate(Short.BYTES).putShort(key).array());
	public static final BinaryComparable<Byte> BYTE = (key) -> unsigned(ByteBuffer.allocate(Byte.BYTES).put(key).array());
	/*
	 extract from https://docs.oracle.com/javase/tutorial/i18n/text/collationintro.html:
	 If your application audience is limited to people who speak English,
	 you can probably perform string comparisons with the String.compareTo method.
	 The String.compareTo method performs a binary comparison of the Unicode characters within the two strings.
	 For most languages, however, this binary comparison cannot be relied on to sort strings,
	 because the Unicode values do not correspond to the relative order of the characters.
	 */
	/**
	 * Uses {@link String#getBytes(Charset)} to get bytes in the lexicographic order of Unicode code points,
	 * as defined in {@link String#compareTo(String)} <br>
	 * Note: Use Collators if you want locale dependent comparisons
	 * @see <a href="https://docs.oracle.com/javase/tutorial/i18n/text/collationintro.html">Collator</a>
	 */
	// TODO: make BinaryComparable interface non exposed for now and only expose the pre-defined implementations.
	// TODO: consider switching to having a short storage in each node
	// to mark a key end (8 bytes spare) so that it works for all keys.
	// It'll also simplify the Node implementations, since they no longer have to
	// explicitly interpret everything as "unsigned"
	public static final BinaryComparable<String> UTF8 = (key) -> terminateUTF8(key.getBytes(StandardCharsets.UTF_8));
}
