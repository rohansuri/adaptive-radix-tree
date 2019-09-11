package art;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ARTIntegerTests {
	@Test
	public void testDeletingAll8BitIntegersUsingValueCollection() {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparables.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertNull(art.put(i, value));
			i++;
		}
		while (i != Byte.MIN_VALUE);

		// check all using values collection
		Collection<String> values = art.values();
		i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertTrue(values.contains(value));
			i++;
		}
		while (i != Byte.MIN_VALUE);

		// remove one by one
		i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			values.remove(value);
			assertFalse(values.contains(value));
			i++;
		}
		while (i != Byte.MIN_VALUE);
		assertEquals(0, values.size());
	}

	@Test
	public void testDescendingIterator() {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparables.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertNull(art.put(i, value));
			i++;
		}
		while (i != Byte.MIN_VALUE);

		i = Byte.MAX_VALUE;
		Iterator<Byte> it = art.descendingKeyIterator();
		while (it.hasNext()) {
			assertEquals(i, (byte) it.next());
			i--;
		}
	}

	// we can remove this test since entrySet, valueSet, keySet iterators all use the same super remove implementation
	// in PrivateEntryIterator
	@Test
	public void testDeletingAll8BitIntegersUsingValueIterator() {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparables.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertNull(art.put(i, value));
			i++;
		}
		while (i != Byte.MIN_VALUE);

		// remove one by one and check if next exists
		i = Byte.MIN_VALUE;
		Iterator<String> it = art.values().iterator();
		while (it.hasNext()) {
			String value = it.next();
			assertEquals(String.valueOf(i), value);
			it.remove();
			i++;
			if (i == Byte.MIN_VALUE) {
				break;
			}
			value = String.valueOf(i);
			assertEquals(value, art.get(i));
		}
	}

	@Test
	public void testDeletingAll8BitIntegersUsingEntrySetIterator() {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparables.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			assertNull(art.put(i, value));
			i++;
		}
		while (i != Byte.MIN_VALUE);

		// remove one by one and check if next exists
		i = Byte.MIN_VALUE;
		Iterator<Map.Entry<Byte, String>> it = art.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Byte, String> entry = it.next();
			assertEquals(i, (byte) entry.getKey());
			it.remove();
			i++;
			if (i == Byte.MIN_VALUE) {
				break;
			}
			String value = String.valueOf(i);
			assertEquals(value, art.get(i));
		}
	}

	@Test
	public void testInsertingAndDeletingAllInt8BitIntegers() throws ReflectiveOperationException {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparables.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		int expectedSize = 0;
		do {
			// floor test
			if (i != Byte.MIN_VALUE) {
				assertEquals(i - 1, (byte) art.floorKey(i));
			}
			else {
				assertNull(art.floorKey(i));
			}

			String value = String.valueOf(i);
			assertFalse(art.containsKey(i));
			assertFalse(art.containsValue(value));
			assertNull(art.put(i, value));
			expectedSize++;
			assertEquals(value, art.get(i));
			assertEquals(expectedSize, art.size());
			assertTrue(art.containsKey(i));
			assertTrue(art.containsValue(value));

			// lowerKey test
			if (i != Byte.MIN_VALUE) {
				assertEquals(i - 1, (byte) art.lowerKey(i));
			}
			else {
				assertNull(art.lowerKey(i));
			}
			i++;
		}
		while (i != Byte.MIN_VALUE);

		Map.Entry<Byte, String> firstEntry = art.firstEntry();
		assertEquals(String.valueOf(Byte.MIN_VALUE), firstEntry.getValue());
		assertEquals((Byte) Byte.MIN_VALUE, firstEntry.getKey());
		assertEquals((Byte) Byte.MIN_VALUE, art.firstKey());

		Map.Entry<Byte, String> lastEntry = art.lastEntry();
		assertEquals(String.valueOf(Byte.MAX_VALUE), lastEntry.getValue());
		assertEquals((Byte) Byte.MAX_VALUE, lastEntry.getKey());
		assertEquals((Byte) Byte.MAX_VALUE, art.lastKey());

		// assert parent of root is null
		Field root = art.getClass().getDeclaredField("root");
		root.setAccessible(true);
		assertNull(((AbstractNode) root.get(art)).parent());


		// test sorted order iteration
		i = Byte.MIN_VALUE;
		for (Map.Entry<Byte, String> entry : art.entrySet()) {
			assertEquals(i, (byte) entry.getKey());
			i++;
		}


		// remove one by one and check if others exist
		i = Byte.MIN_VALUE;
		do {

			// higherKey test
			if (i != Byte.MAX_VALUE) {
				try {
					assertEquals(i + 1, (byte) art.higherKey(i));
				}
				catch (NullPointerException e) {
					System.out.println(i);
					fail();
				}
			}
			else {
				assertNull(art.higherKey(i));
			}

			String value = String.valueOf(i);
			assertEquals(value, art.remove(i));
			expectedSize--;
			assertNull(art.get(i));
			assertEquals(expectedSize, art.size());

			// ceil test
			if (i != Byte.MAX_VALUE) {
				assertEquals(i + 1, (byte) art.ceilingKey(i));
			}
			else {
				assertNull(art.ceilingKey(i));
			}

			// others should exist
			for (byte j = ++i; j != Byte.MIN_VALUE; j++) {
				value = String.valueOf(j);
				assertEquals(value, art.get(j));
			}

		}
		while (i != Byte.MIN_VALUE);
	}

	@Test
	// test that just stretches ART to
	//	two levels and causes growth, shrink, etc.
	// 	shorts are two levels (2 bytes long)
	// 	first inserting all single byte shorts (values less than 256) would have the root
	//  with prefix path length of 1 and then inserting others would cause it to grow
	//	from Node4 -> ... -> Node256
	public void testInsertingAndDeletingAllInt16BitIntegers() {
		AdaptiveRadixTree<Short, String> art = new AdaptiveRadixTree<>(BinaryComparables.SHORT);

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

	/*
			int32 (4 levels)
			p&c of byte variants to expand to all levels:
			0000 0000
			0000 0001

	 */
	@Test
	public void testInsertingAndDeleting32BitIntegers() {
		AdaptiveRadixTree<Integer, Integer> art = new AdaptiveRadixTree<>(BinaryComparables.INTEGER);
		int level = 4;
		List<Integer> l = new ArrayList<>(1 << level);
		pc(l, ByteBuffer.allocate(level), 0, level);
		int expectedSize = 0;
		for (int key : l) {
			assertNull(art.put(key, key));
			expectedSize++;
			assertEquals(expectedSize, art.size());
		}

		// get all
		for(int key: l){
			assertEquals(key, (int)art.get(key));
		}

		for (int i = 0; i < l.size(); i++) {
			int expected = l.get(i);
			assertEquals(expected, (int) art.remove(expected));
			expectedSize--;
			assertEquals(expectedSize, art.size());
		}
	}

	private void pc(List<Integer> l, ByteBuffer num, int currLevel, int maxLevel) {
		if (currLevel == maxLevel) {
			int n = num.getInt(0);
			l.add(n);
			return;
		}

		for (int i = 0; i < 2; i++) {
			// two choices on every level
			num.put((byte) i);
			pc(l, num, currLevel + 1, maxLevel);
			num.position(currLevel);
		}
	}

}
