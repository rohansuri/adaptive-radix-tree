package com.github.rohansuri.art.examples.ip.lookup;

import java.net.InetAddress;

import com.github.rohansuri.art.BinaryComparable;

public enum InetAddressBinaryComparable implements BinaryComparable<InetAddress> {
	INSTANCE;
	@Override
	public byte[] get(InetAddress key) {
		return key.getAddress();
	}
}
