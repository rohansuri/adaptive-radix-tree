package com.github.rohansuri.art;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.openjdk.jol.info.GraphLayout;

public class MemoryConsumption {

    final static String workloadDirectory = "/Users/rohansuri/index-microbench/workloads/";

    private static final String[] workloads = new String[]{
            "c_uniform_10000000_randint_load.dat"
    };

    public static void main(String args[]) {
        for(String w : workloads) {
            calc(w);
        }
    }

    private static void calc(String w){
        try {
            File f = new File(workloadDirectory + w);
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;
            Object o = new Object();
            if(!w.contains("randint")){
                TreeMap<String, Object> t = new TreeMap<>();
                AdaptiveRadixTree<String, Object> art = new AdaptiveRadixTree<>(BinaryComparables.forString(StandardCharsets.US_ASCII));
                while((line = r.readLine())!=null){
                    String toInsert = line.substring(line.indexOf(" ") + 1);
                   t.put(toInsert, o);
                    art.put(toInsert, o);
                }
                System.out.println("TreeMap size:" + GraphLayout.parseInstance(t).totalSize());
                System.out.println("For workload w " + w + ", Radix size:" + GraphLayout
                        .parseInstance(art).totalSize());
            } else {
                TreeMap<Long, Object> t = new TreeMap<>();
                AdaptiveRadixTree<Long, Object> art = new AdaptiveRadixTree<>(BinaryComparables.forLong());
                while((line = r.readLine())!=null){
                    Long toInsert = Long.parseLong(line.substring(line.indexOf(" ") + 1));
                  t.put(toInsert, o);
                    art.put(toInsert, o);
                }
                System.out.println("TreeMap size:" + GraphLayout.parseInstance(t).totalSize());
                System.out.println("For workload w " + w + ", Radix size:" + GraphLayout
                        .parseInstance(art).totalSize());
            }
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
