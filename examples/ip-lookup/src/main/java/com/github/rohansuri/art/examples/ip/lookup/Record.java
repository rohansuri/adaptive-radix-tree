package com.github.rohansuri.art.examples.ip.lookup;

import java.net.InetAddress;

public class Record {
	private final InetAddress start;
	private final InetAddress end;
	private final String country;

	Record(InetAddress start, InetAddress end, String country) {
		this.start = start;
		this.end = end;
		this.country = country;
	}

	String getCountry() {
		return country;
	}

	InetAddress getEnd() {
		return end;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Record o = (Record) obj;
		return start.equals(o.start) && end.equals(o.end) && country.equals(o.country);
	}
}
