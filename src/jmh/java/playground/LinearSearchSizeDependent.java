package playground;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.infra.Blackhole;

public class LinearSearchSizeDependent {
	@State(Scope.Benchmark)
	public static class Data16 {
		byte keys[];
		byte toLookup[];
		int limits[];

		@Setup
		public void setup() {
			int size = 16;
			keys = new byte[size];
			toLookup = new byte[size];

			limits = new int[size];
			for (int i = 0; i < limits.length; i++) {
				ThreadLocalRandom.current().nextInt(0, size + 1);
			}

			ThreadLocalRandom.current().nextBytes(keys);
			Arrays.sort(keys);
			System.arraycopy(keys, 0, toLookup, 0, keys.length);
			ArrayUtils.shuffle(toLookup);
		}
	}

	private static abstract class Unroll {
		abstract int find(byte[] keys, int key);
	}

	private static class Unroll0 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			return -1;
		}
	}

	private static class Unroll1 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			return keys[0] == key ? 0 : -1;
		}
	}

	private static class Unroll2 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			return -1;
		}
	}

	private static class Unroll3 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			return -1;
		}
	}

	private static class Unroll4 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			return -1;
		}
	}

	private static class Unroll5 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			if (keys[4] == key) return 4;
			return -1;
		}
	}

	private static class Unroll6 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			if (keys[4] == key) return 4;
			if (keys[5] == key) return 5;
			return -1;
		}
	}

	private static class Unroll7 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			if (keys[4] == key) return 4;
			if (keys[5] == key) return 5;
			if (keys[6] == key) return 6;
			return -1;
		}
	}

	private static class Unroll8 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			if (keys[4] == key) return 4;
			if (keys[5] == key) return 5;
			if (keys[6] == key) return 6;
			if (keys[7] == key) return 7;
			return -1;
		}
	}

	private static class Unroll9 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
			if (keys[0] == key) return 0;
			if (keys[1] == key) return 1;
			if (keys[2] == key) return 2;
			if (keys[3] == key) return 3;
			if (keys[4] == key) return 4;
			if (keys[5] == key) return 5;
			if (keys[6] == key) return 6;
			if (keys[7] == key) return 7;
			if (keys[8] == key) return 8;
			return -1;
		}
	}


	private static class Unroll10 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}

	private static class Unroll11 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}

	private static class Unroll12 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}

	private static class Unroll13 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}


	private static class Unroll14 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}

	private static class Unroll15 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
			return -1;
		}
	}


	private static class Unroll16 extends Unroll {

		@Override
		int find(byte[] keys, int key) {
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
	}

	private static final Unroll[] UNROLLED = new Unroll[] {
			new Unroll0(),
			new Unroll1(),
			new Unroll2(),
			new Unroll3(),
			new Unroll4(),
			new Unroll5(),
			new Unroll6(),
			new Unroll7(),
			new Unroll8(),
			new Unroll9(),
			new Unroll10(),
			new Unroll11(),
			new Unroll12(),
			new Unroll13(),
			new Unroll14(),
			new Unroll15(),
			new Unroll16(),
	};

	private int find(byte[] keys, int limit, byte key){
		for(int i = 0; i < limit; i++){
			if(keys[i] == key){
				return i;
			}
		}
		return -1;
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int looped(Blackhole b, Data16 d){
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			int limit = d.limits[i];
			sum += find(d.keys, limit, d.toLookup[i]);
		}
		return sum;
	}


	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public int unrolled(Data16 d) {
		int sum = 0;
		for (int i = 0; i < d.toLookup.length; i++) {
			int limit = d.limits[i];
			sum += UNROLLED[limit].find(d.keys, d.toLookup[i]);
		}
		return sum;
	}
}


