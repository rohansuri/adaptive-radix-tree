package art;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static art.BinaryComparableUtils.terminated;
import static art.BinaryComparableUtils.unsigned;

public interface BinaryComparable<K> {
	byte[] get(K key);

	BinaryComparable<Integer> INTEGER = (key) -> unsigned(ByteBuffer.allocate(Integer.BYTES).putInt(key).array());
	BinaryComparable<Long> LONG = (key) -> unsigned(ByteBuffer.allocate(Long.BYTES).putLong(key).array());
	BinaryComparable<Short> SHORT = (key) -> unsigned(ByteBuffer.allocate(Short.BYTES).putShort(key).array());
	BinaryComparable<Byte> BYTE = (key) -> unsigned(ByteBuffer.allocate(Byte.BYTES).put(key).array());
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
	BinaryComparable<String> UTF8 = (key) -> terminated(key.getBytes(StandardCharsets.UTF_8));
}

