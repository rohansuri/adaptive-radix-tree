package playground;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.apache.commons.io.IOUtils;

// https://www.evanjones.ca/software/java-string-encoding.html
// https://github.com/nitsanw/javanetperf/blob/psylobsaw/src/StringEncodingTest.java
// Isolated benchmark to see if offering users an opportunity to reuse byte array
// is advantageous or not.

public class StringEncoding {

	@State(Scope.Benchmark)
	public static class Data {
		String keys[];
		StringBinaryComparable bc;
		StringBinaryComparable2 bc2;

		@Setup
		public void setup() throws IOException, URISyntaxException {
			List<String> s = IOUtils
					.readLines(this.getClass().getResourceAsStream("/words.txt"), StandardCharsets.UTF_8);
			keys = s.toArray(String[]::new);
			if (keys.length != 235886) {
				throw new AssertionError("expected " + 235886 + " words from the file, got " + keys.length);
			}

			bc = new StringBinaryComparable(StandardCharsets.US_ASCII);
			bc2 = new StringBinaryComparable2(StandardCharsets.US_ASCII);
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void getBytes(Blackhole bh, Data d) throws UnsupportedEncodingException {
		for (int i = 0; i < d.keys.length; i++) {
			bh.consume(d.keys[i].getBytes("ASCII"));
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void getBytesStandardCharset(Blackhole bh, Data d) {
		for (int i = 0; i < d.keys.length; i++) {
			bh.consume(d.keys[i].getBytes(StandardCharsets.US_ASCII));
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void charSeq(Blackhole bh, Data d) {
		for (int i = 0; i < d.keys.length; i++) {
			bh.consume(d.bc.get(d.keys[i]));
		}
	}

	@Benchmark
	@BenchmarkMode({Mode.AverageTime})
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	public void getChars(Blackhole bh, Data d) {
		for (int i = 0; i < d.keys.length; i++) {
			bh.consume(d.bc2.get(d.keys[i]));
		}
	}
}
