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
            super.loadInMap(workloadFile);
            holder = new Object();
            List<String> s = IOUtils
                    .readLines(new FileInputStream(workloadDirectory + workloadFile), StandardCharsets.US_ASCII);
            int i = 0;
            operation = new boolean[s.size()];
            List<Long> scanStartList = new ArrayList<>();
            List<Integer> scanRangeList = new ArrayList<>();
            List<Long> toInsertList = new ArrayList<>();
            for (String op : s) {
                if (op.startsWith("SCAN")) {
                    operation[i] = true;
                    int scan = op.indexOf(" ");
                    int range = op.lastIndexOf(" ");
                    scanStartList.add(Long.parseLong(op.substring(scan + 1, range)));
                    scanRangeList.add(Integer.parseInt(op.substring(range + 1)));
                } else if (op.startsWith("INSERT")) {
                    operation[i] = false;
                    String toInsert = op.substring(op.indexOf(" ") + 1);
                    toInsertList.add(Long.parseLong(toInsert));
                } else {
                    throw new RuntimeException("expected either SCAN or INSERT operations in workload E");
                }
                i++;
            }
            scanStart = scanStartList.toArray(Long[]::new);
            scanRange = scanRangeList.toArray(Integer[]::new);
            toInsert = toInsertList.toArray(Long[]::new);
        }

    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int rangeScanAndInsert(Blackhole bh, EData d) {
        int lastInsert = 0;
        int lastScan = 0;
        for (int i = 0; i < d.operation.length; i++) {
            if (d.operation[i]) { // scan
                NavigableMap<Long, Object> tailMap = d.m.tailMap(d.scanStart[lastScan], true);
                // creation of iterator results in one getCeilingEntry call
                Iterator<Map.Entry<Long, Object>> tail = tailMap.entrySet().iterator();
                for (int j = 0; j < d.scanRange[lastScan]-1 && tail.hasNext() ; j++) {
                    // all next calls, call successors (which calls first on Node)
                    bh.consume(tail.next());
                }
                lastScan++;
            } else { // insert
                bh.consume(d.m.put(d.toInsert[lastInsert++], d.holder));
            }
        }
        return d.m.size();
    }
}
