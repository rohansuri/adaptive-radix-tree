package com.github.rohansuri.art.acc;

import java.util.Arrays;
import java.util.NavigableMap;

import com.github.rohansuri.art.AbstractNavigableMapTest;
import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import com.github.rohansuri.art.NavigableKeySetStringTest;
import junit.framework.Test;
import org.apache.commons.collections4.BulkTest;

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

	/*
	 	CLEANUP:
	 	changing sample keys to introduce baaar, baaaz, baoz
		which cause branchOut (since lcp is not totally equal)

	 	changing sample keys to introduce fooooooooz, fooooooood, fooooooooe
	 	which cause optimistic path compression jump

	 	but better to write out a separate test that brings this out behaviour
	 */
	@Override
	public String[] getSampleKeys() {
		Object[] result = new String[] {"fooooooooz", "fooooooood", "fooooooooe", "baaar", "baaaz", "tmp", "baoz", "gee", "hello", "goodbye", "we'll", "see", "you", "all", "again", "key", "key2", "nonnullkey"};
		return (String[]) result;
	}

	// since default sample keys in AbstractNavigableSet are integers
	@Override
	public BulkTest bulkTestNavigableKeySet() {
		return new NavigableKeySetStringTest(this);
	}
}
