package art;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

// contains all stuff borrowed from TreeMap
// such methods/utilities should be taken out and made a library of their own
// so any implementation of NavigableMap can reuse it, while the implementation
// provides certain primitive methods

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

	/*public Spliterator<Map.Entry<K,V>> spliterator() {
		return new TreeMap.EntrySpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
	}*/
}

/**
 * Base class for AdaptiveRadixTree Iterators
 * note: taken from TreeMap
 */
abstract class PrivateEntryIterator<K, V, T> implements Iterator<T> {
	private final AdaptiveRadixTree<K, V> m;
	private LeafNode<K,V> next;
	private LeafNode<K, V> lastReturned;
	private int expectedModCount;

	PrivateEntryIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> first) {
		expectedModCount = m.getModCount();
		lastReturned = null;
		next = first;
		this.m = m;
	}

	public final boolean hasNext() {
		return next != null;
	}

	final LeafNode<K,V> nextEntry() {
		LeafNode<K,V> e = next;
		if (e == null)
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		next = AdaptiveRadixTree.successor(e);
		lastReturned = e;
		return e;
	}

	final LeafNode<K,V> prevEntry() {
		LeafNode<K,V> e = next;
		if (e == null)
			throw new NoSuchElementException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		next = AdaptiveRadixTree.predecessor(e);
		lastReturned = e;
		return e;
	}

	public void remove() {
		if (lastReturned == null)
			throw new IllegalStateException();
		if (m.getModCount() != expectedModCount)
			throw new ConcurrentModificationException();
		m.deleteEntry(lastReturned);
		expectedModCount = m.getModCount();
		lastReturned = null;
	}
}

final class EntryIterator<K, V> extends PrivateEntryIterator<K, V, Map.Entry<K, V>> {
	EntryIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> first) {
		super(m, first);
	}
	@Override
	public Map.Entry<K,V> next() {
		return nextEntry();
	}
}

final class ValueIterator<K, V> extends PrivateEntryIterator<K, V, V> {
	ValueIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> first) {
		super(m, first);
	}
	@Override
	public V next() {
		return nextEntry().getValue();
	}
}

final class KeyIterator<K, V> extends PrivateEntryIterator<K, V, K> {
	KeyIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> first) {
		super(m, first);
	}
	@Override
	public K next() {
		return nextEntry().getKey();
	}
}

final class DescendingKeyIterator<K, V> extends PrivateEntryIterator<K, V, K> {
	DescendingKeyIterator(AdaptiveRadixTree<K, V> m, LeafNode<K,V> last) {
		super(m, last);
	}
	@Override
	public K next() {
		return prevEntry().getKey();
	}
}