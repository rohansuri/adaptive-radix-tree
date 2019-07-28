package art;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;

// implementation simply relays/delegates calls to backing map's methods
final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
	private final NavigableMap<E, ?> m;

	KeySet(NavigableMap<E, ?> map) {
		m = map;
	}

	// this KeySet can only be created either on ART or on one of it's subMaps
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<E> iterator() {
		if (m instanceof AdaptiveRadixTree)

			return ((AdaptiveRadixTree<E, ?>) m).keyIterator();
		else
			return ((NavigableSubMap<E, ?>) m).keyIterator();
	}

	// this KeySet can only be created either on ART or on one of it's subMaps
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<E> descendingIterator() {
		if (m instanceof AdaptiveRadixTree)
			return ((AdaptiveRadixTree<E, ?>) m).descendingKeyIterator();
		else
			return ((NavigableSubMap<E, ?>) m).descendingKeyIterator();
	}

	@Override
	public int size() {
		return m.size();
	}

	@Override
	public boolean isEmpty() {
		return m.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return m.containsKey(o);
	}

	@Override
	public void clear() {
		m.clear();
	}

	@Override
	public E lower(E e) {
		return m.lowerKey(e);
	}

	@Override
	public E floor(E e) {
		return m.floorKey(e);
	}

	@Override
	public E ceiling(E e) {
		return m.ceilingKey(e);
	}

	@Override
	public E higher(E e) {
		return m.higherKey(e);
	}

	@Override
	public E first() {
		return m.firstKey();
	}

	@Override
	public E last() {
		return m.lastKey();
	}

	@Override
	public Comparator<? super E> comparator() {
		return m.comparator();
	}

	@Override
	public E pollFirst() {
		Map.Entry<E, ?> e = m.pollFirstEntry();
		return (e == null) ? null : e.getKey();
	}

	@Override
	public E pollLast() {
		Map.Entry<E, ?> e = m.pollLastEntry();
		return (e == null) ? null : e.getKey();
	}

	@Override
	public boolean remove(Object o) {
		int oldSize = size();
		m.remove(o);
		return size() != oldSize;
	}

	@Override
	public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
			E toElement, boolean toInclusive) {
		return new KeySet<>(m.subMap(fromElement, fromInclusive,
				toElement, toInclusive));
	}

	@Override
	public NavigableSet<E> headSet(E toElement, boolean inclusive) {
		return new KeySet<>(m.headMap(toElement, inclusive));
	}

	@Override
	public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
		return new KeySet<>(m.tailMap(fromElement, inclusive));
	}

	@Override
	public SortedSet<E> subSet(E fromElement, E toElement) {
		return subSet(fromElement, true, toElement, false);
	}

	@Override
	public SortedSet<E> headSet(E toElement) {
		return headSet(toElement, false);
	}

	@Override
	public SortedSet<E> tailSet(E fromElement) {
		return tailSet(fromElement, true);
	}

	@Override
	public NavigableSet<E> descendingSet() {
		return new KeySet<>(m.descendingMap());
	}

	/*
	public Spliterator<E> spliterator() {
			return keySpliteratorFor(m);
	}
	*/
}

