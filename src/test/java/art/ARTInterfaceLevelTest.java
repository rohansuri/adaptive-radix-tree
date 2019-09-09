package art;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ARTInterfaceLevelTest {
	private static final String BAR = "BAR";
	private static final String BAZ = "BAZ";
	private static final String BOZ = "BOZ";
	private static final String BARCA = "BARCA";
	private static final String BARK = "BARK";

	@Test
	public void testSingleRemove() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("1", art.remove(BAR));
		Assert.assertNull(art.get(BAR));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildLeaf() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));

		// ceil tests
		Assert.assertEquals(BAR, art.ceilingKey(BAR));
		Assert.assertEquals(BAZ, art.ceilingKey(BAZ));
		Assert.assertEquals(BOZ, art.ceilingKey(BOZ));

		Assert.assertEquals(BAR, art.ceilingEntry(BAR).getKey());
		Assert.assertEquals(BAZ, art.ceilingEntry(BAZ).getKey());
		Assert.assertEquals(BOZ, art.ceilingEntry(BOZ).getKey());
		Assert.assertEquals("1", art.ceilingEntry(BAR).getValue());
		Assert.assertEquals("2", art.ceilingEntry(BAZ).getValue());
		Assert.assertEquals("3", art.ceilingEntry(BOZ).getValue());

		Assert.assertEquals(BAR, art.ceilingKey("BAQ"));
		Assert.assertEquals(BAZ, art.ceilingKey("BAS"));
		Assert.assertEquals(BOZ, art.ceilingKey("BBA"));
		Assert.assertNull(art.ceilingKey("BPA"));

		Assert.assertEquals(BAR, art.ceilingEntry("BAQ").getKey());
		Assert.assertEquals(BAZ, art.ceilingEntry("BAS").getKey());
		Assert.assertEquals(BOZ, art.ceilingEntry("BBA").getKey());
		Assert.assertNull(art.ceilingEntry("BPA"));
		Assert.assertEquals("1", art.ceilingEntry("BAQ").getValue());
		Assert.assertEquals("2", art.ceilingEntry("BAS").getValue());
		Assert.assertEquals("3", art.ceilingEntry("BBA").getValue());

		// floor tests

		Assert.assertEquals(BAR, art.floorKey(BAR));
		Assert.assertEquals(BAZ, art.floorKey(BAZ));
		Assert.assertEquals(BOZ, art.floorKey(BOZ));

		Assert.assertEquals(BAR, art.floorEntry(BAR).getKey());
		Assert.assertEquals(BAZ, art.floorEntry(BAZ).getKey());
		Assert.assertEquals(BOZ, art.floorEntry(BOZ).getKey());
		Assert.assertEquals("1", art.floorEntry(BAR).getValue());
		Assert.assertEquals("2", art.floorEntry(BAZ).getValue());
		Assert.assertEquals("3", art.floorEntry(BOZ).getValue());


		Assert.assertNull(art.floorKey("BAQ"));
		Assert.assertEquals(BAR, art.floorKey("BAS"));
		Assert.assertEquals(BAZ, art.floorKey("BBA"));
		Assert.assertEquals(BOZ, art.floorKey("BPA"));

		Assert.assertNull(art.floorEntry("BAQ"));
		Assert.assertEquals(BAR, art.floorEntry("BAS").getKey());
		Assert.assertEquals(BAZ, art.floorEntry("BBA").getKey());
		Assert.assertEquals(BOZ, art.floorEntry("BPA").getKey());
		Assert.assertEquals("1", art.floorEntry("BAS").getValue());
		Assert.assertEquals("2", art.floorEntry("BBA").getValue());
		Assert.assertEquals("3", art.floorEntry("BPA").getValue());

		// higher key test
		Assert.assertEquals(BAZ, art.higherKey(BAR));
		Assert.assertEquals(BOZ, art.higherKey(BAZ));
		Assert.assertNull(art.higherKey(BOZ));

		Assert.assertEquals(BAZ, art.higherEntry(BAR).getKey());
		Assert.assertEquals(BOZ, art.higherEntry(BAZ).getKey());
		Assert.assertEquals("2", art.higherEntry(BAR).getValue());
		Assert.assertEquals("3", art.higherEntry(BAZ).getValue());

		Assert.assertEquals(BAR, art.higherKey("BAQ"));
		Assert.assertEquals(BAZ, art.higherKey("BAY"));
		Assert.assertEquals(BOZ, art.higherKey("BOY"));

		Assert.assertEquals(BAR, art.higherEntry("BAQ").getKey());
		Assert.assertEquals(BAZ, art.higherEntry("BAY").getKey());
		Assert.assertEquals(BOZ, art.higherEntry("BOY").getKey());
		Assert.assertEquals("1", art.higherEntry("BAQ").getValue());
		Assert.assertEquals("2", art.higherEntry("BAY").getValue());
		Assert.assertEquals("3", art.higherEntry("BOY").getValue());

		// lower key test
		Assert.assertEquals(BAR, art.lowerKey(BAZ));
		Assert.assertEquals(BAZ, art.lowerKey(BOZ));

		Assert.assertEquals(BAR, art.lowerEntry(BAZ).getKey());
		Assert.assertEquals(BAZ, art.lowerEntry(BOZ).getKey());
		Assert.assertEquals("1", art.lowerEntry(BAZ).getValue());
		Assert.assertEquals("2", art.lowerEntry(BOZ).getValue());

		Assert.assertEquals(BAR, art.lowerKey("BAS"));
		Assert.assertEquals(BAZ, art.lowerKey("BBA"));
		Assert.assertEquals(BOZ, art.lowerKey("BPA"));

		Assert.assertEquals(BAR, art.lowerEntry("BAS").getKey());
		Assert.assertEquals(BAZ, art.lowerEntry("BBA").getKey());
		Assert.assertEquals(BOZ, art.lowerEntry("BPA").getKey());
		Assert.assertEquals("1", art.lowerEntry("BAS").getValue());
		Assert.assertEquals("2", art.lowerEntry("BBA").getValue());
		Assert.assertEquals("3", art.lowerEntry("BPA").getValue());

		// first key test

		Map.Entry<String, String> firstEntry = art.firstEntry();
		Assert.assertEquals(BAR, firstEntry.getKey());
		Assert.assertEquals(BAR, art.firstKey());
		Assert.assertEquals("1", firstEntry.getValue());

		// last key test

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

		// iterate and remove all
		Assert.assertEquals(2, art.size());
		Iterator<Map.Entry<String, String>> it = art.entrySet().iterator();
		try {
			it.remove();
			Assert.fail();
		}
		catch(IllegalStateException e){}
		it.next();
		it.remove();
		Assert.assertEquals(1, art.size());
		it.next();
		it.remove();
		try {
			it.remove();
			Assert.fail();
		}
		catch(IllegalStateException e){}

		Assert.assertEquals(0, art.size());

		try {
			it.next();
			Assert.fail();
		} catch (NoSuchElementException e){}
 	}

 	@Test
	public void testEntrySetRemoval(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BARCA, "1"));

		Set<Map.Entry<String, String>> set =  art.entrySet();
		Assert.assertTrue(set.contains(new AbstractMap.SimpleEntry<>(BARCA, "1")));
		Assert.assertFalse(set.contains(new AbstractMap.SimpleEntry<>(BARCA, "2")));
		Assert.assertFalse(set.remove(new AbstractMap.SimpleEntry<>(BARCA, "2")));
		Assert.assertTrue(set.remove(new AbstractMap.SimpleEntry<>(BARCA, "1")));
	}

	@Test
	public void testClear(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BARCA, "1"));
		Assert.assertEquals(1, art.size());
		art.clear();
		Assert.assertEquals(0, art.size());
		Assert.assertNull(art.get(BARCA));
	}

	@Test
	public void testPollFirstEntry(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertEquals(2, art.size());
		Map.Entry<String, String> entry = art.pollFirstEntry();
		Assert.assertEquals(entry.getKey(), BAR);
		Assert.assertEquals(entry.getValue(), "1");
		Assert.assertNull(art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		entry = art.pollFirstEntry();
		Assert.assertEquals(entry.getKey(), BAZ);
		Assert.assertEquals(entry.getValue(), "2");
		Assert.assertEquals(0, art.size());
	}

	@Test
	public void testPollLastEntry(){
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertEquals(2, art.size());
		Map.Entry<String, String> entry = art.pollLastEntry();
		Assert.assertEquals(entry.getKey(), BAZ);
		Assert.assertEquals(entry.getValue(), "2");
		Assert.assertNull(art.get(BAZ));
		Assert.assertEquals("1", art.get(BAR));
		entry = art.pollFirstEntry();
		Assert.assertEquals(entry.getKey(), BAR);
		Assert.assertEquals(entry.getValue(), "1");
		Assert.assertEquals(0, art.size());
	}

	@Test
	public void testSharedPrefixRemove_onlyChildInnerNode() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

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
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
	}

	/*
		should cause initial lazy stored leaf to split and have "BA" path compressed
	 */
	@Test
	public void testSharedPrefixInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
	}

	@Test
	public void testBreakCompressedPath() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3")); // breaks compressed path of BAR, BAZ
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testReplace() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));

		Assert.assertEquals("1", art.put(BAR, "2"));
		Assert.assertEquals("2", art.get(BAR));
	}

	// we'll have to provide nice Serdes that can take care of strings this way
	@Test
	public void testPrefixesInsert() {
		AdaptiveRadixTree<String, String> art = new AdaptiveRadixTree<>(BinaryComparables.UTF8);

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));

		Assert.assertNull(art.put(BARCA, "2"));
		Assert.assertEquals("2", art.get(BARCA));
	}
}
