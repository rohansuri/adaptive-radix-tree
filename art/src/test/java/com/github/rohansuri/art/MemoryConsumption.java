package com.github.rohansuri.art;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
//import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.MutableGraph;
import org.openjdk.jol.info.GraphLayout;
import static guru.nidi.graphviz.model.Factory.*;

public class MemoryConsumption {

    final static String workloadDirectory = "/home/rohansuri/index-microbench/workloads/";

    private static final String[] workloads = new String[]{
            "e_uniform_100000_randint_load.dat"
    };

    public static void main(String[] args) {
        for(String w : workloads) {
            calc(w);
        }
    }

    private static void graphTreeMap(TreeMap<Long, Object> t){
        MutableGraph g = mutGraph("TreeMap-e-100000").setDirected(true);
        //.add(mutNode("a").add(Color.RED).addLink(mutNode("b")));

        try {
            Graphviz.fromGraph(g).height(100).render(Format.PNG).toFile(new File("example/ex1.png"));
        }
        catch(IOException e){
            throw new RuntimeException(e);
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
                //graphTreeMap(t);
                // System.out.println("TreeMap size:" + GraphLayout.parseInstance(t).totalSize());
                // System.out.println("For workload w " + w + ", Radix size:" + GraphLayout
                   //     .parseInstance(art).totalSize());
                AdaptiveRadixTree.Stats s = art.stats();
                System.out.println("stats: " + s);
                System.out.println("average fill size: " + s.averageFillSize());
                t.height();

            }
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
