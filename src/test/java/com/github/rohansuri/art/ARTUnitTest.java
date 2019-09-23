package com.github.rohansuri.art;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/*
	tests for helper methods defined in ART
 */
public class ARTUnitTest {

	@Test
	public void testCompare() {
		BinaryComparable<String> bc = BinaryComparables.forUTF8();
		byte[] a = bc.get("pqrxyabce");
		byte[] b = bc.get("zabcd");
		// abc, abc (i == aTo && j == bTo)
		Assertions.assertEquals(0, AdaptiveRadixTree.compare(a, 5, 8, b, 1, 4));
		// abc, abcd (i == aTo)
		Assertions.assertEquals(-1, AdaptiveRadixTree.compare(a, 5, 8, b, 1, 5));
		// abce, abc (j == bTo)
		Assertions.assertEquals(1, AdaptiveRadixTree.compare(a, 5, 9, b, 1, 4));
		// abce, abcd (a[i] < b[j])
		Assertions.assertEquals(1, AdaptiveRadixTree.compare(a, 5, 9, b, 1, 5));
	}
}
