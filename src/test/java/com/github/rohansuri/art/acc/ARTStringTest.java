package com.github.rohansuri.art.acc;

import java.util.NavigableMap;
import java.util.SortedMap;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import junit.framework.Test;
import org.apache.commons.collections4.BulkTest;
import org.apache.commons.collections4.map.AbstractSortedMapTest;

public class ARTStringTest extends AbstractNavigableMapTest<String, String> {

	public ARTStringTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return BulkTest.makeSuite(ARTStringTest.class);
	}

	@Override
	public NavigableMap<String, String> makeObject() {
		return new AdaptiveRadixTree<>(BinaryComparables.forUTF8());
	}

}
