package com.github.rohansuri.art;

public class StringBinaryComparable2 implements BinaryComparable<String> {
	/*
		so we want to replace occurences of 0 byte with bytes 01.
		best way would be to hook into encoding process (getBytes)
		and whenever we write a 0 byte, we write 1 as well.
		most efficient. would require no allocations we can reuse a big buffer.
		and string's bytes would be read only once
	 */
	@Override
	public byte[] get(String key) {
		key = key.replace("\u0000", "\u0000\u0001");
		// terminate with 00
		return null;
	}
}
