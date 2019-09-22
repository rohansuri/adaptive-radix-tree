package com.github.rohansuri.art.acc;

import java.util.SortedMap;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.collections4.map.AbstractSortedMapTest;

public class ARTStringTest extends AbstractSortedMapTest<String, String> {

	public ARTStringTest() {
		super("ARTStringTest");
	}

	@Override
	public SortedMap makeObject() {
		return new AdaptiveRadixTree<>(BinaryComparables.forUTF8());
	}
}
