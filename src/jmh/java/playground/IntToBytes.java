package playground;
import java.util.concurrent.ThreadLocalRandom;


import org.openjdk.jmh.annotations.*;
import java.nio.*;

// results are the same
// because both allocate same amount of byte array
// and both do bit shifting

public class IntToBytes {
	@Benchmark
	@BenchmarkMode({Mode.Throughput})
	public byte[] byteBuffer(){
		int x = ThreadLocalRandom.current().nextInt();
		return ByteBuffer.allocate(Integer.BYTES).putInt(x).array();
	}

	@Benchmark
	@BenchmarkMode({Mode.Throughput})
	public byte[] bitShifting(){
		int value = ThreadLocalRandom.current().nextInt();
		return new byte[] {
				(byte)(value >>> 24), // top 8 bits
				(byte)(value >>> 16), // 2nd top 8 bits
				(byte)(value >>> 8), // 3rd most 8 bits
				(byte)value}; // last 8 bits
	}
}