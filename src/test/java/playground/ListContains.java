package playground;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListContains {

	// see slowness of O(n^2) * O(key comparison) in effect
	// (came across when doing verifyValues of AbstractMapTest)
	@Test
	public void testListContains() {
		int n = 300_000;
		List<String> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			l.add(UUID.randomUUID().toString());
		}
		for (int i = 0; i < l.size(); i++) {
			Assertions.assertTrue(l.contains(l.get(i)));
		}
	}
}
