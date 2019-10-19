package com.github.rohansuri.art;

import java.nio.charset.StandardCharsets;

public class StringBinaryComparable1 implements BinaryComparable<String> {
	@Override
	public byte[] get(String key) {
		return key.replace("\u0000", "\u0000\u0001")
				.concat("\u0000\u0000") // results in another copy
				.getBytes(StandardCharsets.UTF_8);
	}
}
