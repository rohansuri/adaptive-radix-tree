package com.github.rohansuri.art;

import java.util.Arrays;

public class NavigableKeySetStringTest extends AbstractNavigableMapTest.TestNavigableKeySet<String, String> {
	public NavigableKeySetStringTest(AbstractNavigableMapTest<String, String> main) {
		super(main);
	}

	@Override
	public String[] getFullNonNullElements() {
		String[] elements = new String[30];

		for (int i = 0; i < 30; ++i) {
			elements[i] = String.valueOf(i + i + 1);
		}
		// AbstractNavigableSetTest requires the sample elements to be sorted
		// since the set's views take subviews based on hard set indices (lobound, hibound)
		// of the provided elements.
		// we should make it depend on the given map's order, just like sub view tests of
		// AbstractNavigableMapTest do.
		Arrays.sort(elements);
		return elements;
	}

	@Override
	public String[] getOtherNonNullElements() {
		String[] elements = new String[30];

		for (int i = 0; i < 30; ++i) {
			elements[i] = String.valueOf(i + i + 2);
		}

		Arrays.sort(elements);
		return elements;
	}
}