// A NavigableMap that adds range checking (if passed in key is within lower and upper bound)
// for all the map methods and then relays the call
// into the backing map
abstract class NavigableSubMap<K, V> extends AbstractMap<K, V>
		implements NavigableMap<K, V> {

	final AdaptiveRadixTree<K, V> m;

	/**
	 * Endpoints are represented as triples (fromStart, lo,
	 * loInclusive) and (toEnd, hi, hiInclusive). If fromStart is
	 * true, then the low (absolute) bound is the start of the
	 * backing map, and the other values are ignored. Otherwise,
	 * if loInclusive is true, lo is the inclusive bound, else lo
	 * is the exclusive bound. Similarly for the upper bound.
	 */

	final K lo, hi;
	final byte[] loBytes, hiBytes;
	final boolean fromStart, toEnd;
	final boolean loInclusive, hiInclusive;

	NavigableSubMap(AdaptiveRadixTree<K, V> m,
			boolean fromStart, K lo, boolean loInclusive,
			boolean toEnd, K hi, boolean hiInclusive) {
		// equivalent to type check in TreeMap
		this.loBytes = m.binaryComparator().get(lo);
		this.hiBytes = m.binaryComparator().get(hi);
		if (!fromStart && !toEnd) {
			if (m.compare(loBytes, 0, loBytes.length, hiBytes, 0, hiBytes.length) > 0)
				throw new IllegalArgumentException("fromKey > toKey");
		}
		this.m = m;
		this.fromStart = fromStart;
		this.lo = lo;
		this.loInclusive = loInclusive;
		this.toEnd = toEnd;
		this.hi = hi;
		this.hiInclusive = hiInclusive;
	}

	// internal utilities

	final boolean tooLow(K key) {
		if (!fromStart) {
			int c = m.compare(key, loBytes);
			// if c == 0 and if lower bound is exclusive
			// then this key is too low
			// else it is not, since it is as low as our lower bound
			if (c < 0 || (c == 0 && !loInclusive))
				return true;
		}
		// we don't have a lower bound
		return false;
	}

	final boolean tooHigh(K key) {
		if (!toEnd) {
			int c = m.compare(key, hiBytes);
			// if c == 0 and if upper bound is exclusive
			// then this key is too higher
			// else it is not, since it is as greater as our upper bound
			if (c > 0 || (c == 0 && !hiInclusive))
				return true;
		}
		// we don't have an upper bound
		return false;
	}

	final boolean inRange(K key) {
		return !tooLow(key) && !tooHigh(key);
	}

	final boolean inClosedRange(K key) {
		// if we don't have any upper nor lower bounds, then all keys are always in range.
		// if we have a lower bound, then this key ought to be higher than our lower bound (closed, hence including).
		// if we have an upper bound, then this key ought to be lower than our upper bound (closed, hence including).
		return (fromStart || m.compare(key, loBytes) >= 0)
				&& (toEnd || m.compare(hiBytes, key) >= 0);
	}

	final boolean inRange(K key, boolean inclusive) {
		return inclusive ? inRange(key) : inClosedRange(key);
	}


	/*
	 * Absolute versions of relation operations.
	 * Subclasses map to these using like-named "sub"
	 * versions that invert senses for descending maps
	 */

	// TODO: pass loBytes, hiBytes into getCeilingEntry, getHigherEntry

	final LeafNode<K, V> absLowest() {
		LeafNode<K, V> e =
				(fromStart ? m.getFirstEntry() :
						(loInclusive ? m.getCeilingEntry(lo) :
								m.getHigherEntry(lo)));
		return (e == null || tooHigh(e.getKey())) ? null : e;
	}

	final LeafNode<K, V> absHighest() {
		LeafNode<K, V> e =
				(toEnd ? m.getLastEntry() :
						(hiInclusive ? m.getFloorEntry(hi) :
								m.getLowerEntry(hi)));
		return (e == null || tooLow(e.getKey())) ? null : e;
	}

	final LeafNode<K, V> absCeiling(K key) {
		if (tooLow(key))
			return absLowest();
		LeafNode<K, V> e = m.getCeilingEntry(key);
		return (e == null || tooHigh(e.getKey())) ? null : e;
	}

	final LeafNode<K, V> absHigher(K key) {
		if (tooLow(key))
			return absLowest();
		LeafNode<K, V> e = m.getHigherEntry(key);
		return (e == null || tooHigh(e.getKey())) ? null : e;
	}

	final LeafNode<K, V> absFloor(K key) {
		if (tooHigh(key))
			return absHighest();
		LeafNode<K, V> e = m.getFloorEntry(key);
		return (e == null || tooLow(e.getKey())) ? null : e;
	}

	final LeafNode<K, V> absLower(K key) {
		if (tooHigh(key))
			return absHighest();
		LeafNode<K, V> e = m.getLowerEntry(key);
		return (e == null || tooLow(e.getKey())) ? null : e;
	}

	/** Returns the absolute high fence for ascending traversal */
	final LeafNode<K, V> absHighFence() {
		return (toEnd ? null : (hiInclusive ?
				m.getHigherEntry(hi) :
				m.getCeilingEntry(hi))); // then hi itself (but we want the entry, hence traversal is required)
	}

	/** Return the absolute low fence for descending traversal  */
	final LeafNode<K, V> absLowFence() {
		return (fromStart ? null : (loInclusive ?
				m.getLowerEntry(lo) :
				m.getFloorEntry(lo))); // then lo itself (but we want the entry, hence traversal is required)
	}

	// Abstract methods defined in ascending vs descending classes
	// These relay to the appropriate absolute versions

	abstract LeafNode<K, V> subLowest();

	abstract LeafNode<K, V> subHighest();

	abstract LeafNode<K, V> subCeiling(K key);

	abstract LeafNode<K, V> subHigher(K key);

	abstract LeafNode<K, V> subFloor(K key);

	abstract LeafNode<K, V> subLower(K key);


	/* Returns ascending iterator from the perspective of this submap */

	abstract Iterator<K> keyIterator();

	abstract Spliterator<K> keySpliterator();


	/* Returns descending iterator from the perspective of this submap*/

	abstract Iterator<K> descendingKeyIterator();

	// public methods
	@Override
	public boolean isEmpty() {
		return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
	}

	@Override
	public int size() {
		return (fromStart && toEnd) ? m.size() : entrySet().size();
	}

	@Override
	public final boolean containsKey(Object key) {
		return inRange((K) key) && m.containsKey(key);
	}

	@Override
	public final V put(K key, V value) {
		if (!inRange(key))
			throw new IllegalArgumentException("key out of range");
		return m.put(key, value);
	}

	@Override
	public final V get(Object key) {
		return !inRange((K) key) ? null : m.get(key);
	}

	@Override
	public final V remove(Object key) {
		return !inRange((K) key) ? null : m.remove(key);
	}

	@Override
	public final Map.Entry<K, V> ceilingEntry(K key) {
		return AdaptiveRadixTree.exportEntry(subCeiling(key));
	}

	@Override
	public final K ceilingKey(K key) {
		return AdaptiveRadixTree.keyOrNull(subCeiling(key));
	}

	@Override
	public final Map.Entry<K, V> higherEntry(K key) {
		return AdaptiveRadixTree.exportEntry(subHigher(key));
	}

	@Override
	public final K higherKey(K key) {
		return AdaptiveRadixTree.keyOrNull(subHigher(key));
	}

	@Override
	public final Map.Entry<K, V> floorEntry(K key) {
		return AdaptiveRadixTree.exportEntry(subFloor(key));
	}

	@Override
	public final K floorKey(K key) {
		return AdaptiveRadixTree.keyOrNull(subFloor(key));
	}

	@Override
	public final Map.Entry<K, V> lowerEntry(K key) {
		return AdaptiveRadixTree.exportEntry(subLower(key));
	}

	@Override
	public final K lowerKey(K key) {
		return AdaptiveRadixTree.keyOrNull(subLower(key));
	}

	@Override
	public final K firstKey() {
		return AdaptiveRadixTree.key(subLowest());
	}

	@Override
	public final K lastKey() {
		return AdaptiveRadixTree.key(subHighest());
	}

	@Override
	public final Map.Entry<K, V> firstEntry() {
		return AdaptiveRadixTree.exportEntry(subLowest());
	}

	@Override
	public final Map.Entry<K, V> lastEntry() {
		return AdaptiveRadixTree.exportEntry(subHighest());
	}

	@Override
	public final Map.Entry<K, V> pollFirstEntry() {
		LeafNode<K, V> e = subLowest();
		Map.Entry<K, V> result = AdaptiveRadixTree.exportEntry(e);
		if (e != null)
			m.deleteEntry(e);
		return result;
	}

	@Override
	public final Map.Entry<K, V> pollLastEntry() {
		LeafNode<K, V> e = subHighest();
		Map.Entry<K, V> result = AdaptiveRadixTree.exportEntry(e);
		if (e != null)
			m.deleteEntry(e);
		return result;
	}

	// Views
	transient NavigableMap<K, V> descendingMapView;
	transient NavigableSubMap.EntrySetView entrySetView;
	transient KeySet<K> navigableKeySetView;

	@Override
	public final NavigableSet<K> navigableKeySet() {
		KeySet<K> nksv = navigableKeySetView;
		return (nksv != null) ? nksv :
				(navigableKeySetView = new KeySet<>(this));
	}

	@Override
	public final Set<K> keySet() {
		return navigableKeySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}

	@Override
	public final SortedMap<K, V> subMap(K fromKey, K toKey) {
		return subMap(fromKey, true, toKey, false);
	}

	@Override
	public final SortedMap<K, V> headMap(K toKey) {
		return headMap(toKey, false);
	}

	@Override
	public final SortedMap<K, V> tailMap(K fromKey) {
		return tailMap(fromKey, true);
	}

	// View classes

	// entry set views for submaps
	abstract class EntrySetView extends AbstractSet<Entry<K, V>> {
		private transient int size = -1, sizeModCount;

		// if the submap does not define any upper and lower bounds
		// i.e. it is the same view as the original map (very unlikely)
		// then no need to explicitly calculate the size.
		@Override
		public int size() {
			if (fromStart && toEnd)
				return m.size();
			// if size == -1, it is the first time we're calculating the size
			// if sizeModCount != m.getModCount(), the map has had modification operations
			// so it's size must've changed, recalculate.
			if (size == -1 || sizeModCount != m.getModCount()) {
				sizeModCount = m.getModCount();
				size = 0;
				Iterator<?> i = iterator();
				while (i.hasNext()) {
					size++;
					i.next();
				}
			}
			return size;
		}

		@Override
		public boolean isEmpty() {
			LeafNode<K, V> n = absLowest();
			return n == null || tooHigh(n.getKey());
		}

		// efficient impl of contains than the default in AbstractSet
		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			if (!inRange((K) key))
				return false;
			LeafNode<?, ?> node = m.getEntry(key);
			return node != null &&
					AdaptiveRadixTree.valEquals(node.getValue(), entry.getValue());
		}

		// efficient impl of remove than the default in AbstractSet
		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			if (!inRange((K) key))
				return false;
			LeafNode<K, V> node = m.getEntry(key);
			if (node != null && AdaptiveRadixTree.valEquals(node.getValue(),
					entry.getValue())) {
				m.deleteEntry(node);
				return true;
			}
			return false;
		}
	}


	/* Dummy value serving as unmatchable fence key for unbounded SubMapIterators */
	private static final Object UNBOUNDED = new Object();

	/*
	*  Iterators for SubMaps
	*  that understand the submap's upper and lower bound while iterating.
	*  Fence is one of the bounds depending on the kind of iterator (ascending, descending)
	*  and first becomes the other one to start from.
	*/
	abstract class SubMapIterator<T> implements Iterator<T> {
		LeafNode<K, V> lastReturned;
		LeafNode<K, V> next;
		final Object fenceKey;
		int expectedModCount;

		SubMapIterator(LeafNode<K, V> first,
				LeafNode<K, V> fence) {
			expectedModCount = m.getModCount();
			lastReturned = null;
			next = first;
			fenceKey = fence == null ? UNBOUNDED : fence.getKey();
		}

		@Override
		public final boolean hasNext() {
			return next != null && next.getKey() != fenceKey;
		}

		final LeafNode<K, V> nextEntry() {
			LeafNode<K, V> e = next;
			if (e == null || e.getKey() == fenceKey)
				throw new NoSuchElementException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			next = AdaptiveRadixTree.successor(e);
			lastReturned = e;
			return e;
		}

		final LeafNode<K, V> prevEntry() {
			LeafNode<K, V> e = next;
			if (e == null || e.getKey() == fenceKey)
				throw new NoSuchElementException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			next = AdaptiveRadixTree.predecessor(e);
			lastReturned = e;
			return e;
		}

		@Override
		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			// deleted entries are replaced by their successors
			//	if (lastReturned.left != null && lastReturned.right != null)
			//		next = lastReturned;
			m.deleteEntry(lastReturned);
			lastReturned = null;
			expectedModCount = m.getModCount();
		}
	}

	final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
		SubMapEntryIterator(LeafNode<K, V> first,
				LeafNode<K, V> fence) {
			super(first, fence);
		}

		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
		DescendingSubMapEntryIterator(LeafNode<K, V> last,
				LeafNode<K, V> fence) {
			super(last, fence);
		}

		@Override
		public Map.Entry<K, V> next() {
			return prevEntry();
		}
	}

	// Implement minimal Spliterator as KeySpliterator backup
	// TODO: understand spliterators
	final class SubMapKeyIterator extends SubMapIterator<K>
			implements Spliterator<K> {
		SubMapKeyIterator(LeafNode<K, V> first,
				LeafNode<K, V> fence) {
			super(first, fence);
		}

		@Override
		public K next() {
			return nextEntry().getKey();
		}

		@Override
		public Spliterator<K> trySplit() {
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super K> action) {
			while (hasNext())
				action.accept(next());
		}

		@Override
		public boolean tryAdvance(Consumer<? super K> action) {
			if (hasNext()) {
				action.accept(next());
				return true;
			}
			return false;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.DISTINCT | Spliterator.ORDERED |
					Spliterator.SORTED;
		}

		@Override
		public final Comparator<? super K> getComparator() {
			return NavigableSubMap.this.comparator();
		}
	}

	final class DescendingSubMapKeyIterator extends SubMapIterator<K>
			implements Spliterator<K> {
		DescendingSubMapKeyIterator(LeafNode<K, V> last,
				LeafNode<K, V> fence) {
			super(last, fence);
		}

		@Override
		public K next() {
			return prevEntry().getKey();
		}

		@Override
		public Spliterator<K> trySplit() {
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super K> action) {
			while (hasNext())
				action.accept(next());
		}

		@Override
		public boolean tryAdvance(Consumer<? super K> action) {
			if (hasNext()) {
				action.accept(next());
				return true;
			}
			return false;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.DISTINCT | Spliterator.ORDERED;
		}
	}
}


