package art;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class BinaryComparableUtilsTest {

	@Test
	public void testTerminator() {
		// empty string case
		assertArray("");

		// normal case, test last portion copy
		assertArray("barca");
	}

	@Test
	public void testTerminatorNullCase() {
		String s = new String(new byte[] {0}); // null
		byte[] bytes = BinaryComparable.UTF8.get(s);
		int terminatorLen = BinaryComparableUtils.TERMINATOR.length;
		Assert.assertEquals(s.length() + terminatorLen + 1, bytes.length);
		byte[] expected = new byte[] {0, 1, 0, 0};
		Assert.assertArrayEquals(expected, bytes);
	}

	@Test
	public void testTerminatorMultipleNullsCase() {
		String s = new String(new byte[] {0, 76, 0, 69, 0, 79, 0}); // null, L, null, E, null, O, null
		byte[] bytes = BinaryComparable.UTF8.get(s);
		int terminatorLen = BinaryComparableUtils.TERMINATOR.length;
		// 3 because of the three "1" bytes added for each of the nulls
		Assert.assertEquals(s.length() + terminatorLen + 4, bytes.length);
		byte[] expected = new byte[] {0, 1, 76, 0, 1, 69, 0, 1, 79, 0, 1, 0, 0};
		Assert.assertArrayEquals(expected, bytes);
	}

	private void assertArray(String s) {
		int terminatorLen = BinaryComparableUtils.TERMINATOR.length;
		byte[] bytes = BinaryComparable.UTF8.get(s);
		Assert.assertEquals(s.length() + terminatorLen, bytes.length);
		ByteArrayOutputStream expected = new ByteArrayOutputStream(s.length() + terminatorLen);
		expected.write(s.getBytes(StandardCharsets.US_ASCII), 0, s.length());
		expected.write(BinaryComparableUtils.TERMINATOR, 0, terminatorLen);
		Assert.assertArrayEquals(expected.toByteArray(), bytes);
	}
}
