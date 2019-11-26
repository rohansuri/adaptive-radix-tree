package com.github.rohansuri.art;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
	private final AdaptiveRadixTree<K, V> m;

	EntrySet(AdaptiveRadixTree<K, V> m) {
		this.m = m;
	}

	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		return m.entryIterator();
	}

	@Override
	public boolean contains(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
		Object value = entry.getValue();
		LeafNode<K, V> p = m.getEntry(entry.getKey());
		return p != null && AdaptiveRadixTree.valEquals(p.getValue(), value);
	}

	@Override
	public boolean remove(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
		Object value = entry.getValue();
		LeafNode<K, V> p = m.getEntry(entry.getKey());
		if (p != null && AdaptiveRadixTree.valEquals(p.getValue(), value)) {
			m.deleteEntry(p);
			return true;
		}
		return false;
	}

	@Override
	public int size() {
		return m.size();
	}

	@Override
	public void clear() {
		m.clear();
	}

	// TODO: implement Spliterator
}