final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {
	// TODO: look into making ART and it's views (bounds) serializable later
	// private static final long serialVersionUID = 912986545866124060L;

	AscendingSubMap(AdaptiveRadixTree<K, V> m,
			boolean fromStart, K lo, boolean loInclusive,
			boolean toEnd, K hi, boolean hiInclusive) {
		super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
	}

	@Override
	public Comparator<? super K> comparator() {
		return m.comparator();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
			K toKey, boolean toInclusive) {
		if (!inRange(fromKey, fromInclusive))
			throw new IllegalArgumentException("fromKey out of range");
		if (!inRange(toKey, toInclusive))
			throw new IllegalArgumentException("toKey out of range");
		return new AscendingSubMap<>(m,
				false, fromKey, fromInclusive,
				false, toKey, toInclusive);
	}

	// TODO: offer another ctor to take in loBytes
	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		if (!inRange(toKey, inclusive))
			throw new IllegalArgumentException("toKey out of range");
		return new AscendingSubMap<>(m,
				fromStart, lo, loInclusive,
				false, toKey, inclusive);
	}

	// TODO: offer another ctor to take in hiBytes
	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		if (!inRange(fromKey, inclusive))
			throw new IllegalArgumentException("fromKey out of range");
		return new AscendingSubMap<>(m,
				false, fromKey, inclusive,
				toEnd, hi, hiInclusive);
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> mv = descendingMapView;
		return (mv != null) ? mv :
				(descendingMapView =
						new DescendingSubMap<>(m,
								fromStart, lo, loInclusive,
								toEnd, hi, hiInclusive));
	}

	@Override
	Iterator<K> keyIterator() {
		return new SubMapKeyIterator(absLowest(), absHighFence());
	}

	@Override
	Spliterator<K> keySpliterator() {
		return new SubMapKeyIterator(absLowest(), absHighFence());
	}

	@Override
	Iterator<K> descendingKeyIterator() {
		return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
	}

	final class AscendingEntrySetView extends EntrySetView {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new SubMapEntryIterator(absLowest(), absHighFence());
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		EntrySetView es = entrySetView;
		return (es != null) ? es : (entrySetView = new AscendingSubMap.AscendingEntrySetView());
	}

	@Override
	LeafNode<K, V> subLowest() {
		return absLowest();
	}

	@Override
	LeafNode<K, V> subHighest() {
		return absHighest();
	}

	@Override
	LeafNode<K, V> subCeiling(K key) {
		return absCeiling(key);
	}

	@Override
	LeafNode<K, V> subHigher(K key) {
		return absHigher(key);
	}

	@Override
	LeafNode<K, V> subFloor(K key) {
		return absFloor(key);
	}

	@Override
	LeafNode<K, V> subLower(K key) {
		return absLower(key);
	}
}

