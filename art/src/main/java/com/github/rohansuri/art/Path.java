package com.github.rohansuri.art;

import java.util.Arrays;

final class Path<K, V> {
    // TODO: determine a good heuristic for initial array size? (max depth ever reached in inserts?)
    private Cursor[] path = new Cursor[10];
    private int size;
    LeafNode<K, V> to;
    private final Uplink<K, V> uplink = new Uplink<>();

    Uplink<K, V> uplink(){
        uplink.from = to;
        uplink.parent = size == 0 ? null : path[size-1];
        uplink.grandParent = size >= 2 ? path[size-2] : null;
        return uplink;
    }

    Uplink<K, V> successorAndUplink(){
        while(size > 0){
            Cursor parent = path[size-1]; // parent
            Node next = parent.next();
            if(next == null){ // this cursor ended, go up
                removeLast();
            } else {
                return AdaptiveRadixTree.getFirstEntryWithUplink(next, this);
            }
        }
        to = null;
        return null;
    }

    void successor(){
        while(size > 0){
            Cursor parent = path[size-1]; // parent
            Node next = parent.next();
            if(next == null){ // this cursor ended, go up
                removeLast();
            } else {
                AdaptiveRadixTree.getFirstEntry(next, this);
            }
        }
        to = null;
    }


     Uplink<K, V> predecessor() {
         while(size > 0){
             Cursor parent = path[size-1]; // parent
             Node prev = parent.previous();
             if(prev == null){ // this cursor ended, go up
                 removeLast();
             } else {
                 return AdaptiveRadixTree.getLastEntryWithUplink(prev, this);
             }
         }
         to = null;
         return null;
    }

    void addLast(Cursor c){
        if (size == path.length)
            path = Arrays.copyOf(path, size << 1);
        path[size] = c;
        size = size + 1;
    }

    int size(){
        return size;
    }

    Cursor get(int index){
        return path[index];
    }

    void set(int index, Cursor c){
        path[index] = c;
    }

    private void removeLast(){
        size--;
        path[size] = null;
    }

    void remove(int i){
        // remove (from ArrayList)
        final int newSize;
        if ((newSize = size - 1) > i)
            System.arraycopy(path, i + 1, path, i, newSize - i);
        path[size = newSize] = null;
    }
}
