package com.github.rohansuri.art.acc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.collections4.map.AbstractSortedMapTest;
import org.junit.jupiter.api.Test;

public class ARTIntegerTest extends AbstractSortedMapTest {

	private final List<Integer> sampleKeys;

	public ARTIntegerTest() {
		super("ARTIntegerTest");
		int level = 4;
		sampleKeys = new ArrayList<>();
		pc(sampleKeys, ByteBuffer.allocate(level), 0, level);
		assertEquals(2 * 2 * 2 * 256, sampleKeys.size());
	}

	/*
		(shrink, grow tested)
		adds 256 children on last level hence we test growth from
		Node4 -> Node16 -> Node48 -> Node256
		and shrink in reverse as well.

		(path compression tested)
		since keys are added in sorted order,
		first 256 keys for example will all have their upper level partial keys as 0
		and hence have path "000" compressed. Only 4th byte would differ.

		(single child node replacement tested)
		since we're on multiple levels, on removals we'd see:
		single child nodes getting replaced by with adjusted compressed path

	 */
	private void pc(List<Integer> l, ByteBuffer num, int currLevel, int maxLevel) {
		if (currLevel == maxLevel) {
			int n = num.getInt(0);
			l.add(n);
			return;
		}

		int choices = currLevel == maxLevel - 1 ? 256 : 2;
		for (int i = 0; i < choices; i++) {
			num.put((byte) i);
			pc(l, num, currLevel + 1, maxLevel);
			num.position(currLevel);
		}
	}

	@Test
	public void testSize() {
		System.out.println(sampleKeys.size());
	}

	@Override
	public Integer[] getSampleKeys() {
		return sampleKeys.toArray(new Integer[0]);
	}

	@Override
	public Integer[] getSampleValues() {
		return getSampleKeys();
	}

	@Override
	public Integer[] getOtherValues() {
		return getOtherKeys();
	}

	@Override
	public Integer[] getNewSampleValues() {
		Integer[] newValues = new Integer[getSampleValues().length];
		List<Integer> existingValues = new ArrayList<>(sampleKeys);
		existingValues.addAll(Arrays.asList(getOtherKeys()));
		for (int i = 0, j = 0; j < newValues.length; i++) {
			if (!existingValues.contains(i)) {
				newValues[j++] = i;
			}
		}
		return newValues;
	}

	@Override
	public Integer[] getOtherKeys() {
		Integer[] other = new Integer[super.getOtherKeys().length];
		for (int i = 0, j = 0; j < other.length; i++) {
			if (!sampleKeys.contains(i)) {
				other[j++] = i;
			}
		}
		return other;
	}

	@Override
	public SortedMap makeObject() {
		return new AdaptiveRadixTree<>(BinaryComparables.forInteger());
	}
}
