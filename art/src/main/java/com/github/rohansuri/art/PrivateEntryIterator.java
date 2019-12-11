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
	Uplink<K, V> next;
	final Path<K, V> path;

	PrivateEntryIterator(AdaptiveRadixTree<K, V> m, Path<K,V> first) {
		expectedModCount = m.getModCount();
		lastReturned = new LastReturned<>();
		this.path = first;
		next = first == null ? null : first.uplink();
		this.m = m;
	}

	public final boolean hasNext() {
		return next != null;
	}

	final LeafNode<K, V> nextEntry() {
		Uplink<K, V> e = next;
		if (e == null)
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		lastReturned.set(e, path);
		next = path.successor();
		return lastReturned.uplink.from;
	}

	final LeafNode<K, V> prevEntry() {
		Uplink<K, V> e = next;
		if (e == null)
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		lastReturned.set(e, path);
		next = path.predecessor();
		return lastReturned.uplink.from;
	}

	public void remove() {
		if (!lastReturned.valid())
			throw new IllegalStateException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		if(!IteratorUtils.shouldInvalidateNext(lastReturned, path)){
			// safe to call throw away delete
			m.deleteEntryUsingThrowAwayUplink(lastReturned.uplink);
		} else {
			Uplink<K, V> uplink = IteratorUtils.deleteEntryAndResetNext(m, lastReturned, path,true);
			if(uplink == null){
				next.parent.seekBack();
			} else {
				next = uplink;
			}
		}
		lastReturned.reset();
		expectedModCount = m.getModCount();
	}

}
