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
	public void testLongestCommonPrefix() {
		// common prefix len more than pessimistic storage
		String leafKey = "xxabcdefghijk";
		String key = "xxabcdefghijp";
		LeafNode<String, String> leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		Node4 pathCompressed = new Node4();
		Assertions.assertEquals(10, AdaptiveRadixTree
				.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(10, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abcdefgh".getBytes(), pathCompressed.getValidPrefixKey());

		// early break
		leafKey = "xxabcd";
		key = "xxabcz";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree
				.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// leaf ends first
		leafKey = "xxabc";
		key = "xxabcdef";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree
				.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// new key ends first
		leafKey = "xxabcdef";
		key = "xxabc";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(3, AdaptiveRadixTree
				.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(3, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), pathCompressed.getValidPrefixKey());

		// no match
		leafKey = "xxz";
		key = "xxa";
		leaf = new LeafNode<>(BinaryComparables.forUTF8().get(leafKey), leafKey, "value");
		pathCompressed = new Node4();
		Assertions.assertEquals(0, AdaptiveRadixTree
				.setLongestCommonPrefix(leaf, BinaryComparables.forUTF8().get(key), pathCompressed, 2));
		Assertions.assertEquals(0, pathCompressed.prefixLen);
		Assertions.assertArrayEquals("".getBytes(), pathCompressed.getValidPrefixKey());
	}

	/*
		cover all windows (toCompress, linking key, onlyChild)
		everything from toCompress
		everything from toCompress + linking key
		everything from toCompress + linking key + some from child
		everything from toCompress + linking key + all from child
	 */
	@Test
	public void testUpdateCompressedPathOfOnlyChild() {
		// everything from toCompress
		Node4 node = new Node4();
		node.prefixLen = 10;
		String toCompressPrefix = "abcdefgh";
		System.arraycopy(toCompressPrefix.getBytes(), 0, node.prefixKeys, 0, toCompressPrefix.length());
		InnerNode onlyChild = new Node4();
		byte linkingKey = 1;
		node.addChild(linkingKey, onlyChild);
		onlyChild.prefixLen = 3;
		String onlyChildPrefix = "pqr";
		System.arraycopy(onlyChildPrefix.getBytes(), 0, onlyChild.prefixKeys, 0, onlyChildPrefix.length());
		AdaptiveRadixTree.updateCompressedPathOfOnlyChild(node);
		Assertions.assertEquals(14, onlyChild.prefixLen);
		Assertions.assertArrayEquals(node.getValidPrefixKey(), onlyChild.getValidPrefixKey());

		// everything from toCompress + linking key
		node = new Node4();
		node.prefixLen = 7;
		toCompressPrefix = "abcdefg";
		System.arraycopy(toCompressPrefix.getBytes(), 0, node.prefixKeys, 0, toCompressPrefix.length());
		onlyChild = new Node4();
		node.addChild(linkingKey, onlyChild);
		onlyChild.prefixLen = 3;
		onlyChildPrefix = "pqr";
		System.arraycopy(onlyChildPrefix.getBytes(), 0, onlyChild.prefixKeys, 0, onlyChildPrefix.length());
		AdaptiveRadixTree.updateCompressedPathOfOnlyChild(node);
		Assertions.assertEquals(11, onlyChild.prefixLen);
		byte[] expected = new byte[InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT];
		for (int i = 0; i < node.prefixLen; i++) {
			expected[i] = node.prefixKeys[i];
		}
		expected[node.prefixLen] = linkingKey;
		Assertions.assertArrayEquals(expected, onlyChild.getValidPrefixKey());

		// everything from toCompress + linking key + some from child
		node = new Node4();
		node.prefixLen = 4;
		toCompressPrefix = "abcd";
		System.arraycopy(toCompressPrefix.getBytes(), 0, node.prefixKeys, 0, toCompressPrefix.length());
		onlyChild = new Node4();
		node.addChild(linkingKey, onlyChild);
		onlyChild.prefixLen = 5;
		onlyChildPrefix = "pqrst";
		System.arraycopy(onlyChildPrefix.getBytes(), 0, onlyChild.prefixKeys, 0, onlyChildPrefix.length());
		AdaptiveRadixTree.updateCompressedPathOfOnlyChild(node);
		Assertions.assertEquals(10, onlyChild.prefixLen);
		expected = new byte[InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT];
		for (int i = 0; i < node.prefixLen; i++) {
			expected[i] = node.prefixKeys[i];
		}
		expected[node.prefixLen] = linkingKey;
		for (int i = node.prefixLen + 1, j = 0; i < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && j < onlyChildPrefix
				.length(); i++, j++) {
			expected[i] = onlyChildPrefix.getBytes()[j];
		}
		Assertions.assertArrayEquals(expected, onlyChild.getValidPrefixKey());

		// everything from toCompress + linking key + all from child
		node = new Node4();
		node.prefixLen = 4;
		toCompressPrefix = "abcd";
		System.arraycopy(toCompressPrefix.getBytes(), 0, node.prefixKeys, 0, toCompressPrefix.length());
		onlyChild = new Node4();
		node.addChild(linkingKey, onlyChild);
		onlyChild.prefixLen = 2;
		onlyChildPrefix = "pq";
		System.arraycopy(onlyChildPrefix.getBytes(), 0, onlyChild.prefixKeys, 0, onlyChildPrefix.length());
		AdaptiveRadixTree.updateCompressedPathOfOnlyChild(node);
		Assertions.assertEquals(7, onlyChild.prefixLen);
		expected = new byte[InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT - 1];
		for (int i = 0; i < node.prefixLen; i++) {
			expected[i] = node.prefixKeys[i];
		}
		expected[node.prefixLen] = linkingKey;
		for (int i = node.prefixLen + 1, j = 0; i < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && j < onlyChildPrefix
				.length(); i++, j++) {
			expected[i] = onlyChildPrefix.getBytes()[j];
		}
		Assertions.assertArrayEquals(expected, onlyChild.getValidPrefixKey());
	}

	@Test
	public void testRemoveLCPFromCompressedPath() {
		InnerNode node = new Node4();
		String compressedPath = "abcd";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		// LCP = 3, hence "d" would be the differing partial key, therefore new compressed path
		// would be "", hence 0 length
		AdaptiveRadixTree.removeLCPFromCompressedPath(node, 3);
		Assertions.assertEquals(0, node.prefixLen);

		// LCP = 2, hence "c" would be differing partial key
		// and new compressed path would be "d"
		node.prefixLen = compressedPath.length();
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		AdaptiveRadixTree.removeLCPFromCompressedPath(node, 2);
		Assertions.assertEquals(1, node.prefixLen);
		Assertions.assertArrayEquals("d".getBytes(), node.getValidPrefixKey());

		// LCP = 4, does not obey constraint of method
		node.prefixLen = compressedPath.length();
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		Assertions.assertThrows(AssertionError.class, () -> AdaptiveRadixTree
				.removeLCPFromCompressedPath(node, compressedPath.length()));

	}
}
