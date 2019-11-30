package com.github.rohansuri.art.ycsb.Long;

import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class E {
    @State(Scope.Benchmark)
    public static class EData extends Data {

        @Param({"e_uniform_1000_randint_txn.dat",
                "e_uniform_5000_randint_txn.dat",
                "e_uniform_10000_randint_txn.dat",
                "e_uniform_50000_randint_txn.dat",
                "e_uniform_100000_randint_txn.dat",
                "e_uniform_500000_randint_txn.dat",
                "e_uniform_1000000_randint_txn.dat",
                "e_uniform_5000000_randint_txn.dat",
                "e_uniform_10000000_randint_txn.dat",
                "e_uniform_50000000_randint_txn.dat"})
        String workloadFile;

        boolean[] operation; // what is the ith operation? true == SCAN, false == INSERT

        Long[] scanStart;
        Integer[] scanRange;

        Long[] toInsert;

        Object holder;

        @Setup
        public void setup() throws IOException {
            super.loadFor(workloadFile);
            holder = new Object();
            List<String> s = IOUtils
                    .readLines(new FileInputStream(workloadDirectory + workloadFile), StandardCharsets.US_ASCII);
            int lastInsert = 0;
            int lastScan = 0;
            int i = 0;
            operation = new boolean[s.size()];
            scanStart = new Long[s.size()];
            scanRange = new Integer[s.size()];
            toInsert = new Long[s.size()];
            for (String op : s) {
                if (op.startsWith("SCAN")) {
                    operation[i] = true;
                    int scan = op.indexOf(" ");
                    int range = op.lastIndexOf(" ");
                    scanStart[lastScan] = Long.parseLong(op.substring(scan + 1, range));
                    scanRange[lastScan] = Integer.parseInt(op.substring(range + 1));
                    lastScan++;
                } else if (op.startsWith("INSERT")) {
                    operation[i] = false;
                    String toInsert = op.substring(op.indexOf(" ") + 1);
                    this.toInsert[lastInsert++] = Long.parseLong(toInsert);
                } else {
                    throw new RuntimeException("expected either SCAN or INSERT operations in workload E");
                }
                i++;
            }
        }

    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int rangeScanAndInsert(Blackhole bh, EData d) {
        int lastInsert = 0;
        int lastScan = 0;
        for (int i = 0; i < d.operation.length; i++) {
            if (d.operation[i]) {
                NavigableMap<Long, Object> tailMap = d.m.tailMap(d.scanStart[lastScan], true);
                int rangeLimit = Math.min(tailMap.size(), d.scanRange[lastScan]);
                Iterator<Map.Entry<Long, Object>> tail = tailMap.entrySet().iterator();
                for (int j = 0; j < rangeLimit; j++) {
                    bh.consume(tail.next());
                }
                lastScan++;
            } else {
                bh.consume(d.m.put(d.toInsert[lastInsert++], d.holder));
            }
        }
        return d.m.size();
    }
}
