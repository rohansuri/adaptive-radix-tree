package playground;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TwoBytesInChar {

	@Test
	public void test() {
		byte[] b = new byte[] {0, 1};
		String s = new String(b, StandardCharsets.UTF_8);
		s = s.replace("\u0000", "\u0000\u0001"); // 0 1 1
		s = s.concat("\u0000\u0000");
		System.out.println(Arrays.toString(s.getBytes())); // 0 1 1 0 0
	}
}
