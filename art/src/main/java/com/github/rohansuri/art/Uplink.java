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
    void copy(Uplink<K, V> uplink){
        from = uplink.from;
        // TODO: pool cursor instances IF this produces garbage?
        parent = uplink.parent == null ? null : new Cursor(uplink.parent); // snapshot cursor position
        grandParent = uplink.grandParent == null ? null : new Cursor(uplink.grandParent); // snapshot cursor position
    }

    void remove(){
        parent.remove();
        parent = null;
    }
}
