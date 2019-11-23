package playground;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;

public class ExtraSpaceSentinelLinearSearch {

	@State(Scope.Benchmark)
	public static class Data {
		byte keys[];
		byte toLookup[];

		@Param({"16"})
		int size;

		@Setup
		public void setup() {
			keys = new byte[size + 1];
			toLookup = new byte[size];
			ThreadLocalRandom.current().nextBytes(keys);
			Arrays.sort(keys);
			System.arraycopy(keys, 0, toLookup, 0, toLookup.length);
			ArrayUtils.shuffle(toLookup);
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int sentinel(Data d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += sentinel(d.keys, d.toLookup[i]); // sum to avoid Dead code elimination
		}
		return sum;
	}

	private int sentinel(byte[] keys, byte key) {
		keys[keys.length - 1] = key;
		int i = 0;
		while (keys[i] != key) i++;
		return i == keys.length - 1 ? -1 : i;
	}

}
