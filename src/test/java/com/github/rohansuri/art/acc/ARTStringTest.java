package com.github.rohansuri.art.acc;

import java.util.NavigableMap;

import com.github.rohansuri.art.AbstractNavigableMapTest;
import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
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

	@Override
	public String[] getSampleKeys() {
		// changing sample keys to introduce baaar, baaaz, baoz
		// which cause branchOut
		// but better to write out a separate test that brings this out behaviour
		Object[] result = new String[] {"gosh", "foo", "baaar", "baaaz", "tmp", "baoz", "golly", "gee", "hello", "goodbye", "we'll", "see", "you", "all", "again", "key", "key2", "nonnullkey"};
		return (String[]) result;
	}

}
