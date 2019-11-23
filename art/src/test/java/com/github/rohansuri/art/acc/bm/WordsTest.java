package com.github.rohansuri.art.acc.bm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import com.github.rohansuri.art.AbstractNavigableMapShortTest;
import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class WordsTest extends AbstractNavigableMapShortTest<String, String> {
	private static final String[] sampleKeys = loadWords();
	private static final String[] newSampleValues = newSampleValues();
	private static final String[] otherKeys = generateOtherKeys();

	private static String[] generateOtherKeys() {
		int n = 10;
		String[] otherKeys = new String[n];
		for (int i = 0; i < 10; i++) {
			otherKeys[i] = UUID.randomUUID().toString();
		}
		return otherKeys;
	}

	private static String[] newSampleValues() {
		String[] s = new String[sampleKeys.length];
		for(int i = 0; i < s.length; i++){
			s[i] = UUID.randomUUID().toString();
		}
		return s;
	}

	private static String[] loadWords() {
		try {
			List<String> lines = IOUtils
					.readLines(WordsTest.class.getResourceAsStream("/words.txt"), StandardCharsets.UTF_8);
			return lines.toArray(new String[0]);
		}
		catch (IOException e) {
			throw new RuntimeException("failed to load words", e);
		}
	}

	public WordsTest(String testName) {
		super(testName);
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
