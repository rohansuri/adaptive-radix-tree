package com.github.rohansuri.art;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ARTShortTest {


	@Test
	// test that just stretches ART to
	//	two levels and causes growth, shrink, etc.
	// 	shorts are two levels (2 bytes long)
	// 	first inserting all single byte shorts (values less than 256) would have the root
	//  with prefix path length of 1 and then inserting others would cause it to grow
	//	from Node4 -> ... -> Node256
	// TODO: replace with AbstractNavigableMapTest
	public void testInsertingAndDeletingAllInt16BitIntegers() {
		AdaptiveRadixTree<Short, String> art = new AdaptiveRadixTree<>(BinaryComparables.forShort());

		/*
			insert all

			should result in a tree with
			256 size root having 256 child pointers
			representing the higher 8bits in the 16bit integer.
			and each of those 256 child pointers in turn pointing to a 256 node type,
			representing all possible combinations of lower 8 bits of the 16bit integer.
					  256
				   /  |    |    \ ... 256 such child paths
				256	  256  256  256  .. lower 8 bit combinations
	 	*/
		short i = Short.MIN_VALUE;
		int expectedSize = 0;
		do {
			String value = String.valueOf(i);
			assertNull(art.put(i, value));
			assertEquals(value, art.get(i));
			expectedSize++;
			assertEquals(expectedSize, art.size());
			i++;
		}
		while (i != Short.MIN_VALUE);

		// remove one by one and check if others exist
		i = Short.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertEquals(value, art.remove(i));
			assertNull(art.get(i));
			expectedSize--;
			assertEquals(expectedSize, art.size());
			i++;
		}
		while (i != Short.MIN_VALUE);
	}
}
