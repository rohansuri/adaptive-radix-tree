package playground;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysUnsignedInterpretation {
	/*
	byte span in paper is unsigned.
	so how can we interpret given bits totally as unsigned for node4 and node48 types?

	Doing a flip (of each byte in key) and then storing in node4, will keep it in sorted order
	(lexicographically bitwise)
	even though it'll be stored as signed byte, the order would be maintained.

	For node48, since we index into array as an integer, we only have to interpret the byte as unsigned.

	Then for storing signed integers, we do key transformations as stated in paper and then
	they're ready to be stored in the trie.
	 */

	// 2^7 = 128
	private static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

	@Test
	public void testForNode4() { // still stored as byte so range available -128 to 127
		byte largestLexicoWise = (byte) 0b11111111;
		byte smallestLexicoWise = (byte) 0b00000000;
		Assert.assertTrue(smallestLexicoWise > largestLexicoWise);
		Assert.assertTrue(Byte.toUnsignedInt(largestLexicoWise) > Byte.toUnsignedInt(smallestLexicoWise));

		byte x = storeAsUnsignedInNode4(largestLexicoWise);
		byte y = storeAsUnsignedInNode4(smallestLexicoWise);
		Assert.assertTrue(x > y);

		System.out.println("largestLexicoWise = " + largestLexicoWise);
		System.out.println("largestLexicoWise after flip = " + x);
		System.out.println("smallestLexicoWise = " + smallestLexicoWise);
		System.out.println("smallestLexicoWise after flip = " + y);
	}

	static byte storeAsUnsignedInNode4(byte b){
		return (byte)(b ^ BYTE_SHIFT);
	}

	@Test
	public void testForNode48() { // integer indexed, so range 0 to 255
		byte largestLexicoWise = (byte) 0b11111111;
		byte smallestLexicoWise = (byte) 0b00000000;
		Assert.assertTrue(smallestLexicoWise > largestLexicoWise);

		int x = storeAsUnsignedInNode48(largestLexicoWise);
		int y = storeAsUnsignedInNode48(smallestLexicoWise);
		Assert.assertTrue(x > y);

		System.out.println("largestLexicoWise = " + largestLexicoWise);
		System.out.println("largestLexicoWise after flip = " + x); // 255
		System.out.println("smallestLexicoWise = " + smallestLexicoWise);
		System.out.println("smallestLexicoWise after flip = " + y); // 0
	}

	static int storeAsUnsignedInNode48(byte b){
		return Byte.toUnsignedInt(b);
	}
}
