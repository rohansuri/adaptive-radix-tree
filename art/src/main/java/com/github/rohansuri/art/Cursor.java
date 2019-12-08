package com.github.rohansuri.art;

// different from iterator because allows to inspect current position multiple times
// without moving the cursor position.
// also the same instance supports going forward, backward any number of times even after reaching the boundaries.
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
            c.cursor = node.child.length - 1;
            c.previous();
        }
        return c;
    }

    static Cursor firstNonLeaf(InnerNode node){
        Cursor c = new Cursor(node);
        c.cursor = LEAF;
        c.next();
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

    Node current(){
        if(node instanceof Node48){
            Node48 node48 = (Node48)node;
            byte index = node48.getKeyIndex()[cursor];
            assert index >= 0 && index <= 47;
            return node.child[index];
        }
        return node.child[cursor];
    }

    // moves cursor position forward and returns the next child at the new position
    Node next(){
        // left extremer could either be at -1 or -2
        // -1 means no leaf and hence next is the first child
        // -2 means we have a leaf and hence return that
        if(cursor + 1 == LEAF) {
            // leaf surely exists otherwise cursor wouldn't have reached beyond leaf
            cursor++;
            return node.getLeaf();
        }
        if(node instanceof Node4 || node instanceof Node16){
            if(cursor+1 < node.noOfChildren){
                return node.child[++cursor];
            }
        } else if (node instanceof Node48) {
            Node48 node48 = (Node48) node;
            byte[] keyIndex = node48.getKeyIndex();
            while (cursor+1 < Node48.NODE_SIZE) {
                cursor++;
                byte index = keyIndex[cursor];
                if(index != Node48.ABSENT){
                    return node.child[index];
                }
            }
        } else if (node instanceof Node256) {
            while (cursor+1 < Node256.NODE_SIZE) {
                cursor++;
                Node child = node.child[cursor];
                if(child != null){
                    return child;
                }
            }
        }
        return null;
    }

    // moves cursor position backward and returns the next child at the new position
    Node previous(){
        if(cursor-1 == LEAF){
            cursor--;
            return node.getLeaf();
        }
        if(cursor == LEAF){ // either we're already at the end or we have a leaf and hence we can go further beyond
            if(!node.hasLeaf()){
                return null;
            }
            cursor--;
            return null;
        }

        if(node instanceof Node4 || node instanceof Node16){
            if(cursor-1 >= 0){
                return node.child[--cursor];
            }
        } else if (node instanceof Node48) {
            Node48 node48 = (Node48) node;
            byte[] keyIndex = node48.getKeyIndex();
            while (cursor-1 >= 0) {
                cursor--;
                byte index = keyIndex[cursor];
                if(index != Node48.ABSENT){
                    return node.child[index];
                }
            }
        } else if (node instanceof Node256) {
            while (cursor-1 >= 0) {
                cursor--;
                Node child = node.child[cursor];
                if(child != null){
                    return child;
                }
            }
        }
        return null;
    }

    void removeAndNext(){
        // TODO
        node.remove(cursor);
    }

    void removeAndPrevious(){
        // TODO
    }

    void replace(Node replaceWith){
        if(node instanceof Node48){
            Node48 node48 = (Node48)node;
            byte index = node48.getKeyIndex()[cursor];
            assert index >= 0 && index <= 47;
            node.child[index] = replaceWith;
        } else {
            node.child[cursor] = replaceWith;
        }
    }

    // does current cursor position correspond to given partialKey?
    // CLEANUP: make floorCursor, ceilCursor return this as metadata along with cursor
    // so that we don't need to check again?
    boolean isOn(byte partialKey){
        if(node instanceof Node48 || node instanceof Node256){
            return cursor == Byte.toUnsignedInt(partialKey);
        } else if(node instanceof Node4){
            Node4 node4 = (Node4)node;
            return BinaryComparableUtils.unsigned(partialKey) == node4.getKeys()[cursor];
        } else {
            Node16 node16 = (Node16)node;
            return BinaryComparableUtils.unsigned(partialKey) == node16.getKeys()[cursor];
        }
    }
}
