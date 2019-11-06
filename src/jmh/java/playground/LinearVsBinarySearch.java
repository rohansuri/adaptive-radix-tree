package playground;

import com.github.rohansuri.art.string.LargeData;
import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LinearVsBinarySearch {

	@State(Scope.Benchmark)
	public static class Data {
		byte keys[];
		byte toLookup[];

		@Param({"4", "16"})
		int size;

		@Setup
		public void setup() {
			keys = new byte[size];
			toLookup = new byte[size];
			ThreadLocalRandom.current().nextBytes(keys);
			Arrays.sort(keys);
			System.arraycopy(keys, 0, toLookup, 0, keys.length);
			ArrayUtils.shuffle(toLookup);
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void linear(Blackhole b, Data d) {
		for (int i = 0; i < d.toLookup.length; i++) {
			b.consume(linear(d.keys, d.toLookup[i]));
		}
	}

	private int linear(byte[] keys, byte key) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == key) {
				return i;
			}
		}
		return -1;
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void binary(Blackhole b, Data d) {
		for (int i = 0; i < d.toLookup.length; i++) {
			b.consume(Arrays.binarySearch(d.keys, d.toLookup[i]));
		}
	}

}
