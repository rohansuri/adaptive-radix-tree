package com.github.rohansuri.art;

/*
    used to save current leaf node reached
    before moving onto next (successor/predecessor)
    we copy parent, grandParent's current cursor positions as well
    because the successor/predecessor calls could change them.
    but we need it for remove calls.
*/
class LastReturned<K, V> {
    final Uplink<K, V> uplink;

    /*
        the index of the parent of the leafnode in uplink.from
        since path could be empty (in case of empty tree), pathIndex could be -1.
     */
    int pathIndex;

    LastReturned() {
        uplink = new Uplink<>();
        // reuse these cursor instances across all set calls
        uplink.parent = new Cursor();
        uplink.grandParent = new Cursor();
    }

    void set(Path<K, V> path) {
        path.copyInto(uplink);
        this.pathIndex = path.size() - 1;
    }

    /*
        after the last returned is removed
     */
    void reset() {
        this.uplink.from = null;
    }

    boolean valid() {
        return this.uplink.from != null;
        
    }
}