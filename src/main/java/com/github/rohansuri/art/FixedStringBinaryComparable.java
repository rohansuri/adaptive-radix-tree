package com.github.rohansuri.art;

import java.nio.charset.StandardCharsets;

public class FixedStringBinaryComparable implements BinaryComparable<String> {
	@Override
	public byte[] get(String key) {
		return key.getBytes(StandardCharsets.UTF_8);
	}
}