final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {

	DescendingSubMap(AdaptiveRadixTree<K, V> m,
			boolean fromStart, K lo, boolean loInclusive,
			boolean toEnd, K hi, boolean hiInclusive) {
		super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
	}

	@Override
	public Comparator<? super K> comparator() {
		return m.comparator();
	}

	// create a new submap out of a submap.
	// the new bounds should be within the current submap's bounds
	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
			K toKey, boolean toInclusive) {
		if (!inRange(fromKey, fromInclusive))
			throw new IllegalArgumentException("fromKey out of range");
		if (!inRange(toKey, toInclusive))
			throw new IllegalArgumentException("toKey out of range");
		return new DescendingSubMap<>(m,
				false, toKey, toInclusive,
				false, fromKey, fromInclusive);
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		if (!inRange(toKey, inclusive))
			throw new IllegalArgumentException("toKey out of range");
		return new DescendingSubMap<>(m,
				false, toKey, inclusive,
				toEnd, hi, hiInclusive);
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		if (!inRange(fromKey, inclusive))
			throw new IllegalArgumentException("fromKey out of range");
		return new DescendingSubMap<>(m,
				fromStart, lo, loInclusive,
				false, fromKey, inclusive);
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> mv = descendingMapView;
		return (mv != null) ? mv :
				(descendingMapView =
						new AscendingSubMap<>(m,
								fromStart, lo, loInclusive,
								toEnd, hi, hiInclusive));
	}

	@Override
	Iterator<K> keyIterator() {
		return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
	}

	@Override
	Spliterator<K> keySpliterator() {
		return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
	}

	@Override
	Iterator<K> descendingKeyIterator() {
		return new SubMapKeyIterator(absLowest(), absHighFence());
	}

	final class DescendingEntrySetView extends EntrySetView {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
		}
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		EntrySetView es = entrySetView;
		return (es != null) ? es : (entrySetView = new DescendingSubMap.DescendingEntrySetView());
	}

	@Override
	LeafNode<K, V> subLowest() {
		return absHighest();
	}

	@Override
	LeafNode<K, V> subHighest() {
		return absLowest();
	}

	@Override
	LeafNode<K, V> subCeiling(K key) {
		return absFloor(key);
	}

	@Override
	LeafNode<K, V> subHigher(K key) {
		return absLower(key);
	}

	@Override
	LeafNode<K, V> subFloor(K key) {
		return absCeiling(key);
	}

	@Override
	LeafNode<K, V> subLower(K key) {
		return absHigher(key);
	}
}
