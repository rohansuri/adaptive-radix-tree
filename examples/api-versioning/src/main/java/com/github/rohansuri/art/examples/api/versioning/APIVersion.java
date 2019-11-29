package com.github.rohansuri.art.examples.api.versioning;

public class APIVersion {
	final int major, minor;

	APIVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		APIVersion o = (APIVersion) obj;
		return major == o.major && minor == o.minor;
	}
}
