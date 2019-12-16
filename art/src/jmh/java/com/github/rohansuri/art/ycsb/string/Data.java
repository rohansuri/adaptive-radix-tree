package com.github.rohansuri.art.ycsb.string;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
public abstract class Data {
    final static String workloadDirectory = "/Users/rohansuri/index-microbench/workloads/";

    @Param
    MapType mapType;

    NavigableMap<String, Object> m;

    public enum MapType {
        ART,
        TREE_MAP
    }

    static Supplier<NavigableMap<String, Object>> supplier(MapType mapType) {
        switch (mapType) {
            case ART:
                return () -> new AdaptiveRadixTree<>(BinaryComparables.forString());
            case TREE_MAP:
                return () -> new TreeMap<>();
            default:
                throw new AssertionError();
        }
    }

    String[] loadInArray(String workloadFile) throws IOException {
        String loadFile = workloadFile.endsWith("load.dat") ? workloadFile : workloadFile.replace("txn.dat", "load.dat");

        List<String> s = IOUtils
                .readLines(new FileInputStream(workloadDirectory + loadFile), StandardCharsets.US_ASCII);
        Assertions.assertTrue(s.stream().allMatch(line -> line.startsWith("INSERT")));
        List<String> i = s.stream().map(line -> line.substring(line.indexOf(" ") + 1))
                .collect(Collectors.toList());
        return i.toArray(String[]::new);
    }

    private void loadFromArray(String[] i){
        Object o = new Object();
        m = supplier(mapType).get();
        for (String l : i) {
            m.put(l, o);
        }
    }

    void loadInMap(String workloadFile) throws IOException {
        String[] i = loadInArray(workloadFile);
        loadFromArray(i);
    }
}
