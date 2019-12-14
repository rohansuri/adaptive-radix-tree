package com.github.rohansuri.art;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Base class for AdaptiveRadixTree Iterators
 * note: taken from TreeMap
 */
abstract class PrivateEntryIterator<K, V, T> implements Iterator<T> {
	private final AdaptiveRadixTree<K, V> m;
	private int expectedModCount;
	final LastReturned<K, V> lastReturned;
	final Path<K, V> path;

	PrivateEntryIterator(AdaptiveRadixTree<K, V> m, Path<K,V> first) {
		expectedModCount = m.getModCount();
		lastReturned = new LastReturned<>();
		this.path = first == null ? new Path<>() : first;
		this.m = m;
	}

	public final boolean hasNext() {
		return path.to != null;
	}

	final LeafNode<K, V> nextEntry() {
		if (!hasNext())
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		lastReturned.set(path);
		path.successor();
		return lastReturned.uplink.from;
	}

	final LeafNode<K, V> prevEntry() {
		if (!hasNext())
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		lastReturned.set(path);
		path.predecessor();
		return lastReturned.uplink.from;
	}

	@Override
	public void remove() {
		if (!lastReturned.valid())
			throw new IllegalStateException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		if(!IteratorUtils.shouldInvalidateNext(lastReturned, path)){
			// safe to call throw away delete
			m.deleteEntryUsingThrowAwayUplink(lastReturned.uplink);
		} else {
			IteratorUtils.deleteEntryAndResetNext(m, lastReturned, path,true);
		}
		lastReturned.reset();
		expectedModCount = m.getModCount();
	}

	final void removeDescending() {
		if (!lastReturned.valid())
			throw new IllegalStateException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		if(!IteratorUtils.shouldInvalidateNext(lastReturned, path)){
			// safe to call throw away delete
			m.deleteEntryUsingThrowAwayUplink(lastReturned.uplink);
		} else {
			IteratorUtils.deleteEntryAndResetNext(m, lastReturned, path,false);
		}
		lastReturned.reset();
		expectedModCount = m.getModCount();
	}

}
