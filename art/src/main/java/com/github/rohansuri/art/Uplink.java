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

    boolean noParent(){
        return parent == null || parent.node == null;
    }

    boolean noGrandParent(){
        return grandParent == null || grandParent.node == null;
    }
}
