package com.github.rohansuri.art.examples.api.versioning;

import com.github.rohansuri.art.BinaryComparable;
import com.github.rohansuri.art.BinaryComparables;

public enum APIVersionBinaryComparable implements BinaryComparable<APIVersion> {
	INSTANCE;

	private BinaryComparable<Integer> bc;

	APIVersionBinaryComparable() {
		bc = BinaryComparables.forInteger();
	}

	@Override
	public byte[] get(APIVersion key) {
		// 4 bytes
		byte[] major = bc.get(key.major);

		// 4 bytes
		byte[] minor = bc.get(key.minor);

		// bytes of major followed by bytes of minor
		byte[] version = new byte[8];
		System.arraycopy(major, 0, version, 0, 4);
		System.arraycopy(minor, 0, version, 4, 4);
		return version;
	}
}
