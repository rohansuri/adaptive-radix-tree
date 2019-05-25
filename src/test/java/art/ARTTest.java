package art;

import org.junit.Assert;
import org.junit.Test;

public class ARTTest {
	private static final byte[] BAR = "BAR".getBytes();
	private static final byte[] BAZ = "BAZ".getBytes();
	private static final byte[] BOZ = "BOZ".getBytes();

	@Test
	public void testSingleInsert(){
		ART<String> art = new ART<>();

		art.put(BAR, "1");
		Assert.assertEquals("1", art.get(BAR));
	}

	/*
		should cause initial lazy stored leaf to split and have "BA" path compressed
	 */
	@Test
	public void testSharedPrefixInsert(){
		ART<String> art = new ART<>();

		art.put(BAR, "1");
		art.put(BAZ, "2");
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
	}

	@Test
	public void testBreakCompressedPath(){
		ART<String> art = new ART<>();

		art.put(BAR, "1");
		art.put(BAZ, "2");
		art.put(BOZ, "3"); // breaks compressed path of BAR, BAZ
		Assert.assertEquals("1", art.get(BAR));
		Assert.assertEquals("2", art.get(BAZ));
		Assert.assertEquals("3", art.get(BOZ));
	}

	@Test
	public void testReplace(){
		ART<String> art = new ART<>();

		art.put(BAR, "1");
		Assert.assertEquals("1", art.get(BAR));

		art.put(BAR, "2");
		Assert.assertEquals("2", art.get(BAR));
	}
}
