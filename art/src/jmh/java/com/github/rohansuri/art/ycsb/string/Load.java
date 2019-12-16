package com.github.rohansuri.art.ycsb.string;

import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/*
    load a ycsb generated load.dat file and benchmark the loading phase
 */
public class Load {

    @State(Scope.Benchmark)
    public static class LoadData extends Data {
        Supplier<NavigableMap<String, Object>> supplier;

        @Param({"c_uniform_1000_repo_load.dat",
               "c_uniform_5000_repo_load.dat",
                "c_uniform_10000_repo_load.dat",
                "c_uniform_50000_repo_load.dat",
                "c_uniform_100000_repo_load.dat",
                "c_uniform_500000_repo_load.dat",
                "c_uniform_1000000_repo_load.dat",
                "c_uniform_5000000_repo_load.dat",
                "c_uniform_10000000_repo_load.dat"})
        String workloadFile;

        String[] toInsert;

        Object holder;

        @Setup
        public void setup() throws IOException {
            toInsert = loadInArray(workloadFile);
            supplier = supplier(mapType);
            Assertions.assertEquals(0, supplier.get().size());
            holder = new Object();
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int insert(Blackhole bh, LoadData d) {
        Map<String, Object> m = d.supplier.get();
        for (int i = 0; i < d.toInsert.length; i++) {
            bh.consume(m.put(d.toInsert[i], d.holder));
        }
        return m.size();
    }
}
