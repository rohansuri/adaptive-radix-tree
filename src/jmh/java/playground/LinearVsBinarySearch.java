package playground;

import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
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
	public int linear(Data d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += linear(d.keys, d.toLookup[i]);
		}
		return sum;
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int sentinel(Data d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += (sentinel(d.keys, d.toLookup[i]));
		}
		return sum;
	}

	// https://stackoverflow.com/a/2741888/3804127
	// https://schani.wordpress.com/2010/04/30/linear-vs-binary-search/
	private int sentinel(byte[] keys, byte key) {
		byte last = keys[keys.length - 1];
		keys[keys.length - 1] = key;
		int i = 0;
		while (keys[i] != key) i++;
		keys[keys.length - 1] = last;
		return i < keys.length - 1 || key == last ? i : -1;
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
	public int binary(Data d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			sum += Arrays.binarySearch(d.keys, d.toLookup[i]);
		}
		return sum;
	}

}
