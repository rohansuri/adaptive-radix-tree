package com.github.rohansuri.art;

final class Uplink<K, V> {
    LeafNode<K, V> from;
    Cursor parent;
    Cursor grandParent;

    // better name?
    void moveDown(Cursor newParent){
        grandParent = parent; // new grand parent = old parent
        parent = newParent; // new parent = given parent
    }

    Uplink(){}

    // snapshot uplink (to snapshot cursor positions of parent, grand parent)

    static <K, V> void copy(Path<K, V> from, Uplink<K, V> into){
        into.from = from.to;
        Cursor.copy(from.parent(), into.parent);
        Cursor.copy(from.grandParent(), into.grandParent);
    }

    boolean noParent(){
        return parent == null || parent.node == null;
    }

    boolean noGrandParent(){
        return grandParent == null || grandParent.node == null;
    }
}
