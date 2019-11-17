package com.github.rohansuri.art.Long;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.collections4.trie.GenericPatriciaTrie;
import org.ardverk.collection.IntegerKeyAnalyzer;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Insert {

	@State(Scope.Benchmark)
	public static class Data {
		Object holder;
		Set<Long> keySet; // for the purpose of dedup when preparing random sparse data set
		Long[] keys;
		Supplier<Map<Long, Object>> supplier;
		@Param({"100", "1000", "10000", "100000", "1000000"})
		int size;

		public enum MapType {
			HASH_MAP,
			ART,
			TREE_MAP,
			//PATRICIA_TRIE
		}

		public enum DistributionType {
			SPARSE,
			DENSE_SORTED,
			DENSE_SHUFFLE
		}

		@Param
		DistributionType distributionType;

		@Param
		MapType mapType;

		@Setup
		public void setup() {
			switch (mapType) {
			case HASH_MAP:
				supplier = () -> new HashMap<>();
				break;
			case ART:
				supplier = () -> new AdaptiveRadixTree<>(BinaryComparables.forLong());
				break;
			case TREE_MAP:
				supplier = () -> new TreeMap<>();
				break;
			/*case PATRICIA_TRIE:
				supplier = () -> new GenericPatriciaTrie<Long, Object>(IntegerKeyAnalyzer.INSTANCE);
				break;*/
			default:
				throw new AssertionError();
			}

			holder = new Object();
			keys = new Long[size];
			keySet = new HashSet<>(size);

			// TODO: refactor if-else block into a KeyGenerator
			if (distributionType == DistributionType.DENSE_SORTED ||
					distributionType == DistributionType.DENSE_SHUFFLE) {
				// dense keys
				// 0, 1, 2, 3, 4, .... ,size
				for (int i = 0; i < size; i++) {
					keys[i] = (long) i;
				}

				if (distributionType == DistributionType.DENSE_SHUFFLE) {
					ArrayUtils.shuffle(keys);
				}
			}
			else if (distributionType == DistributionType.SPARSE) {
				// sparse keys
				long x;
				for (int i = 0; i < size; i++) {
					do {
						x = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
					}
					while (keySet.contains(x));
					keys[i] = x;
					keySet.add(x);
				}
			}
			else {
				throw new IllegalArgumentException("not a valid distribution type");
			}
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int Long(Blackhole bh, Data d) {
		Map<Long, Object> m = d.supplier.get();
		for (int i = 0; i < d.size; i++) {
			bh.consume(m.put(d.keys[i], d.holder));
		}
		return m.size();
	}
}