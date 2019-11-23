package com.github.rohansuri.art.integer;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.collections4.trie.GenericPatriciaTrie;
import org.ardverk.collection.IntegerKeyAnalyzer;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jol.info.GraphLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// https://github.com/openjdk/jdk13/blob/master/test/micro/org/openjdk/bench/java/util/HashMapBench.java
public class Lookup {

	@State(Scope.Benchmark)
	public static class Data {
		Set<Integer> keySet; // for the purpose of dedup when preparing random sparse data set
		Integer[] keys;
		Map<Integer, Object> m;
		@Param({"100", "1000", "10000", "100000", "1000000", "10000000"})
		int size;

		public enum MapType {
			HASH_MAP,
			ART,
			TREE_MAP,
			PATRICIA_TRIE
		}

		public enum DistributionType {
			SPARSE,
			DENSE
		}

		@Param
		DistributionType distributionType;

		@Param({"ART", "TREE_MAP"})
		MapType mapType;

		@Setup
		public void setup() {
			switch (mapType) {
			case HASH_MAP:
				m = new HashMap<>();
				break;
			case ART:
				m = new AdaptiveRadixTree<>(BinaryComparables.forInteger());
				break;
			case TREE_MAP:
				m = new TreeMap<>();
				break;
			case PATRICIA_TRIE:
				m = new GenericPatriciaTrie<Integer, Object>(IntegerKeyAnalyzer.INSTANCE);
				break;
			default:
				throw new AssertionError();
			}
			Object holder = new Object();
			keySet = new HashSet<>(size);
			keys = new Integer[size];

			// TODO: refactor if-else block into a KeyGenerator
			if (distributionType == DistributionType.DENSE) {
				// dense keys
				// 0, 1, 2, 3, 4, .... ,size
				for (int i = 0; i < size; i++) {
					keys[i] = i;
				}
				ArrayUtils.shuffle(keys);
			}
			else if (distributionType == DistributionType.SPARSE) {
				// sparse keys
				int x;
				for (int i = 0; i < size; i++) {
					do {
						x = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
					}
					while (keySet.contains(x));
					keys[i] = x;
					keySet.add(x);
				}
			}
			else {
				throw new IllegalArgumentException("not a valid distribution type");
			}
			// insert into map
			for (int key : keys) {
				m.put(key, holder);
			}
			System.out
					.printf("\n\tmapType:%s, distributionType:%s, size:%d\n%s\n", mapType, distributionType, size, GraphLayout
							.parseInstance(m).toFootprint());
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int integer(Blackhole bh, Data d) {
		for (int i = 0; i < d.size; i++) {
			bh.consume(d.m.get(d.keys[i]));
		}
		return d.m.size();
	}
}
