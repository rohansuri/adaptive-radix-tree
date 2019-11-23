package playground;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;


public class LinearSearchFixedSizeUnrolledLoop {

	@State(Scope.Benchmark)
	public static class Data4 {
		byte keys[];
		byte toLookup[];

		@Setup
		public void setup() {
			int size = 4;
			keys = new byte[size];
			toLookup = new byte[size];
			ThreadLocalRandom.current().nextBytes(keys);
			Arrays.sort(keys);
			System.arraycopy(keys, 0, toLookup, 0, keys.length);
			ArrayUtils.shuffle(toLookup);
		}
	}

	@State(Scope.Benchmark)
	public static class Data16 {
		byte keys[];
		byte toLookup[];

		@Setup
		public void setup() {
			int size = 16;
			keys = new byte[size];
			toLookup = new byte[size];
			ThreadLocalRandom.current().nextBytes(keys);
			Arrays.sort(keys);
			System.arraycopy(keys, 0, toLookup, 0, keys.length);
			ArrayUtils.shuffle(toLookup);
		}
	}

	private int unrolled4(byte[] keys, byte key) {
		if (keys[0] == key) return 0;
		if (keys[1] == key) return 1;
		if (keys[2] == key) return 2;
		if (keys[3] == key) return 3;
		return -1;
	}

	private int unrolled16(byte[] keys, byte key) {
		if (keys[0] == key) return 0;
		if (keys[1] == key) return 1;
		if (keys[2] == key) return 2;
		if (keys[3] == key) return 3;
		if (keys[4] == key) return 4;
		if (keys[5] == key) return 5;
		if (keys[6] == key) return 6;
		if (keys[7] == key) return 7;
		if (keys[8] == key) return 8;
		if (keys[9] == key) return 9;
		if (keys[10] == key) return 10;
		if (keys[11] == key) return 11;
		if (keys[12] == key) return 12;
		if (keys[13] == key) return 13;
		if (keys[14] == key) return 14;
		if (keys[15] == key) return 15;
		return -1;
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int unrolled4(Data4 d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += unrolled4(d.keys, d.toLookup[i]);
		}
		return sum;
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int unrolled16(Data16 d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += unrolled16(d.keys, d.toLookup[i]);
		}
		return sum;
	}
}
