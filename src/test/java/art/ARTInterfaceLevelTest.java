package art;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ARTInterfaceLevelTest {
	private static final byte[] BAR = "BAR".getBytes();
	private static final byte[] BAZ = "BAZ".getBytes();
	private static final byte[] BOZ = "BOZ".getBytes();
	private static final byte[] BARCA = "BARCA".getBytes();
	private static final byte[] BARK = "BARK".getBytes();

	@Test
	public void testSingleRemove() {
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("1", art.remove(BAR));
		Assert.assertNull(art.get(BAR));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildLeaf() {
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));

		// remove BAR that shares prefix A with BAZ
		Assert.assertEquals("1", art.remove(BAR));

		// path to BAZ should still exist
		Assert.assertEquals("2", art.get(BAZ));

		// untouched
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testSharedPrefixRemove_onlyChildInnerNode() {
		ART<String> art = new ART<>();

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
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));
	}

	/*
		should cause initial lazy stored leaf to split and have "BA" path compressed
	 */
	@Test
	public void testSharedPrefixInsert() {
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
	}

	@Test
	public void testBreakCompressedPath() {
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertNull(art.put(BAZ, "2"));
		Assert.assertNull(art.put(BOZ, "3")); // breaks compressed path of BAR, BAZ
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testReplace() {
		ART<String> art = new ART<>();

		Assert.assertNull(art.put(BAR, "1"));
		Assert.assertEquals("1", art.get(BAR));

		Assert.assertEquals("1", art.put(BAR, "2"));
		Assert.assertEquals("2", art.get(BAR));
	}

	// we'll have to provide nice Serdes that can take care of strings this way
	@Test
	public void testPrefixesInsert() {
		ART<String> art = new ART<>();

		byte[] bar = nullTerminated(BAR);
		Assert.assertNull(art.put(bar, "1"));
		Assert.assertEquals("1", art.get(bar));
		Assert.assertNull(art.get(BAR));

		byte[] barca = nullTerminated("BARCA".getBytes());
		Assert.assertNull(art.put(barca, "2"));
		Assert.assertEquals("2", art.get(barca));
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
		ART<String> art = new ART<>();

		// insert all

		for (short i = 0; i < Short.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Short.BYTES).putShort(i).array();
			String value = String.valueOf(i);
			// System.out.println("value to be added = " + value);
			// System.out.println(Arrays.toString(key));
			Assert.assertNull(art.put(key, value));
			Assert.assertEquals(value, art.get(key));
		}

		// get all after inserting everything

		for (short i = 0; i < Short.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Short.BYTES).putShort(i).array();
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.get(key));
		}
	}

	@Test
	public void testInsertingAndDeletingAllInt8BitIntegers() {
		ART<String> art = new ART<>();

		// insert all

		for (int i = 0; i < 256; i++) {
			byte[] key = new byte[] {(byte) i};
			String value = String.valueOf(i);
			Assert.assertNull(art.put(key, value));
			Assert.assertEquals(value, art.get(key));
		}

		// remove one by one and check if others exist
		for (int i = 0; i < 256; i++) {
			byte[] key = new byte[] {(byte) i};
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.remove(key));
			Assert.assertNull(art.get(key));

			// others should exist
			if (i < 255) {
				IntStream.range(i + 1, 256).parallel()
						.forEach(x -> {
							byte[] _key = new byte[] {(byte) x};
							String _value = String.valueOf(x);
							Assert.assertEquals(_value, art.get(_key));
						});
			}
		}
	}

	@Test
	@Ignore // takes too long (1m 20secs locally)
	public void testInsertingAndDeletingAllInt16BitIntegers() {
		ART<String> art = new ART<>();

		// insert all

		for (short i = 0; i < Short.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Short.BYTES).putShort(i).array();
			String value = String.valueOf(i);
			Assert.assertNull(art.put(key, value));
			Assert.assertEquals(value, art.get(key));
		}

		// remove one by one and check if others exist
		for (short i = 0; i < Short.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Short.BYTES).putShort(i).array();
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.remove(key));
			Assert.assertNull(art.get(key));

			// others should exist
			if (i < Short.MAX_VALUE - 1) {
				IntStream.range(i + 1, Short.MAX_VALUE).parallel()
						.forEach(x -> {
							short j = (short) x;
							byte[] _key = ByteBuffer.allocate(Short.BYTES).putShort(j).array();
							String _value = String.valueOf(j);
							Assert.assertEquals(_value, art.get(_key));
						});
			}
		}
	}

	@Test
	@Ignore // heavy test (in terms of?). Disable logging
	public void testInsertingAllInt32BitIntegers() {
		ART<String> art = new ART<>();

		// insert all

		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
			String value = String.valueOf(i);
			Assert.assertNull(art.put(key, value));
			Assert.assertEquals(value, art.get(key));
		}

		// get all after inserting everything

		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			byte[] key = ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
			String value = String.valueOf(i);
			Assert.assertEquals(value, art.get(key));
		}
	}

	private byte[] nullTerminated(byte[] key) {
		// is this the best way?
		byte[] keyBytes = new byte[key.length + 1];
		System.arraycopy(key, 0, keyBytes, 0, key.length);
		return keyBytes;
	}

	private byte[] nullTerminated(String key) {
		return nullTerminated(key.getBytes());
	}
}
