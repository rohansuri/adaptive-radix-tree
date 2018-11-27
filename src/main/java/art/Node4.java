package art;

public class Node4 {
    private final Node child[] = new Node[4];
    private final byte[] keys = new byte[4];
}

/*
    any optimisations possible for these Node structures?
    get rid of bounds checking etc? (off-heap?)
    megamorphic call sites?
    combine into single node?
*/
