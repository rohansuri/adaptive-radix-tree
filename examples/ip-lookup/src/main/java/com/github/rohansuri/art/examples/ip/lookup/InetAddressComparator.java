package com.github.rohansuri.art.examples.ip.lookup;

import java.net.InetAddress;
import java.util.Comparator;

public enum InetAddressComparator implements Comparator<InetAddress> {
	INSTANCE;

	@Override
	public int compare(InetAddress o1, InetAddress o2) {
		byte[] b1 = o1.getAddress();
		byte[] b2 = o2.getAddress();
		for (int i = 0; i < 4; i++) {
			int res = Byte.compareUnsigned(b1[i], b2[i]);
			if (res != 0) {
				return res;
			}
		}
		return 0;
	}
}
