package com.github.rohansuri.art;

abstract class Node {
    /**
     * @return child pointer for the smallest partialKey (non prefix ending) stored in this Node.
     * Returns null if this node has no children.
     */
    abstract Node first();

    abstract Node firstOrLeaf();

    /**
     * @return returns cursor to the first child (leaf or first child) of this node.
     */
    abstract Cursor front();

    /**
     * @return returns cursor to the last child of this node.
     */
    abstract Cursor rear();

    /**
     * @return child pointer for the largest partialKey stored in this Node.
     * Returns null if this node has no children.
     */
    abstract Node last();
}
