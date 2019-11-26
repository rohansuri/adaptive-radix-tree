package com.github.rohansuri.art;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;

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

	// TODO: implement Spliterator
}
