package playground;

import org.junit.Assert;
import org.junit.Test;

public class SignedInterpretation {
	/*
		So we know how to always interpret bytes as unsigned and maintain the right
		lexico order.

		Now let's see how can we deal with signed integers?

		Since we've already found the way to store it in unsigned.
		All we gotta do is convert this signed into unsigned number.
		For that paper suggests us to flip the sign bit and interpret the bytes as unsigned.
		Interpreting an integer as unsigned means interpret every byte as unsigned,
		which we already know.
		So I have an intuition, only doing the initial bit flip and later store in the same previous way
		should work.
	 */
	private static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

	@Test
	public void testNode4(){
		byte storeSmallest = Byte.MIN_VALUE;
		byte storeLargest = Byte.MAX_VALUE;
		Assert.assertTrue(storeSmallest < storeLargest);

		// convert to unsigned, a one time initial thing
		byte unsignedSmallest = (byte)(storeSmallest ^ BYTE_SHIFT);
		byte unsignedLargest = (byte)(storeLargest ^ BYTE_SHIFT);

		// store as unsigned in node4
		byte x = AlwaysUnsignedInterpretation.storeAsUnsignedInNode4(unsignedLargest);
		byte y = AlwaysUnsignedInterpretation.storeAsUnsignedInNode4(unsignedSmallest);
		Assert.assertTrue(y < x);
	}

	@Test
	public void testNode48(){
		byte storeSmallest = Byte.MIN_VALUE;
		byte storeLargest = Byte.MAX_VALUE;
		Assert.assertTrue(storeSmallest < storeLargest);

		// convert to unsigned, a one time initial thing
		byte unsignedSmallest = (byte)(storeSmallest ^ BYTE_SHIFT);
		byte unsignedLargest = (byte)(storeLargest ^ BYTE_SHIFT);

		int x = AlwaysUnsignedInterpretation.storeAsUnsignedInNode48(unsignedSmallest);
		int y = AlwaysUnsignedInterpretation.storeAsUnsignedInNode48(unsignedLargest);
		Assert.assertTrue(y > x);
	}
}
