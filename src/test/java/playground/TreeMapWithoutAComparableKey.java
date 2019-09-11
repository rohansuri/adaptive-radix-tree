package playground;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TreeMapWithoutAComparableKey {

	private static class Foo { }

	@Test
	public void test() {
		Map<Foo, String> map = new TreeMap<>();
		Assertions.assertThrows(ClassCastException.class, () -> map.put(new Foo(), "hello"));
	}
}
