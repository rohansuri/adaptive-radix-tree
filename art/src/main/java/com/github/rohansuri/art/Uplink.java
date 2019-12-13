package com.github.rohansuri.art;

class Uplink<K, V> {
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
    final void copy(Uplink<K, V> uplink){
        from = uplink.from;
        Cursor.copy(uplink.parent, parent);
        Cursor.copy(uplink.grandParent, grandParent);
    }

    boolean noParent(){
        return parent == null || parent.node == null;
    }

    boolean noGrandParent(){
        return grandParent == null || grandParent.node == null;
    }
}
