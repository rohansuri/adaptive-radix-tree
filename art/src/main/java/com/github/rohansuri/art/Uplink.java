package com.github.rohansuri.art;

class Uplink {
    final Cursor[] path = new Cursor[2]; // parent, grand parent
    LeafNode from;

    // better name?
    void moveDown(Cursor parent){
        path[1] = path[0]; // new grand parent = old parent
        path[0] = parent; // new parent = given parent
    }
}
