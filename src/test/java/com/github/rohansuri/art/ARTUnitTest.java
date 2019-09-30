package com.github.rohansuri.art;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

	/*
		cp for all i == key for all i
			but len(key) >= len(cp) expect 0
			but len(key) < len(cp) expect 1
		cp at i < key at i expect -1
		cp at i > key at i expect 1
	 */
	@Test
	public void testCompareCompressedPath() {
		InnerNode node = new Node4();
		BinaryComparable<String> bc = BinaryComparables.forUTF8();

		// 0 (even when key length more than compressed path)
		String compressedPath = "abcd";
		String key = "xx" + compressedPath + "ef";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		Assertions.assertEquals(0, AdaptiveRadixTree.compareCompressedPath(node, bc.get(key), 2));

		// 0 (totally equal and length same)
		key = compressedPath;
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		Assertions.assertEquals(0, AdaptiveRadixTree.compareCompressedPath(node, bc.get(key), 0));


		// 1 (compressed path length is more than key)
		key = "cab";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		Assertions.assertEquals(1, AdaptiveRadixTree.compareCompressedPath(node, bc.get(key), 1));

		// 1 (inequality and compressed path being greater)
		compressedPath = "xxz";
		key = "xxa";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		Assertions.assertEquals(1, AdaptiveRadixTree.compareCompressedPath(node, bc.get(key), 0));

		// -1 (only in case of inequality of partial key byte)
		compressedPath = "xxaa";
		key = "xxabcd";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		Assertions.assertEquals(-1, AdaptiveRadixTree.compareCompressedPath(node, bc.get(key), 0));

	}


	@Test
	public void testSetLongestCommonPrefix() {
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

		// coverage for assert onlyChild != null;
		Assertions.assertThrows(AssertionError.class, () -> AdaptiveRadixTree
				.updateCompressedPathOfOnlyChild(new Node4()));
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

	@Test
	public void testBranchOut() {
		InnerNode node = new Node4();
		BinaryComparable<String> bc = BinaryComparables.forUTF8();
		String compressedPath = "abcxyz";
		System.arraycopy(compressedPath.getBytes(), 0, node.prefixKeys, 0, compressedPath.length());
		node.prefixLen = compressedPath.length();
		String key = "xxabcdef";
		String value = "value";
		// lcp == "abc"
		Node newNode = AdaptiveRadixTree.branchOut(node, bc.get(key), key, value, 3, 5);
		Assertions.assertEquals(2, newNode.size());
		Assertions.assertEquals(node, newNode.findChild((byte) 'x'));
		Node leaf = newNode.findChild((byte) 'd');
		Assertions.assertTrue(leaf instanceof LeafNode);
		Assertions.assertEquals(key, ((LeafNode) leaf).getKey());
		Assertions.assertEquals(value, ((LeafNode) leaf).getValue());
		Assertions.assertEquals(3, ((InnerNode) newNode).prefixLen);
		Assertions.assertArrayEquals("abc".getBytes(), ((InnerNode) newNode).getValidPrefixKey());

		// test removeLCPFromCompressedPath
		Assertions.assertEquals(2, node.prefixLen);
		Assertions.assertArrayEquals("yz".getBytes(), node.getValidPrefixKey());

		// obey constraints
		node.prefixLen = 1;
		Assertions.assertThrows(AssertionError.class, () -> AdaptiveRadixTree
				.branchOut(node, bc.get(key), key, value, 3, 5));

		node.prefixLen = 10;
		Assertions.assertThrows(AssertionError.class, () -> AdaptiveRadixTree
				.branchOut(node, bc.get(key), key, value, InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, 5));

	}

	@Test
	public void testReplace() {
		BinaryComparable<String> bc = BinaryComparables.forUTF8();
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(bc);
		String key = "foo";
		String value = "value";
		// adding the very first key would result in replacing root
		LeafNode<String, String> leafNode = new LeafNode<>(bc.get(key), key, value);
		art.replace(0, bc.get("foo"), null, leafNode);
		Assertions.assertEquals(value, art.get(key));

		art = new AdaptiveRadixTree<>(bc);
		// setup root with one child
		Node4 root = new Node4();
		art.replace(0, new byte[] {}, null, leafNode);
		Node child = Mockito.spy(AbstractNode.class);
		root.addChild((byte) 'x', child);

		// replace root's x downlink with new child (for various reasons, for example because we just grew this child)
		Node newChild = Mockito.spy(AbstractNode.class);
		art.replace(1, bc.get("x"), root, newChild);

		Assertions.assertEquals(1, root.size());
		Assertions.assertSame(newChild, root.findChild((byte) 'x'));
	}

}
