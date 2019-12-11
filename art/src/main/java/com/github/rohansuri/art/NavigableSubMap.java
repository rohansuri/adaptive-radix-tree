package com.github.rohansuri.art;

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
import java.util.Spliterator;
import java.util.function.Consumer;

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
		this.loBytes = fromStart ? null : m.binaryComparable().get(lo);
		this.hiBytes = toEnd ? null : m.binaryComparable().get(hi);
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
				&& (toEnd || m.compare(key, hiBytes) <= 0);
	}

	final boolean inRange(K key, boolean inclusive) {
		return inclusive ? inRange(key) : inClosedRange(key);
	}


	/*
	 * Absolute versions of relation operations.
	 * Subclasses map to these using like-named "sub"
	 * versions that invert senses for descending maps
	 */

	final LeafNode<K, V> absLowest() {
		LeafNode<K, V> e =
				(fromStart ? m.getFirstEntry() :
						(loInclusive ? m.getCeilingEntry(loBytes) :
								m.getHigherEntry(loBytes)));
		return (e == null || tooHigh(e.getKey())) ? null : e;
	}

	final Uplink<K, V> absLowestWithUplink() {
		Uplink<K, V> e =
				(fromStart ? m.getFirstEntryWithUplink() :
						(loInclusive ? m.getCeilingEntryWithUplink(loBytes) :
								m.getHigherEntryWithUplink(loBytes)));
		return (e == null || tooHigh(e.from.getKey())) ? null : e;
	}

	final Path<K, V> absLowestWithPath() {
		Path<K, V> e =
				(fromStart ? m.getFirstEntryWithPath() :
						(loInclusive ? m.getCeilingEntryWithPath(loBytes) :
								m.getHigherEntryWithPath(loBytes)));
		return (e == null || tooHigh(e.to.getKey())) ? null : e;
	}

	final LeafNode<K, V> absHighest() {
		LeafNode<K, V> e =
				(toEnd ? m.getLastEntry() :
						(hiInclusive ? m.getFloorEntry(hiBytes) :
								m.getLowerEntry(hiBytes)));
		return (e == null || tooLow(e.getKey())) ? null : e;
	}

	final Uplink<K, V> absHighestWithUplink() {
		Uplink<K, V> e =
				(toEnd ? m.getLastEntryWithUplink() :
						(hiInclusive ? m.getFloorEntryWithUplink(hiBytes) :
								m.getLowerEntryWithUplink(hiBytes)));
		return (e == null || tooLow(e.from.getKey())) ? null : e;
	}

	final Path<K, V> absHighestWithPath() {
		Path<K, V> e =
				(toEnd ? m.getLastEntryWithPath() :
						(hiInclusive ? m.getFloorEntryWithPath(hiBytes) :
								m.getLowerEntryWithPath(hiBytes)));
		return (e == null || tooLow(e.to.getKey())) ? null : e;
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
				m.getHigherEntry(hiBytes) :
				m.getCeilingEntry(hiBytes))); // then hi itself (but we want the entry, hence traversal is required)
	}

	/** Return the absolute low fence for descending traversal  */
	final LeafNode<K, V> absLowFence() {
		return (fromStart ? null : (loInclusive ?
				m.getLowerEntry(loBytes) :
				m.getFloorEntry(loBytes))); // then lo itself (but we want the entry, hence traversal is required)
	}

	// Abstract methods defined in ascending vs descending classes
	// These relay to the appropriate absolute versions

	abstract LeafNode<K, V> subLowest();

	abstract Uplink<K, V> subLowestWithUplink();

	abstract LeafNode<K, V> subHighest();

	abstract Uplink<K, V> subHighestWithUplink();

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
		Uplink<K, V> uplink = subLowestWithUplink();
		if(uplink == null){
			return null;
		}
		Map.Entry<K, V> result = AdaptiveRadixTree.exportEntry(uplink.from);
		m.deleteEntryUsingThrowAwayUplink(uplink);
		return result;
	}

	@Override
	public final Map.Entry<K, V> pollLastEntry() {
		Uplink<K, V> uplink = subHighestWithUplink();
		if(uplink == null){
			return null;
		}
		Map.Entry<K, V> result = AdaptiveRadixTree.exportEntry(uplink.from);
		m.deleteEntryUsingThrowAwayUplink(uplink);
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
			Uplink<K, V> uplink = m.getEntryWithUplink(key);
			if(uplink == null){
				return false;
			}
			LeafNode<K, V> node = uplink.from;
			if (AdaptiveRadixTree.valEquals(node.getValue(),
					entry.getValue())) {
				m.deleteEntryUsingThrowAwayUplink(uplink);
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
		final LastReturned<K, V> lastReturned;
		Uplink<K, V> next;
		final Object fenceKey;
		int expectedModCount;
		final Path<K, V> path;

		SubMapIterator(Path<K, V> first,
					   LeafNode<K, V> fence) {
			expectedModCount = m.getModCount();
			lastReturned = new LastReturned<>();
			next = first.uplink();
			path = first;
			fenceKey = fence == null ? UNBOUNDED : fence.getKey();
		}

		@Override
		public final boolean hasNext() {
			return next != null && next.from.getKey() != fenceKey;
		}

		final LeafNode<K, V> nextEntry() {
			Uplink<K, V> e = next;
			if (e == null || e.from.getKey() == fenceKey)
				throw new NoSuchElementException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			lastReturned.set(e, path);
			next = path.successor();
			return lastReturned.uplink.from;
		}

		final LeafNode<K, V> prevEntry() {
			Uplink<K, V> e = next;
			if (e == null || e.from.getKey() == fenceKey)
				throw new NoSuchElementException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			lastReturned.set(e, path);
			next = path.predecessor();
			return lastReturned.uplink.from;
		}

		/*
			fundamentally the reasons for invalidation of next is because removal of array backed nodes (Node4, Node16),
			causes array index shifting and hence next happens to be in incorrect position.
			The other could be because of InnerNode references changed (shrinking, path compression)

			basically when any Cursor image is changed (which comprises of InnerNode and position)
		 */
		/*
			if next and current's common ancestor point is what current's parent is
			(i.e. path.path.get(lastReturned.pathIndex) == lastReturned.uplink.parent)
			then:
				removing current could cause shrinking:
					a -> b -> c (current)
							\-> d (next)
							no of children of b = 17
					after removing c, no of children 16.
					upon removing our cursor moves to next.

					also we've crossed threshold for Node16 and hence we should shrink.
					after shrinking we should get a new cursor on next on the new node.
					we should replace path.path.set(lastReturned.pathIndex) with this new Cursor.
						Q. do we need to touch the path after that index?
						A.
						I don't think so
						i.e. example if path to next was actually a -> b -> ... -> d
						since only the InnerNode reference has changed in between,
						but all downlinks for this new InnerNode are to the same leaves.
						As long as we reestablish the right cursor position on this new InnerNode,
						which the next was taking/will take to continue it's path, we're good.
						The metadata in the path is only the cursor, which doesn't change for downlinks.
						Neither do the downlink cursor's node references change.
						Only the parent's reference has changed and our path needs to reflect it.


				removing current could cause parent deletion and move up (path compression):
					since next has same parent it means next is the only child left!
					this means a -> b -> c (current)
									 \--> d (next)
					after removal, trie is a -> d (next)
					so we need to remove pathIndex from path (b in above example)
					cursor position of a -> d remains the same.


				removing current causes neither shrinking nor move up:
					if current.parent is Node4, Node16
						we just need to decrement cursor at path.path.get(lastReturned.pathIndex)

		 */
		final void removeAscending() {
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

		final void removeDescending() {
			if (!lastReturned.valid())
				throw new IllegalStateException();
			if (m.getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
			if(!IteratorUtils.shouldInvalidateNext(lastReturned, path)){
				// safe to call throw away delete
				m.deleteEntryUsingThrowAwayUplink(lastReturned.uplink);
			} else {
				Uplink<K, V> uplink = IteratorUtils.deleteEntryAndResetNext(m, lastReturned, path,false);
				if(uplink != null){
					next = uplink;
				}
			}
			lastReturned.reset();
			expectedModCount = m.getModCount();
		}
	}

	final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
		SubMapEntryIterator(Path<K, V> first,
				LeafNode<K, V> fence) {
			super(first, fence);
		}

		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
		DescendingSubMapEntryIterator(Path<K, V> last,
				LeafNode<K, V> fence) {
			super(last, fence);
		}

		@Override
		public Map.Entry<K, V> next() {
			return prevEntry();
		}
	}

	// Implement minimal Spliterator as KeySpliterator backup
	final class SubMapKeyIterator extends SubMapIterator<K>
			implements Spliterator<K> {
		SubMapKeyIterator(Path<K, V> first,
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

		// estimating size of submap would be expensive
		// since we'd have to traverse from lower bound to upper bound
		// for this submap
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
		DescendingSubMapKeyIterator(Path<K, V> last,
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


