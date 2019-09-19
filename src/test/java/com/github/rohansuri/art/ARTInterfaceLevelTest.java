package com.github.rohansuri.art;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ARTInterfaceLevelTest {
	private static final String BAR = "BAR";
	private static final String BAZ = "BAZ";
	private static final String BOZ = "BOZ";
	private static final String BARCA = "BARCA";
	private static final String BARK = "BARK";

	@Test
	public void testSingleRemove() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertEquals("1", art.get(BAR));
		assertEquals("1", art.remove(BAR));
		assertNull(art.get(BAR));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildLeaf() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertNull(art.put(BAZ, "2"));
		assertNull(art.put(BOZ, "3"));
		assertEquals("1", art.get(BAR));
		assertEquals("2", art.get(BAZ));
		assertEquals("3", art.get(BOZ));

		// ceil tests
		assertEquals(BAR, art.ceilingKey(BAR));
		assertEquals(BAZ, art.ceilingKey(BAZ));
		assertEquals(BOZ, art.ceilingKey(BOZ));

		assertEquals(BAR, art.ceilingEntry(BAR).getKey());
		assertEquals(BAZ, art.ceilingEntry(BAZ).getKey());
		assertEquals(BOZ, art.ceilingEntry(BOZ).getKey());
		assertEquals("1", art.ceilingEntry(BAR).getValue());
		assertEquals("2", art.ceilingEntry(BAZ).getValue());
		assertEquals("3", art.ceilingEntry(BOZ).getValue());

		assertEquals(BAR, art.ceilingKey("BAQ"));
		assertEquals(BAZ, art.ceilingKey("BAS"));
		assertEquals(BOZ, art.ceilingKey("BBA"));
		assertNull(art.ceilingKey("BPA"));

		assertEquals(BAR, art.ceilingEntry("BAQ").getKey());
		assertEquals(BAZ, art.ceilingEntry("BAS").getKey());
		assertEquals(BOZ, art.ceilingEntry("BBA").getKey());
		assertNull(art.ceilingEntry("BPA"));
		assertEquals("1", art.ceilingEntry("BAQ").getValue());
		assertEquals("2", art.ceilingEntry("BAS").getValue());
		assertEquals("3", art.ceilingEntry("BBA").getValue());

		// floor tests

		assertEquals(BAR, art.floorKey(BAR));
		assertEquals(BAZ, art.floorKey(BAZ));
		assertEquals(BOZ, art.floorKey(BOZ));

		assertEquals(BAR, art.floorEntry(BAR).getKey());
		assertEquals(BAZ, art.floorEntry(BAZ).getKey());
		assertEquals(BOZ, art.floorEntry(BOZ).getKey());
		assertEquals("1", art.floorEntry(BAR).getValue());
		assertEquals("2", art.floorEntry(BAZ).getValue());
		assertEquals("3", art.floorEntry(BOZ).getValue());


		assertNull(art.floorKey("BAQ"));
		assertEquals(BAR, art.floorKey("BAS"));
		assertEquals(BAZ, art.floorKey("BBA"));
		assertEquals(BOZ, art.floorKey("BPA"));

		assertNull(art.floorEntry("BAQ"));
		assertEquals(BAR, art.floorEntry("BAS").getKey());
		assertEquals(BAZ, art.floorEntry("BBA").getKey());
		assertEquals(BOZ, art.floorEntry("BPA").getKey());
		assertEquals("1", art.floorEntry("BAS").getValue());
		assertEquals("2", art.floorEntry("BBA").getValue());
		assertEquals("3", art.floorEntry("BPA").getValue());

		// higher key test
		assertEquals(BAZ, art.higherKey(BAR));
		assertEquals(BOZ, art.higherKey(BAZ));
		assertNull(art.higherKey(BOZ));

		assertEquals(BAZ, art.higherEntry(BAR).getKey());
		assertEquals(BOZ, art.higherEntry(BAZ).getKey());
		assertEquals("2", art.higherEntry(BAR).getValue());
		assertEquals("3", art.higherEntry(BAZ).getValue());

		assertEquals(BAR, art.higherKey("BAQ"));
		assertEquals(BAZ, art.higherKey("BAY"));
		assertEquals(BOZ, art.higherKey("BOY"));

		assertEquals(BAR, art.higherEntry("BAQ").getKey());
		assertEquals(BAZ, art.higherEntry("BAY").getKey());
		assertEquals(BOZ, art.higherEntry("BOY").getKey());
		assertEquals("1", art.higherEntry("BAQ").getValue());
		assertEquals("2", art.higherEntry("BAY").getValue());
		assertEquals("3", art.higherEntry("BOY").getValue());

		// lower key test
		assertEquals(BAR, art.lowerKey(BAZ));
		assertEquals(BAZ, art.lowerKey(BOZ));

		assertEquals(BAR, art.lowerEntry(BAZ).getKey());
		assertEquals(BAZ, art.lowerEntry(BOZ).getKey());
		assertEquals("1", art.lowerEntry(BAZ).getValue());
		assertEquals("2", art.lowerEntry(BOZ).getValue());

		assertEquals(BAR, art.lowerKey("BAS"));
		assertEquals(BAZ, art.lowerKey("BBA"));
		assertEquals(BOZ, art.lowerKey("BPA"));

		assertEquals(BAR, art.lowerEntry("BAS").getKey());
		assertEquals(BAZ, art.lowerEntry("BBA").getKey());
		assertEquals(BOZ, art.lowerEntry("BPA").getKey());
		assertEquals("1", art.lowerEntry("BAS").getValue());
		assertEquals("2", art.lowerEntry("BBA").getValue());
		assertEquals("3", art.lowerEntry("BPA").getValue());

		// first key test

		Map.Entry<String, String> firstEntry = art.firstEntry();
		assertEquals(BAR, firstEntry.getKey());
		assertEquals(BAR, art.firstKey());
		assertEquals("1", firstEntry.getValue());

		// last key test

		Map.Entry<String, String> lastEntry = art.lastEntry();
		assertEquals(BOZ, lastEntry.getKey());
		assertEquals(BOZ, art.lastKey());
		assertEquals("3", lastEntry.getValue());

		// remove BAR that shares prefix A with BAZ
		assertEquals("1", art.remove(BAR));

		// path to BAZ should still exist
		assertEquals("2", art.get(BAZ));

		// untouched
		assertEquals("3", art.get(BOZ));

		// iterate and remove all
		assertEquals(2, art.size());
		Iterator<Map.Entry<String, String>> it = art.entrySet().iterator();
		try {
			it.remove();
			fail();
		}
		catch(IllegalStateException e){}
		it.next();
		it.remove();
		assertEquals(1, art.size());
		it.next();
		it.remove();
		try {
			it.remove();
			fail();
		}
		catch(IllegalStateException e){}

		assertEquals(0, art.size());

		try {
			it.next();
			fail();
		} catch (NoSuchElementException e){}
 	}

 	@Test
	public void testEntrySetRemoval(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BARCA, "1"));

		Set<Map.Entry<String, String>> set =  art.entrySet();
		assertTrue(set.contains(new AbstractMap.SimpleEntry<>(BARCA, "1")));
		assertFalse(set.contains(new AbstractMap.SimpleEntry<>(BARCA, "2")));
		assertFalse(set.remove(new AbstractMap.SimpleEntry<>(BARCA, "2")));
		assertTrue(set.remove(new AbstractMap.SimpleEntry<>(BARCA, "1")));
	}

	@Test
	public void testClear(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BARCA, "1"));
		assertEquals(1, art.size());
		art.clear();
		assertEquals(0, art.size());
		assertNull(art.get(BARCA));
	}

	@Test
	public void testPollFirstEntry(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertNull(art.put(BAZ, "2"));
		assertEquals(2, art.size());
		Map.Entry<String, String> entry = art.pollFirstEntry();
		assertEquals(entry.getKey(), BAR);
		assertEquals(entry.getValue(), "1");
		assertNull(art.get(BAR));
		assertEquals("2", art.get(BAZ));
		entry = art.pollFirstEntry();
		assertEquals(entry.getKey(), BAZ);
		assertEquals(entry.getValue(), "2");
		assertEquals(0, art.size());
	}

	@Test
	public void testPollLastEntry(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertNull(art.put(BAZ, "2"));
		assertEquals(2, art.size());
		Map.Entry<String, String> entry = art.pollLastEntry();
		assertEquals(entry.getKey(), BAZ);
		assertEquals(entry.getValue(), "2");
		assertNull(art.get(BAZ));
		assertEquals("1", art.get(BAR));
		entry = art.pollFirstEntry();
		assertEquals(entry.getKey(), BAR);
		assertEquals(entry.getValue(), "1");
		assertEquals(0, art.size());
	}

	@Test
	public void testSharedPrefixRemove_onlyChildInnerNode() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BARCA, "1"));
		assertNull(art.put(BAZ, "2"));
		assertNull(art.put(BOZ, "3"));
		assertNull(art.put(BARK, "4"));
		assertEquals("1", art.get(BARCA));
		assertEquals("2", art.get(BAZ));
		assertEquals("3", art.get(BOZ));
		assertEquals("4", art.get(BARK));

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
		assertEquals("2", art.remove(BAZ));

		/*
		after removing BAZ

				p = B
		take O	/      \  take A
			leaf BOZ   inner p = R
			    	   /     \
			    leaf BARK   leaf BARCA
		 */

		// path to BARCA and BARK should still exist
		assertEquals("4", art.get(BARK));
		assertEquals("1", art.get(BARCA));

		// untouched
		assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testSingleInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertEquals("1", art.get(BAR));
	}

	/*
		should cause initial lazy stored leaf to split and have "BA" path compressed
	 */
	@Test
	public void testSharedPrefixInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertNull(art.put(BAZ, "2"));
		assertEquals("1", art.get(BAR));
		assertEquals("2", art.get(BAZ));
	}

	@Test
	public void testBreakCompressedPath() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertNull(art.put(BAZ, "2"));
		assertNull(art.put(BOZ, "3")); // breaks compressed path of BAR, BAZ
		assertEquals("1", art.get(BAR));
		assertEquals("2", art.get(BAZ));
		assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testReplace() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertEquals("1", art.get(BAR));

		assertEquals("1", art.put(BAR, "2"));
		assertEquals("2", art.get(BAR));
	}

	// we'll have to provide nice Serdes that can take care of strings this way
	@Test
	public void testPrefixesInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		assertNull(art.put(BAR, "1"));
		assertEquals("1", art.get(BAR));

		assertNull(art.put(BARCA, "2"));
		assertEquals("2", art.get(BARCA));
	}
}
