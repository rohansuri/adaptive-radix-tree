package com.github.rohansuri.art;

// internal use hence does not throw ConcurrentModificationException
// nor NoSuchElementException, nor implements Iterator
// static dispatch, rather than having next implemented over Node
class NodeIterator {

    private final InnerNode node;
    private int cursor;
    private static final int FROM_START = -1;

    NodeIterator(InnerNode node, int startFrom) {
        this.node = node;
        this.cursor = startFrom;
    }

    NodeIterator(InnerNode node){
        this.node = node;
        this.cursor = FROM_START;
    }

    // null means iteration over
    public Node next() {
        if(cursor == FROM_START){

        }
        if (cursor == node.child.length - 1) { // what about leaf node?
            return null;
        }
        int ret = cursor++;
        if (node instanceof Node48) {
            Node48 node48 = (Node48) node;
            byte[] keyIndex = node48.getKeyIndex();
            while (cursor < Node48.NODE_SIZE && keyIndex[cursor] == Node48.ABSENT) {
                cursor++;
            }
            int pos = node48.getKeyIndex()[ret];
            return node.child[pos];
        } else if (node instanceof Node256) {
            while (cursor < Node256.NODE_SIZE && node.child[cursor] == null) {
                cursor++;
            }
        }
        return node.child[ret];
    }

}
