package art;

public class Node48 {
    private final Node child[] = new Node[48];
    private final byte[] keyIndex = new byte[256]; // 256 + 48*8
}

/*
    other nodes:
    key = [a, b, c, d]
    I'd look up by binary searching for them

    but with 48 my key size has grown, so rather than binary searching
    (log 48 base 2 = 5.58.. about 6 comparisons)
    I index into an array which will tell me the position in the child pointer array
    since as my child pointers grow (tree becomes fat) I don't want to spend time in searching
    for the right "next-child" pointer
 */
