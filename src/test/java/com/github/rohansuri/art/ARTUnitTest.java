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

	@Test
	public void testLongestCommonPrefix(){
		// common prefix len more than pessimistic storage
		String leafKey = "xxabcdefghijk";
		String key = "xxabcdefghijp";
		LeafNode<String, String> leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		Node4 pathCompressed = new Node4();
		Assertions.assertEquals(10, AdaptiveRadixTree.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(10, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abcdefgh".getBytes(), pathCompressed.getValidPrefixKey());

		// early break
		leafKey = "xxabcd";
		key = "xxabcz";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// leaf ends first
		leafKey = "xxabc";
		key = "xxabcdef";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// new key ends first
		leafKey = "xxabcdef";
		key = "xxabc";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// no match
		leafKey = "xxz";
		key = "xxa";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(0, AdaptiveRadixTree.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(0, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("".getBytes(), pathCompressed.getValidPrefixKey());
	}
}
