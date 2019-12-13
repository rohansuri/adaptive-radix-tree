package com.github.rohansuri.art;

import java.util.ArrayList;
import java.util.List;

class Path<K, V> {
    // TODO: determine a good heuristic for initial array size? (max depth ever reached in inserts?)
    final List<Cursor> path = new ArrayList<>();
    LeafNode<K, V> to;
    private final Uplink<K, V> uplink = new Uplink<>();

    private Cursor parent(){
        return !path.isEmpty() ? path.get(path.size()-1) : null;
    }

    private Cursor grandParent(){
        return path.size() >= 2 ? path.get(path.size()-2) : null;
    }

    Uplink<K, V> uplink(){
        uplink.from = to;
        uplink.parent = parent();
        uplink.grandParent = grandParent();
        return uplink;
    }

    Uplink<K, V> successor(){
        while(!path.isEmpty()){
            Cursor parent = path.get(path.size()-1); // parent
            Node next = parent.next();
            if(next == null){ // this cursor ended, go up
                path.remove(path.size()-1);
            } else {
                return AdaptiveRadixTree.getFirstEntryWithUplink(next, this);
            }
        }
        to = null;
        return null;
    }

     Uplink<K, V> predecessor() {
         while(!path.isEmpty()){
             Cursor parent = path.get(path.size()-1); // parent
             Node prev = parent.previous();
             if(prev == null){ // this cursor ended, go up
                 path.remove(path.size()-1);
             } else {
                 return AdaptiveRadixTree.getLastEntryWithUplink(prev, this);
             }
         }
         to = null;
         return null;
    }
}
