package com.github.rohansuri.art;

// TODO: assume cursor is either used totally for going next or totally for going previous, hence simplifying checks?

// different from iterator because allows to inspect current position multiple times
// without moving the cursor position.
// also the same instance supports going forward, backward any number of times even after reaching the boundaries.

/*
        possible positions of cursor:
            -2 surely leaf exists and surely previous() was called over this cursor
            -1 on leaf if it exists else starting boundary
            0 to child.length-2 on some valid child
            child.length - 1 end boundary
*/
class Cursor {
    final InnerNode node;
    private int cursor;
    private static final int LEAF = -1;

    // copy ctor
    Cursor(Cursor c){
        node = c.node;
        cursor = c.cursor;
    }

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
            return c;
        }
        c.cursor = node instanceof Node48 ? Node48.KEY_INDEX_SIZE : node.child.length - 1;
        c.previous();
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
        // TODO: safely assume cursor will always be >=0 ? internal constraint?
        if(cursor < LEAF){
            return null;
        }
        if(cursor == LEAF){
            // non-null if leaf exists else null
            return node.child[node.child.length-1];
        }
        if(node instanceof Node48){
            Node48 node48 = (Node48)node;
            byte index = node48.getKeyIndex()[cursor];
            return node.child[index];
        }
        return node.child[cursor];
    }

    // moves cursor position forward and returns the next child at the new position.
    // after reaching boundary, all next() calls return null.
    Node next(){
        // left extreme could either be at -1 or -2
        // -2 means we have a leaf and hence return that
        // TODO: remove this check if we assume Cursors are totally going forward or totally going backward
        if(cursor + 1 == LEAF) {
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
            while (cursor+1 < Node48.KEY_INDEX_SIZE) {
                cursor++;
                byte index = keyIndex[cursor];
                if(index != Node48.ABSENT){
                    return node.child[index];
                }
            }
        } else { // Node256
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

    // moves cursor position backward and returns the next child at the new position.
    // after reaching boundary, all previous() calls return null.
    Node previous(){
        if(cursor-1 == LEAF){
            cursor--;
            return node.getLeaf();
        }
        // either we're already at the end or we have a leaf and hence we can go further beyond
        if(cursor == LEAF){
            if(node.hasLeaf()){
                cursor--;
            }
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
        } else { // Node256
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

    void remove(boolean forward){
        if(forward){
            removeAndNext();
        } else {
            removeAndPrevious();
        }
    }

    private void removeAndNext(){
        if(cursor == LEAF){
            node.removeLeaf();
            next();
        } else {
            node.remove(cursor);
            // nothing to do for Node4, Node16
            // since the array left shift on delete will make current cursor point to next child
            if(node instanceof Node48 || node instanceof Node256){
                next();
            }
        }
    }

    // to be used by only throw away cursors
    // after this, cursor next, prev are not valid.
    void remove(){
        if(cursor == LEAF){
            node.removeLeaf();
        } else {
            node.remove(cursor);
        }
    }

    // no-op if reached beginning
    private void removeAndPrevious(){
        if(cursor < LEAF){
            return;
        }
        if(cursor == LEAF){
            node.removeLeaf(); // no-op if no leaf
            previous();
        } else {
            node.remove(cursor);
            if(node instanceof Node48 || node instanceof Node256){
                previous();
            } else {
                cursor--;
            }
        }
    }

    // QUES: can cursor be on leaf?
    void replace(Node replaceWith){
        assert cursor >= 0 && cursor < ((node instanceof Node4 || node instanceof Node16)? node.noOfChildren : node.child.length-1);
        if(node instanceof Node48){
            Node48 node48 = (Node48)node;
            byte index = node48.getKeyIndex()[cursor];
            node.child[index] = replaceWith;
        } else {
            node.child[cursor] = replaceWith;
        }
    }

    // does current cursor position correspond to given partialKey?
    // CLEANUP: make floorCursor, ceilCursor return this as metadata along with cursor
    // so that we don't need to check again?
    // DEVNOTE: internal constraint on calling this is that cursor will never be on
    // leaf or outside boundaries
    boolean isOn(byte partialKey){
        assert cursor >= 0;
        assert cursor < ((node instanceof Node4 || node instanceof Node16)? node.noOfChildren :
                         (node instanceof Node48 ? Node48.KEY_INDEX_SIZE : node.child.length-1)) : cursor;
        if(node instanceof Node4){
            Node4 node4 = (Node4)node;
            return BinaryComparableUtils.unsigned(partialKey) == node4.getKeys()[cursor];
        } else if(node instanceof Node16){
            Node16 node16 = (Node16)node;
            return BinaryComparableUtils.unsigned(partialKey) == node16.getKeys()[cursor];
        }
        return cursor == Byte.toUnsignedInt(partialKey);
    }

    void seekBack(){
        assert (node instanceof Node4 || node instanceof Node16) && node.noOfChildren >= 1;
        cursor--;
    }

    Cursor shrink(){
        return node.shrinkAndGetCursor(cursor);
    }

    byte partialKey(){
        if(node instanceof Node4){
            return BinaryComparableUtils.signed(((Node4)node).getKeys()[cursor]);
        } else if(node instanceof Node16){
            return BinaryComparableUtils.signed(((Node16)node).getKeys()[cursor]);
        } else {
            return (byte) cursor;
        }
    }
}
