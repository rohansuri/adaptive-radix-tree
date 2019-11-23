package com.github.rohansuri.art.acc.bm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NavigableMap;
import java.util.UUID;

import com.github.rohansuri.art.AbstractNavigableMapShortTest;
import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

public class UUIDTest extends AbstractNavigableMapShortTest<String, String> {
	public UUIDTest(String testName) {
		super(testName);
	}

	private static final String[] sampleKeys = loadUUIDs();
	private static final String[] newSampleValues = reverseSampleKeys();
	private static final String[] otherKeys = generateOtherKeys();

	private static String[] generateOtherKeys() {
		int n = 10;
		String[] otherKeys = new String[n];
		for (int i = 0; i < 10; i++) {
			otherKeys[i] = UUID.randomUUID().toString();
		}
		return otherKeys;
	}

	private static String[] reverseSampleKeys() {
		String[] keys = sampleKeys.clone();
		ArrayUtils.reverse(keys);
		return keys;
	}

	private static String[] loadUUIDs() {
		try {
			List<String> lines = IOUtils
					.readLines(UUIDTest.class.getResourceAsStream("/uuid.txt"), StandardCharsets.UTF_8);
			return lines.toArray(new String[0]);
		}
		catch (IOException e) {
			throw new RuntimeException("failed to load uuids", e);
		}
	}

	@Override
	public String[] getSampleKeys() {
		return sampleKeys;
	}

	@Override
	public String[] getSampleValues() {
		return sampleKeys;
	}

	@Override
	public String[] getNewSampleValues() {
		return newSampleValues;
	}

	@Override
	public String[] getOtherKeys() {
		return otherKeys;
	}

	@Override
	public String[] getOtherValues() {
		return otherKeys;
	}

	@Override
	public NavigableMap<String, String> makeObject() {
		return new AdaptiveRadixTree<>(BinaryComparables.forString());
	}
}
