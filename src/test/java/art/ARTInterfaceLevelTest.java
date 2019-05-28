package art;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class ARTInterfaceLevelTest {
	private static final byte[] BAR = "BAR".getBytes();
	private static final byte[] BAZ = "BAZ".getBytes();
	private static final byte[] BOZ = "BOZ".getBytes();

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

		for(short i = 0; i < Short.MAX_VALUE; i++){
			byte[] key = ByteBuffer.allocate(Short.BYTES).putShort(i).array();
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
