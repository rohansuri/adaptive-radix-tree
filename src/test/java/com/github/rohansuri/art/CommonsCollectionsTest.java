package com.github.rohansuri.art;

import java.util.SortedMap;

import org.apache.commons.collections4.map.AbstractSortedMapTest;

public class CommonsCollectionsTest extends AbstractSortedMapTest {

	public CommonsCollectionsTest() {
		super("CommonsCollectionsTestSuite");
	}

	@Override
	public SortedMap makeObject() {
		return new AdaptiveRadixTree<>(BinaryComparables.forUTF8());
	}
}
