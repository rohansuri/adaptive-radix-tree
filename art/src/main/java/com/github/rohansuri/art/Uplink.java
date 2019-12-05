package com.github.rohansuri.art;

class Uplink {
    LeafNode from;
    Cursor parent;
    Cursor grandParent;

    // better name?
    void moveDown(Cursor newParent){
        grandParent = parent; // new grand parent = old parent
        parent = newParent; // new parent = given parent
    }
}
