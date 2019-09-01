package art;

import org.openjdk.jmh.annotations.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.apache.commons.lang3.ArrayUtils;
import java.util.concurrent.TimeUnit;

public class Insert {

	@State(Scope.Benchmark)
	public static class Data {
		Object holder;
		Set<Integer> keySet; // for the purpose of dedup when preparing random sparse data set
		int[] keys;
		Supplier<Map<Integer, Object>> supplier;
		@Param({"1000000"}) // 1m
		int size;

		public enum MapType {
			HASH_MAP,
			ART,
			TREE_MAP
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
				supplier = () -> new AdaptiveRadixTree<>(BinaryComparables.INTEGER);
				break;
			case TREE_MAP:
				supplier = () -> new TreeMap<>();
				break;
			default:
				throw new AssertionError();
			}

			holder = new Object();
			keys = new int[size];
			keySet = new HashSet<>(size);

			// TODO: refactor if-else block into a KeyGenerator
			if (distributionType == DistributionType.DENSE_SORTED ||
				distributionType == DistributionType.DENSE_SHUFFLE) {
				// dense keys
				// 0, 1, 2, 3, 4, .... ,size
				for (int i = 0; i < size; i++) {
					keys[i] = i;
				}

				if(distributionType == DistributionType.DENSE_SHUFFLE){
					ArrayUtils.shuffle(keys);
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
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int integer(Data d) {
		Map<Integer, Object> m = d.supplier.get();
		for (int i = 0; i < d.size; i++) {
			m.put(d.keys[i], d.holder);
		}
		return m.size();
	}
}