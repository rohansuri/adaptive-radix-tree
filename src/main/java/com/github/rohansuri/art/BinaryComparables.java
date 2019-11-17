package com.github.rohansuri.art;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.github.rohansuri.art.BinaryComparableUtils.unsigned;

// noninstantiable companion class
public class BinaryComparables {

	private BinaryComparables() {
		throw new AssertionError();
	}

	public static BinaryComparable<Integer> forInteger() {
		return INTEGER;
	}

	public static BinaryComparable<Long> forLong() {
		return LONG;
	}

	static BinaryComparable<Short> forShort() {
		return SHORT;
	}

	static BinaryComparable<Byte> forByte() {
		return BYTE;
	}

	public static BinaryComparable<String> forString() {
		return String::getBytes;
	}

	/**
	 * Uses {@link String#getBytes(Charset)} to get bytes in the lexicographic order of Unicode code points,
	 * as defined in {@link String#compareTo(String)} <br>
	 * Note: Use Collators if you want locale dependent comparisons
	 * @see <a href="https://docs.oracle.com/javase/tutorial/i18n/text/collationintro.html">Collator</a>
	 */
	public static BinaryComparable<String> forString(Charset charset){
		return (key) -> key.getBytes(charset);
	}


	private static final BinaryComparable<Integer> INTEGER = (key) -> BinaryComparableUtils
			.unsigned(ByteBuffer.allocate(Integer.BYTES).putInt(key).array());
	private static final BinaryComparable<Long> LONG = (key) -> BinaryComparableUtils
			.unsigned(ByteBuffer.allocate(Long.BYTES).putLong(key).array());
	private static final BinaryComparable<Short> SHORT = (key) -> BinaryComparableUtils
			.unsigned(ByteBuffer.allocate(Short.BYTES).putShort(key).array());
	private static final BinaryComparable<Byte> BYTE = (key) -> BinaryComparableUtils
			.unsigned(ByteBuffer.allocate(Byte.BYTES).put(key).array());
	/*
	 extract from https://docs.oracle.com/javase/tutorial/i18n/text/collationintro.html:
	 If your application audience is limited to people who speak English,
	 you can probably perform string comparisons with the String.compareTo method.
	 The String.compareTo method performs a binary comparison of the Unicode characters within the two strings.
	 For most languages, however, this binary comparison cannot be relied on to sort strings,
	 because the Unicode values do not correspond to the relative order of the characters.
	 */
}
