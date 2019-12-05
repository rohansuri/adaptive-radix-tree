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

    // initial cursor from leaf, leaf must exist
    Cursor(InnerNode node){
        assert node.hasLeaf();
        this.node = node;
        cursor = LEAF;
    }

    Node next(){
        if (cursor == node.child.length - 1) { // end of child array
            return null;
        }
        int ret = cursor++;
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
