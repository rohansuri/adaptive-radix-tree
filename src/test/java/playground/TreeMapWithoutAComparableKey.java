package playground;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

public class TreeMapWithoutAComparableKey {

	private static class Foo { }

	@Test(expected = ClassCastException.class)
	public void test(){
		Map<Foo, String> map = new TreeMap<>();
		map.put(new Foo(), "hello");
	}
}
