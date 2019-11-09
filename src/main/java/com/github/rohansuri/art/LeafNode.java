package com.github.rohansuri.art;

// CLEANUP: better design to avoid LeafNode not having those UnsupportedExceptions?

import java.util.Map;

/*
    avoid this LeafNode extra hop? rather use child pointers as Object? (read notes below)
    i.e using the same pointer for both pointing to child Nodes as well as value

    currently we use what the paper mentions as "Single-value" leaves
 */
class LeafNode<K, V> extends Node implements Map.Entry<K, V> {
	private V value;

	private static final String EXCEPTION_MSG = "should not be called on LeafNode";

	// we have to save the keyBytes, because leaves are lazy expanded at times
	private final byte[] keyBytes;
	private final K key;

	LeafNode(byte[] keyBytes, K key, V value) {
		this.value = value;
		this.keyBytes = keyBytes;
		this.key = key;
	}

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	public V getValue() {
		return value;
	}

	byte[] getKeyBytes() {
		return keyBytes;
	}

	public K getKey() {
		return key;
	}

	@Override
	public Node findChild(byte partialKey) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public boolean addChild(byte partialKey, Node child) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public void replace(byte partialKey, Node newChild) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public void removeChild(byte partialKey) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public Node grow() {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public boolean shouldShrink() {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public Node shrink() {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	/**
	 Dev note: first() is implemented to detect end of the SortedMap.firstKey()
	 */
	@Override
	public Node first() {
		return null;
	}

	/**
	 Dev note: last() is implemented to detect end of the SortedMap.lastKey()
	 */
	@Override
	public Node last() {
		return null;
	}

	@Override
	public Node greater(byte partialKey) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public Node lesser(byte partialKey) {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public short size() {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	@Override
	public boolean isFull() {
		throw new UnsupportedOperationException(EXCEPTION_MSG);
	}

	/**
	 * Compares this <code>Map.Entry</code> with another <code>Map.Entry</code>.
	 * <p>
	 * Implemented per API documentation of {@link java.util.Map.Entry#equals(Object)}
	 *
	 * @param obj  the object to compare to
	 * @return true if equal key and value
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Map.Entry)) {
			return false;
		}
		final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
		return
				(getKey() == null ? other.getKey() == null : getKey().equals(other.getKey())) &&
						(getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
	}

	/**
	 * Gets a hashCode compatible with the equals method.
	 * <p>
	 * Implemented per API documentation of {@link java.util.Map.Entry#hashCode()}
	 *
	 * @return a suitable hash code
	 */
	@Override
	public int hashCode() {
		return (getKey() == null ? 0 : getKey().hashCode()) ^
				(getValue() == null ? 0 : getValue().hashCode());
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}
}