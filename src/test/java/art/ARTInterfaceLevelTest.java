package art;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ARTInterfaceLevelTest {
	private static final String BAR = "BAR";
	private static final String BAZ = "BAZ";
	private static final String BOZ = "BOZ";
	private static final String BARCA = "BARCA";
	private static final String BARK = "BARK";

	@Test
	public void testSingleRemove() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("1", art.remove(BAR));
		Assert.assertNull(art.get(BAR));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildLeaf() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));

		Map.Entry<String, String> firstEntry = art.firstEntry();
		Assert.assertEquals(BAR, firstEntry.getKey());
		Assert.assertEquals(BAR, art.firstKey());
		Assert.assertEquals("1", firstEntry.getValue());

		Map.Entry<String, String> lastEntry = art.lastEntry();
		Assert.assertEquals(BOZ, lastEntry.getKey());
		Assert.assertEquals(BOZ, art.lastKey());
		Assert.assertEquals("3", lastEntry.getValue());

		// remove BAR that shares prefix A with BAZ
		Assert.assertEquals("1", art.remove(BAR));

		// path to BAZ should still exist
		Assert.assertEquals("2", art.get(BAZ));

		// untouched
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildInnerNode() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BARCA, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3"));
		Assert.assertNull(art.put(BARK, "4"));
		Assert.assertEquals("1", art.get(BARCA));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));
		Assert.assertEquals("4", art.get(BARK));

		/*
				p = B
		take O	/      \  take A
			leaf BOZ    inner
			    		/     \ p = R
			    	leaf BAZ   inner
			    	          /     \
				         leaf BARK   leaf BARCA
		 */


		// remove BAZ that shares prefix BA with node parent of BARCA, BARK
		Assert.assertEquals("2", art.remove(BAZ));

		/*
		after removing BAZ

				p = B
		take O	/      \  take A
			leaf BOZ   inner p = R
			    	   /     \
			    leaf BARK   leaf BARCA
		 */

		// path to BARCA and BARK should still exist
		Assert.assertEquals("4", art.get(BARK));
		Assert.assertEquals("1", art.get(BARCA));

		// untouched
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testSingleInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
	}

	/*
		should cause initial lazy stored leaf to split and have "BA" path compressed
	 */
	@Test
	public void testSharedPrefixInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
	}

	@Test
	public void testBreakCompressedPath() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3")); // breaks compressed path of BAR, BAZ
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testReplace() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));

		Assert.assertEquals("1", art.put(BAR, "2"));
		Assert.assertEquals("2", art.get(BAR));
	}

	// we'll have to provide nice Serdes that can take care of strings this way
	@Test
	public void testPrefixesInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparable.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));

		Assert.assertNull(art.put(BARCA, "2"));
		Assert.assertEquals("2", art.get(BARCA));
	}


	/*
		should result in a tree with
		256 size root having 256 child pointers
		representing the higher 8bits in the 16bit integer.
		and each of those 256 child pointers in turn pointing to a 256 node type,
		representing all possible combinations of lower 8 bits of the 16bit integer.
				  256
			   /  |    |    \ ... 256 such child paths
			256	  256  256  256  .. lower 8 bit combinations
	 */
	@Test
	public void testInsertingAllInt16BitIntegers() {
		AdaptiveRadixTree<Short, String> art = new AdaptiveRadixTree<>(BinaryComparable.SHORT);

		short i = Short.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertNull(art.put(i, value));
			Assert.assertEquals(value, art.get(i));
			i++;
		}
		while (i != Short.MIN_VALUE);

		i = Short.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.get(i));
			i++;
		}
		while (i != Short.MIN_VALUE);

	}

	@Test
	public void testInsertingAndDeletingAllInt8BitIntegers() throws ReflectiveOperationException {
		AdaptiveRadixTree<Byte, String> art = new AdaptiveRadixTree<>(BinaryComparable.BYTE);

		// insert all
		byte i = Byte.MIN_VALUE;
		int expectedSize = 0;
		do {
			String value = String.valueOf(i);
			Assert.assertNull(art.put(i, value));
			expectedSize++;
			Assert.assertEquals(value, art.get(i));
			Assert.assertEquals(expectedSize, art.size());
			i++;
		}
		while (i != Byte.MIN_VALUE);

		Map.Entry<Byte, String> firstEntry = art.firstEntry();
		Assert.assertEquals(String.valueOf(Byte.MIN_VALUE), firstEntry.getValue());
		Assert.assertEquals((Byte) Byte.MIN_VALUE, firstEntry.getKey());
		Assert.assertEquals((Byte) Byte.MIN_VALUE, art.firstKey());

		Map.Entry<Byte, String> lastEntry = art.lastEntry();
		Assert.assertEquals(String.valueOf(Byte.MAX_VALUE), lastEntry.getValue());
		Assert.assertEquals((Byte) Byte.MAX_VALUE, lastEntry.getKey());
		Assert.assertEquals((Byte) Byte.MAX_VALUE, art.lastKey());

		// assert parent of root is null
		Field root = art.getClass().getDeclaredField("root");
		root.setAccessible(true);
		Assert.assertNull(((AbstractNode) root.get(art)).parent);

		// remove one by one and check if others exist
		i = Byte.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.remove(i));
			expectedSize--;
			Assert.assertNull(art.get(i));
			Assert.assertEquals(expectedSize, art.size());

			// others should exist
			for (byte j = ++i; j != Byte.MIN_VALUE; j++) {
				value = String.valueOf(j);
				Assert.assertEquals(value, art.get(j));
			}
		}
		while (i != Byte.MIN_VALUE);
	}

	@Test
	@Ignore // takes too long (1m 20secs locally)
	public void testInsertingAndDeletingAllInt16BitIntegers() {
		AdaptiveRadixTree<Short, String> art = new AdaptiveRadixTree<>(BinaryComparable.SHORT);

		// insert all
		short i = Short.MIN_VALUE;

		do {
			String value = String.valueOf(i);
			Assert.assertNull(art.put(i, value));
			Assert.assertEquals(value, art.get(i));
			i++;
		}
		while (i != Short.MIN_VALUE);

		// remove one by one and check if others exist
		i = Short.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.remove(i));
			Assert.assertNull(art.get(i));

			// others should exist
			for (short j = ++i; j != Short.MIN_VALUE; j++) {
				value = String.valueOf(j);
				Assert.assertEquals(value, art.get(j));
			}

		}
		while (i != Short.MIN_VALUE);
	}

	@Test
	@Ignore // heavy test (in terms of?). Disable logging
	public void testInsertingAllInt32BitIntegers() {
		AdaptiveRadixTree<Integer, String> art = new AdaptiveRadixTree<>(BinaryComparable.INTEGER);

		int i = Integer.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertNull(art.put(i, value));
			Assert.assertEquals(value, art.get(i));
			i++;
		}
		while (i != Integer.MIN_VALUE);

		i = Integer.MIN_VALUE;
		do {
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.get(i));
			i++;
		}
		while (i != Integer.MIN_VALUE);
	}

}
