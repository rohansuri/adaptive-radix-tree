package com.github.rohansuri.art;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

// contains all stuff borrowed from TreeMap
// such methods/utilities should be taken out and made a library of their own
// so any implementation of NavigableMap can reuse it, while the implementation
// provides certain primitive methods (getEntry, successor, predecessor, etc)

class Values<K, V> extends AbstractCollection<V> {
	private final AdaptiveRadixTree<K, V> m;

	Values(AdaptiveRadixTree<K, V> m){
		this.m = m;
	}

	@Override
	public Iterator<V> iterator() {
		return m.valueIterator();
	}

	@Override
	public int size() {
		return m.size();
	}

	@Override
	public boolean contains(Object o) {
		return m.containsValue(o);
	}

	@Override
	public boolean remove(Object o) {
		Iterator<Map.Entry<K, V>> it = m.entrySet().iterator();
		while(it.hasNext()){
			if (AdaptiveRadixTree.valEquals(it.next().getValue(), o)){
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		m.clear();
	}

	// TODO: implement Spliterator
}

