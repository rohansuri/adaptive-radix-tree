package com.github.rohansuri.art;

import java.util.ArrayList;
import java.util.List;

class Path {
    // TODO: determine a good heuristic for initial array size? (max depth ever reached in inserts?)
    final List<Cursor> path = new ArrayList<>();
    LeafNode to;
}
