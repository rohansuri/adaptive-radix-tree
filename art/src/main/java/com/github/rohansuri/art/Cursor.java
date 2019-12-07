package com.github.rohansuri.art;

class Cursor {
    final InnerNode node;
    private int cursor;
    private static final int LEAF = -1;

    // initial cursor must be valid
    Cursor(InnerNode node, int cursor){
        assert node instanceof Node48 ? ((Node48)node).getKeyIndex()[cursor] != Node48.ABSENT : node.child[cursor] != null;
        this.node = node;
        this.cursor = cursor;
    }

    // for use with static factory methods
    private Cursor(InnerNode node){
        this.node = node;
    }

    static Cursor last(InnerNode node){
        Cursor c = new Cursor(node);
        if(node instanceof Node4 || node instanceof Node16){
            c.cursor = node.noOfChildren-1;
        } else {
            // bad: relies on the fact that we have space for leaf at the end of child array
            // leaf position, just as placeholder for the previous call to land on the last valid child
            c.cursor = node.child.length - 1;
            c.previous();
        }
        return c;
    }

    // initialize cursor from leaf if exists
    // else from the first child position
    static Cursor first(InnerNode node){
        Cursor c = new Cursor(node);
        c.cursor = LEAF;
        if(!node.hasLeaf()){
            c.next();
        }
        return c;
    }

    private boolean reachedEnd(){
        if(node instanceof Node4 || node instanceof Node16){
            return cursor == node.noOfChildren;
        }
        return cursor == node.child.length - 1;  // end of child array
    }

    Node next(){
        if (reachedEnd()) {
            return null;
        }
        int ret = cursor++; // Node4, Node16
        if (node instanceof Node48) {
            Node48 node48 = (Node48) node;
            byte[] keyIndex = node48.getKeyIndex();
            while (cursor < Node48.NODE_SIZE && keyIndex[cursor] == Node48.ABSENT) {
                cursor++;
            }
            return ret == LEAF ? node.getLeaf() : node.child[keyIndex[ret]];
        }
        if (node instanceof Node256) {
            while (cursor < Node256.NODE_SIZE && node.child[cursor] == null) {
                cursor++;
            }
        }
        return ret == LEAF ? node.getLeaf() : node.child[ret];
    }

    private boolean reachedStart(){
        int end = node.hasLeaf() ? LEAF : 0;
        return cursor == end - 1;
    }

    Node previous(){
        if(reachedStart()){
            return null;
        }

        int ret = cursor--; // Node4, Node16

        if(ret == LEAF){
            return node.getLeaf();
        }

        if (node instanceof Node48) {
            Node48 node48 = (Node48) node;
            byte[] keyIndex = node48.getKeyIndex();
            while (cursor >= 0  && keyIndex[cursor] == Node48.ABSENT) {
                cursor--;
            }
            return node.child[keyIndex[ret]];
        }
        if (node instanceof Node256) {
            while (cursor >= 0 && node.child[cursor] == null) {
                cursor--;
            }
        }
        return node.child[ret];
    }

    void remove(){
        node.remove(cursor);
    }

    void replace(Node replaceWith){
        if(node instanceof Node48){
            Node48 node48 = (Node48)node;
            byte i = node48.getKeyIndex()[cursor];
            assert i >= 0 && i <= 47;
            node.child[i] = replaceWith;
        } else {
            node.child[cursor] = replaceWith;
        }
    }
}
