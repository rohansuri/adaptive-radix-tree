package art;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.NavigableMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

// https://github.com/openjdk/jdk13/blob/master/test/micro/org/openjdk/bench/java/util/HashMapBench.java
public class Lookup {

	@State(Scope.Benchmark)
	public static class Dense {
		Map<Integer, Object> m;
		@Param({"65000", "16000000", "256000000"}) // 65k, 16m, 256m
		int size;

		public enum MapType {
			HASH_MAP,
			ART,
			TREE_MAP
		}

		@Param
		MapType mapType;

		@Setup
		public void setup() {
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
			List<Integer> keys = new ArrayList<>(size);

			// dense keys
			for (int i = 0; i < size; i++) {
				keys.add(i);
			}

			// randomly permute keys
			Collections.shuffle(keys);

			// insert into map
			for (int key : keys) {
				m.put(key, holder);
			}
		}
	}

	@Benchmark
	@BenchmarkMode(Mode.Throughput)
	public Object dense(Dense d) {
		// https://hg.openjdk.java.net/code-tools/jmh/file/99d7b73cf1e3/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_38_PerInvokeSetup.java#l124
		int x = ThreadLocalRandom.current().nextInt(0, d.size);
		return d.m.get(x);
	}
}
