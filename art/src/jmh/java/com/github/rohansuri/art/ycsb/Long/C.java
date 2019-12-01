package com.github.rohansuri.art.ycsb.Long;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
    workload C (100% lookup)
 */
public class C {

    @State(Scope.Benchmark)
    public static class CData extends Data {

        @Param({"c_uniform_1000_randint_txn.dat",
                "c_uniform_5000_randint_txn.dat",
                "c_uniform_10000_randint_txn.dat",
                "c_uniform_50000_randint_txn.dat",
                "c_uniform_100000_randint_txn.dat",
                "c_uniform_500000_randint_txn.dat",
                "c_uniform_1000000_randint_txn.dat",
                "c_uniform_5000000_randint_txn.dat",
                "c_uniform_10000000_randint_txn.dat",
                "c_uniform_50000000_randint_txn.dat"})
        String workloadFile;

        // workload C
        Long[] toLookup;

        @Setup
        public void setup() throws IOException {
            super.loadInMap(workloadFile);

            // prepare lookup operations for workload C
            List<String> s = IOUtils
                    .readLines(new FileInputStream(workloadDirectory + workloadFile), StandardCharsets.US_ASCII);
            Assertions.assertTrue(s.stream().allMatch(line -> line.startsWith("READ")));
            List<Long> i = s.stream().map(line -> line.substring(line.indexOf(" ") + 1))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            toLookup = i.toArray(Long[]::new);
        }

    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int lookup(Blackhole bh, CData d) {
        for (int i = 0; i < d.toLookup.length; i++) {
            bh.consume(d.m.get(d.toLookup[i]));
        }
        return d.m.size();
    }

}
