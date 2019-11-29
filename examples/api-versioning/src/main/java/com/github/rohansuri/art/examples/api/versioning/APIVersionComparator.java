package com.github.rohansuri.art.examples.api.versioning;

import java.util.Comparator;

public enum APIVersionComparator implements Comparator<APIVersion> {
	INSTANCE;
	
	@Override
	public int compare(APIVersion o1, APIVersion o2) {
		int res = Integer.compare(o1.major, o2.major);
		if (res != 0) {
			return res;
		}
		return Integer.compare(o1.minor, o2.minor);
	}
}
