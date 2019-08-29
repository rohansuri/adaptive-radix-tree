package art;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

// https://github.com/openjdk/jdk13/blob/master/test/micro/org/openjdk/bench/java/util/HashMapBench.java
public class Lookup {

	@State(Scope.Benchmark)
	public static class Data {
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
			System.out.println("setup for " + distributionType + " distribution");
			switch (mapType) {
			case HASH_MAP:
				m = new HashMap<>();
				break;
			case ART:
				m = new AdaptiveRadixTree<>(BinaryComparable.INTEGER);
				break;
			case TREE_MAP:
				m = new TreeMap<>();
				break;
			default:
				throw new AssertionError();
			}
			Object holder = new Object();

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
					while (m.containsKey(x));
					keys[i] = x;
				}
			}

			// insert into map
			for (int key : keys) {
				m.put(key, holder);
			}

		}
	}

	@Benchmark
	@BenchmarkMode({Mode.Throughput})
	public Object lookup(Data d) {
		// why Setup(Level.Invocation) is not used
		// http://javadox.com/org.openjdk.jmh/jmh-core/1.7/org/openjdk/jmh/annotations/Level.html
		int x = ThreadLocalRandom.current().nextInt(0, d.size);
		int key = d.keys[x];
		return d.m.get(key);
	}
}
