package art;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		int[] keys;
		Map<Integer, Object> m;
		@Param({"65000", "16000000"}) // 65k, 16m
		int size;

		public enum MapType {
			HASH_MAP,
			ART,
			TREE_MAP
		}

		public enum DistributionType {
			SPARSE,
			DENSE
		}

		@Param
		DistributionType distributionType;

		@Param
		MapType mapType;

		@Setup
		public void setup() {
			switch (mapType) {
			case HASH_MAP:
				m = new HashMap<>();
				break;
			case ART:
				m = new AdaptiveRadixTree<>(BinaryComparables.INTEGER);
				break;
			case TREE_MAP:
				m = new TreeMap<>();
				break;
			default:
				throw new AssertionError();
			}
			Object holder = new Object();
			keySet = new HashSet<>(size);
			keys = new int[size];

			// TODO: refactor if-else block into a KeyGenerator
			if (distributionType == DistributionType.DENSE) {
				// dense keys
				// 0, 1, 2, 3, 4, .... ,size
				for (int i = 0; i < size; i++) {
					keys[i] = i;
				}
			}
			else {
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
			// insert into map
			for (int key : keys) {
				m.put(key, holder);
			}
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
