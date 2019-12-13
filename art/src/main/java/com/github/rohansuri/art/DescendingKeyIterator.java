package com.github.rohansuri.art;

final class DescendingKeyIterator<K, V> extends PrivateEntryIterator<K, V, K> {
	DescendingKeyIterator(AdaptiveRadixTree<K, V> m, Path<K,V> last) {
		super(m, last);
	}
	@Override
	public K next() {
		return prevEntry().getKey();
	}


	@Override
	public void remove(){
		removeDescending();
	}

}
