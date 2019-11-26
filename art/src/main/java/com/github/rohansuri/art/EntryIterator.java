package com.github.rohansuri.art;

import java.util.Map;

final class EntryIterator<K, V> extends PrivateEntryIterator<K, V, Map.Entry<K, V>> {
	EntryIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> first) {
		super(m, first);
	}
	@Override
	public Map.Entry<K,V> next() {
		return nextEntry();
	}
}
