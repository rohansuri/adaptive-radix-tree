package com.github.rohansuri.art;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*
	These are internal contracts/interfaces
 	They've been written with only what they're used for internally
 	For example InnerNode#remove could have returned a false indicative of a failed remove
 	due to partialKey entry not actually existing, but the return value is of no use in code till now
 	and is sure to be called from places where it'll surely exist.
 	since they're internal, we could change them later if a better contract makes more sense.

	The impls have assert conditions all around to make sure the methods are called being in the right
	state. For example you should not call shrink() if the Node is not ready to shrink, etc.
	Or for example when calling last() on Node16 or higher, we're sure we'll have at least
	X amount of children hence safe to return child[noOfChildren-1], without worrying about bounds.

 */
abstract class InnerNode extends Node {

    static final int PESSIMISTIC_PATH_COMPRESSION_LIMIT = 8;

    // max limit of 8 bytes (Pessimistic)
    final byte[] prefixKeys;

    // Optimistic
    int prefixLen; // 4 bytes

    // TODO: we could save space by making this a byte and returning
    // Byte.toUnsignedInt wherever comparison with it is done.
    short noOfChildren;

    final Node[] child;

    InnerNode(int size) {
        prefixKeys = new byte[PESSIMISTIC_PATH_COMPRESSION_LIMIT];
        child = new Node[size + 1];
    }

    // copy ctor. called when growing/shrinking
    InnerNode(InnerNode node, int size) {
        child = new Node[size + 1];
        // copy header
        this.noOfChildren = node.noOfChildren;
        this.prefixLen = node.prefixLen;
        this.prefixKeys = node.prefixKeys;

        // copy leaf
        child[size] = node.getLeaf();
    }

    public void setLeaf(LeafNode<?, ?> leaf) {
        child[child.length - 1] = leaf;
    }

    // no-op if no leaf
    public final void removeLeaf() {
        child[child.length - 1] = null;
    }

    public boolean hasLeaf() {
        return child[child.length - 1] != null;
    }

    public LeafNode<?, ?> getLeaf() {
        return (LeafNode<?, ?>) child[child.length - 1];
    }

    public final Cursor cursorIfLeaf(){
        return hasLeaf() ? Cursor.first(this) : null;
    }

    @Override
    public final Cursor rear(){
        return Cursor.last(this);
    }

    public final Cursor frontNoLeaf(){
        return Cursor.firstNonLeaf(this);
    }

    @Override
    public Node firstOrLeaf() {
        if (hasLeaf()) {
            return getLeaf();
        }
        return first();
    }

    Node[] getChild() {
        return child;
    }

    /**
     * @return no of children this Node has
     */
    public short size() {
        return noOfChildren;
    }

    /**
     * @param partialKey search if this node has an entry for given partialKey
     * @return if it does, then return the following child pointer.
     * Returns null if there is no corresponding entry.
     */
    abstract Node findChild(byte partialKey);

    abstract Cursor cursor(byte partialKey);

    abstract Cursor ceilCursor(byte partialKey);


    abstract Cursor floorCursor(byte partialKey);

    /**
     * Note: caller needs to check if {@link InnerNode} {@link #isFull()} before calling this.
     * If it is full then call {@link #grow()} followed by {@link #addChild(byte, Node)} on the new node.
     *
     * @param partialKey partialKey to be mapped
     * @param child      the child node to be added
     */
    abstract void addChild(byte partialKey, Node child);

    /**
     * @param partialKey for which the child pointer mapping is to be updated
     * @param newChild   the new mapping to be added for given partialKey
     */
    abstract void replace(byte partialKey, Node newChild);

    /**
     * @param partialKey for which the child pointer mapping is to be removed
     */
    abstract void removeChild(byte partialKey);

    /**
     * creates and returns the next larger node type with the same mappings as this node
     *
     * @return a new node with the same mappings
     */
    abstract InnerNode grow();

    abstract boolean shouldShrink();

    /**
     * creates and returns the a smaller node type with the same mappings as this node
     *
     * @return a smaller node with the same mappings
     */
    abstract InnerNode shrink();

    // TODO: take in cursor and make cursor's inner node non final to refer to newly created inner node
    abstract Cursor shrinkAndGetCursor(int cursor);

    /**
     * @return true if Node has reached it's capacity
     */
    abstract boolean isFull();


    abstract void remove(int index);
}
