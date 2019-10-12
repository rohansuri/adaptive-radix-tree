package com.github.rohansuri.art;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

// document that we don't allow null keys
// check which methods of TreeMap throw NPE
// we throw there too

public class AdaptiveRadixTree<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
	private final BinaryComparable<K> binaryComparable;
	private transient EntrySet<K, V> entrySet;
	private transient NavigableMap<K, V> descendingMap;
	private transient KeySet<K> navigableKeySet;
	private transient Collection<V> values;
	private transient int size = 0;
	/**
	 * The number of structural modifications to the tree.
	 * To be touched where ever size changes.
	 */
	private transient int modCount = 0;

	int getModCount() {
		return modCount;
	}

	// TODO: offer a bulk create constructor

	public AdaptiveRadixTree(BinaryComparable<K> binaryComparable) {
		// TODO: allow keys themselves to be BinaryComparable
		Objects.requireNonNull(binaryComparable, "Specifying a BinaryComparable is necessary. Support for having keys themselves"
				+ " being BinaryComparable will come soon.");
		this.binaryComparable = binaryComparable;
	}

	private Node root;

	public V put(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}
		byte[] bytes = binaryComparable.get(key);
		return put(bytes, key, value);
	}

	// note: taken from TreeMap
	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	// note: taken from TreeMap
	// why doesn't TreeMap use AbstractMap's provided impl?
	// the only difference is default impl requires an iterator to be created,
	// but it ultimately uses the successor calls to iterate.
	@Override
	public boolean containsValue(Object value) {
		for (LeafNode<K, V> e = getFirstEntry(); e != null; e = successor(e))
			if (valEquals(value, e.getValue()))
				return true;
		return false;
	}

	// Note: taken from TreeMap
	public Map.Entry<K, V> pollFirstEntry() {
		LeafNode<K, V> p = getFirstEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if (p != null)
			deleteEntry(p);
		return result;
	}

	// Note: taken from TreeMap
	public Map.Entry<K, V> pollLastEntry() {
		LeafNode<K, V> p = getLastEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if (p != null)
			deleteEntry(p);
		return result;
	}

	@Override
	public void clear() {
		size = 0;
		root = null;
		modCount++;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		EntrySet<K, V> es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet<>(this));
	}

	@Override
	public Collection<V> values() {
		Collection<V> c = values;
		return (c != null) ? c : (values = new Values<>(this));
	}

	private V put(byte[] keyBytes, K key, V value) {
		if (root == null) {
			// create leaf node and set root to that
			root = new LeafNode<>(keyBytes, key, value);
			size = 1;
			modCount++;
			return null;
		}
		return put(root, keyBytes, key, value, 0, null);
	}

	@Override
	public V get(Object key) {
		LeafNode<K, V> entry = getEntry(key);
		return (entry == null ? null : entry.getValue());
	}

	/**
	 * Returns this map's entry for the given key, or {@code null} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry for the given key, or {@code null} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException if the specified key cannot be compared
	 *         with the keys currently in the map
	 * @throws NullPointerException if the specified key is null
	 *         and this map uses natural ordering, or its comparator
	 *         does not permit null keys
	 */
	LeafNode<K, V> getEntry(Object key) {
		if (key == null)
			throw new NullPointerException();
		if (root == null) { // empty tree
			return null;
		}
		@SuppressWarnings("unchecked")
		K k = (K) key;
		byte[] bytes = binaryComparable.get(k);
		return getEntry(root, bytes, 0);
	}

	@Override
	public V remove(Object key) {
		LeafNode<K, V> p = getEntry(key);
		if (p == null)
			return null;
		V oldValue = p.getValue();
		deleteEntry(p);
		return oldValue;
	}

	// do we really need prevDepth to be passed around in call stack?
	// if the uplinks have already been setup, then we could use them

	private void pathCompress(Node4 toCompress) {
		updateCompressedPathOfOnlyChild(toCompress);
		Node onlyChild = toCompress.getChild()[0];
		replace(toCompress.uplinkKey(), toCompress.parent(), onlyChild);
	}

	static void updateCompressedPathOfOnlyChild(Node4 toCompress) {
		Node onlyChild = toCompress.getChild()[0];
		assert onlyChild != null;
		if (!(onlyChild instanceof LeafNode)) {
			byte partialKeyToOnlyChild = toCompress.getOnlyChildKey();// toCompress.getKeys()[0]; // R
			InnerNode oc = (InnerNode) onlyChild;
			// update nextNode's compressed path with toCompress'
			int toCopy = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, toCompress.prefixLen + 1);
			int leftForMe = InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT - toCopy;
			int iHave = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, oc.prefixLen);

			// make space
			System.arraycopy(oc.prefixKeys, 0, oc.prefixKeys, toCopy, Math.min(leftForMe, iHave));

			int toCopyFromToCompress = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, toCompress.prefixLen);
			System.arraycopy(toCompress.prefixKeys, 0, oc.prefixKeys, 0, toCopyFromToCompress);
			if (toCopyFromToCompress < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				// we got space left for the partialKey to only child
				oc.prefixKeys[toCopyFromToCompress] = partialKeyToOnlyChild;
			}
			oc.prefixLen += toCompress.prefixLen + 1;
		}
	}

	private LeafNode<K, V> getEntry(Node node, byte[] key, int depth) {
		while (true) {
			if (node instanceof LeafNode) {
				// match key to leaf
				// IDEA: this is where the complete matching can be optimized
				// if we keep track of what parts of key have already matched.
				// Because of optimistic path compression, it may not be necessary
				// that at depth D, first D bytes of key and this leaf node totally match.
				// but we could skip matching the pessimistic parts of the key
				// also the parts of the key that were directly taken traversed over (findChild)
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leaf = (LeafNode<K, V>) node;
				if (Arrays.equals(leaf.getKeyBytes(), key)) {
					return leaf;
				}
				return null;
			}

			InnerNode innerNode = (InnerNode) node;

			// if key.length == depth + innerNode.prefixLen
			// that'd only mean the key is a prefix, but we don't have that prefix
			// the key must be greater for us to take optimistic jump
			// and carry out comparisons
			if (key.length <= depth + innerNode.prefixLen
					|| comparePessimisticCompressedPath(innerNode, key, depth) != 0) {
				return null;
			}

			// complete match, continue search
			depth = depth + innerNode.prefixLen;
			Node nextNode = node.findChild(key[depth]);
			if (nextNode == null) {
				return null;
			}
			// set fields for next iteration
			depth++;
			node = nextNode;
		}
	}

	// TODO: no need for this method, this behaviour is only needed for Map.get
	// we can call compare from get directly
	// is compressed path equal/more/lesser (0, 1, -1) than key
	static int comparePessimisticCompressedPath(InnerNode node, byte[] key, int depth) {
		byte[] prefix = node.prefixKeys;
		int upperLimitForPessimisticMatch = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		// limit key because if key length greater than compressed path
		// and all byte comparisons are same, then also we consider
		// compressed path == key length
		return compare(prefix, 0, upperLimitForPessimisticMatch, key, depth, Math
				.min(depth + upperLimitForPessimisticMatch, key.length));
	}

	static int compareOptimisticCompressedPath(InnerNode node, byte[] key, int depth) {
		int result = comparePessimisticCompressedPath(node, key, depth);
		if (result != 0 || node.prefixLen <= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
			return result;
		}
		// expand optimistic path and compare
		byte[] leafBytes = getFirstEntry(node).getKeyBytes();
		// limit key because if key length greater than compressed path
		// and all byte comparisons are same, then also we consider
		// compressed path == key length
		return compare(leafBytes, depth + InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, depth + node.prefixLen,
				key, depth + InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, Math
						.min(depth + node.prefixLen, key.length));
	}


	void replace(int depth, byte[] key, Node prevDepth, Node replaceWith) {
		if (prevDepth == null) {
			assert depth == 0;
			root = replaceWith;
			AbstractNode.replaceUplink(null, root);
		}
		else {
			assert depth > 0;
			prevDepth.replace(key[depth - 1], replaceWith);
		}
	}

	// replace down link
	private void replace(byte partialKey, Node prevDepth, Node replaceWith) {
		if (prevDepth == null) {
			root = replaceWith;
			AbstractNode.replaceUplink(null, root);
		}
		else {
			prevDepth.replace(partialKey, replaceWith);
		}
	}

	private V put(Node node, byte[] keyBytes, K key, V value, int depth, Node prevDepth) {
		while (true) {
			if (node instanceof LeafNode) {
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leaf = (LeafNode<K, V>) node;
				Node pathCompressedNode = lazyExpansion(leaf, keyBytes, key, value, depth);
				if (pathCompressedNode == node) {
					// key already exists
					V oldValue = leaf.getValue();
					leaf.setValue(value);
					return oldValue;
				}
				// we gotta replace the prevDepth's child pointer to this new node
				replace(depth, keyBytes, prevDepth, pathCompressedNode);
				size++;
				modCount++;
				return null;
			}
			// compare with compressed path
			int newDepth = matchCompressedPath((InnerNode) node, keyBytes, key, value, depth, prevDepth);
			if (newDepth == -1) { // matchCompressedPath already inserted the leaf node for us
				return null;
			}
			// we're now at line 26 in paper
			byte partialKey = keyBytes[newDepth];
			Node child = node.findChild(partialKey);
			if (child == null) {
				addChild(node, partialKey, keyBytes, key, value, depth, prevDepth);
				return null;
			}
			// set fields for next iteration
			prevDepth = node;
			depth = newDepth + 1;
			node = child;
		}
	}

	/*
		create leaf node and add it lazy expanded?
		why do we say add it lazy expanded?
		because even if we're left with X partial keys (each of 1 byte),
		we're not going to branch down and create X new levels down the road.
		Nope. We reduce the height of the tree by lazy expanding.
		IDEA: Could we just keep the left over part of the key in the leaf?
		We could use byte buffer to do this (for index manipulation).
		The paper suggests storing complete key.
		Let's see, we'll refactor if we face trouble later.
		Or rather let's keep the entire key's reference?
	*/
	private void addChild(Node node, byte partialKey, byte[] keyBytes, K key, V value, int depth, Node prevDepth) {
		Node leaf = new LeafNode<>(keyBytes, key, value);
		// CLEANUP: check isFull before calling addChild? to be consistent with paper?
		if (!node.addChild(partialKey, leaf)) {
			node = node.grow();
			assert node.addChild(partialKey, leaf);

			// Important NOTE: depth != height of tree
			// depth is the depth/index in partialKey
			replace(depth, keyBytes, prevDepth, node);
		}
		size++;
		modCount++;
	}

	/*
		we reached a lazy expanded leaf node, we gotta expand it now.
		but how much should we expand?
		since we reached depth X, it means till now both leaf node and new node have same bytes.
		now what has been stored lazily is leaf node's key(depth, end).
		that's the part over which we need to compute longest common prefix.
		that's the part we can path compress.
		what is left over for both leaf, new node can be stored lazy expanded.
	*/
	private static <K, V> Node lazyExpansion(LeafNode<K, V> leaf, byte[] keyBytes, K key, V value, int depth) {
		// we refactored creation of path compressed node before knowing if it's the same key or not
		// so that early copying of path compressed node can be done.
		// but what if it is the same key?
		// then we unnecessarily have created this node.
		// what is worse? having to copy the path compressed before and later discarding it
		// or having to recopy the common prefix?
		Node4 pathCompressedNode = new Node4();
		int lcp = setLongestCommonPrefix(leaf, keyBytes, pathCompressedNode, depth);
		// why both conditions needed?
		// think of BAR present as lazily stored and we inserting BARCA
		// lcp = 3 and depth + lcp == leaf.getKey().length i.e 0 + 3 == len(BAR) = 3
		// this only confirms that leaf is a prefix of the key to be inserted (which we forbid).
		// similarly if BARCA exists and we insert BAR
		// lcp = 3, depth + lcp != leaf.getKey().length, but depth + lcp = key.length
		// so it means we're trying to insert a prefix this time (which we forbid).
		// for exact key match (i.e. key already exists), both these conditions need to be true
		if (depth + lcp == keyBytes.length && keyBytes.length == leaf.getKeyBytes().length) {
			// we're referring to a key that already exists, replace value
			// and return current
			return leaf;
		}

		// if these fail, that means:
		assert depth + lcp != keyBytes.length; // prefix is being attempted to be inserted
		assert depth + lcp != leaf.getKeyBytes().length; // current leaf will become prefix of to be inserted key

		// create path compressed node
		// make this path compressed node take the place of "child" for current on going partialKey
		// and add to it, two lazy expanded leaf nodes?
		addTwoLazyLeavesToPathCompressedNode(leaf, lcp, keyBytes, pathCompressedNode, depth, key, value);
		return pathCompressedNode;
	}

	static void removeOptimisticLCPFromCompressedPath(InnerNode node, int depth, int lcp, byte[] leafBytes) {
		// lcp cannot be equal to node.prefixLen
		// it has to be less, else it'd mean the compressed path matches completely
		assert lcp < node.prefixLen && lcp >= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT : lcp;

		// since there's more compressed path left
		// we need to "bring up" more of it what we can take
		node.prefixLen = node.prefixLen - lcp - 1;
		int end = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		for (int i = 0; i < end; i++) {
			node.prefixKeys[i] = leafBytes[i + depth + 1];
		}
	}

	static void removePessimisticLCPFromCompressedPath(InnerNode node, int depth, int lcp) {
		// lcp cannot be equal to Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen)
		// it has to be less, else it'd mean the compressed path matches completely
		assert lcp < Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		if (node.prefixLen <= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
			node.prefixLen = node.prefixLen - lcp - 1;
			for (int i = 0; i < node.prefixLen; i++) {
				node.prefixKeys[i] = node.prefixKeys[i + lcp + 1];
			}
		}
		else {
			// since there's more compressed path left
			// we need to "bring up" more of it what we can take
			node.prefixLen = node.prefixLen - lcp - 1;
			byte[] leafBytes = getFirstEntry(node).getKeyBytes();
			int end = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
			for (int i = 0; i < end; i++) {
				node.prefixKeys[i] = leafBytes[i + depth + 1];
			}
		}
	}

	/*
		 1) pessimistic path matched entirely

	 			case 1: key has nothing left (can't happen, else they'd be prefixes and our key transformations
	 					must ensure that it is not possible)
				case 2: prefixLen <= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT
				  		we're done here, we can do a findChild for next partial key (caller's depth + lcp + 1)
				case 3: prefixLen is more i.e. an optimistic path is left to match.
				  		traverse down and get leaf to match remaining optimistic prefix path.
							case 3a: optimistic path matches, we can do findChild for next partial key
							case 3b: have to split

		 2) pessimistic path did not match, we have to split
	 */
	private int matchCompressedPath(InnerNode node, byte[] keyBytes, K key, V value, int depth, Node prevDepth) {
		int lcp = 0;
		int end = Math.min(keyBytes.length, Math.min(node.prefixLen, InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT));
		// first match pessimistic compressed path
		for (; lcp < end && keyBytes[depth] == node.prefixKeys[lcp]; lcp++, depth++)
			;

		if (lcp == node.prefixLen) {
			return depth;
		}
		Node newNode;
		if (lcp == InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
			// match remaining optimistic path
			byte[] leafBytes = getFirstEntry(node).getKeyBytes();
			int leftToMatch = node.prefixLen - InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT;
			end = Math.min(keyBytes.length, depth + leftToMatch);
			/*
				match remaining optimistic path
				if we match entirely we return with new depth and caller can proceed with findChild (depth + lcp + 1)
				if we don't match entirely, then we split
			 */
			for (; depth < end && keyBytes[depth] == leafBytes[depth]; depth++, lcp++)
				;
			if (lcp == node.prefixLen) {
				return depth;
			}
			// depth != keyBytes.length
			// since if this happens, it'd mean we reached the key's end
			// and it is completely equal to the optimistic compressed path
			// which'd mean the key to be inserted is prefix of
			// all children of this node, which should never happen
			// as our key transformation must guarantee that prefixes are never possible.
			assert depth != keyBytes.length;
			newNode = branchOutOptimistic(node, keyBytes, key, value, lcp, depth, leafBytes);
		}
		else {
			newNode = branchOutPessimistic(node, keyBytes, key, value, lcp, depth);
		}
		// replace "this" node with newNode
		// initialDepth can be zero even if prefixLen is not zero.
		// the root node could have a prefix too, for example after insertions of
		// BAR, BAZ? prefix would be BA kept in the root node itself
		replace(depth - lcp, keyBytes, prevDepth, newNode);
		size++;
		modCount++;
		return -1; // we've already inserted the leaf node, caller needs to do nothing more
	}

	// called when lcp has become more than InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT
	static <K, V> Node branchOutOptimistic(InnerNode node, byte[] keyBytes, K key, V value, int lcp, int depth,
			byte[] leafBytes) {
		// prefix doesn't match entirely, we have to branch
		assert lcp < node.prefixLen && lcp >= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT : lcp;
		int initialDepth = depth - lcp;
		LeafNode leafNode = new LeafNode<>(keyBytes, key, value);

		// new node with updated prefix len, compressed path
		Node4 branchOut = new Node4();
		branchOut.prefixLen = lcp;
		// note: depth is the updated depth (initialDepth = depth - lcp)
		System.arraycopy(keyBytes, initialDepth, branchOut.prefixKeys, 0, InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
		branchOut.addChild(keyBytes[depth], leafNode);
		branchOut.addChild(leafBytes[depth], node); // reusing "this" node

		// remove lcp common prefix key from "this" node
		removeOptimisticLCPFromCompressedPath(node, depth, lcp, leafBytes);
		return branchOut;
	}

	static <K, V> Node branchOutPessimistic(InnerNode node, byte[] keyBytes, K key, V value, int lcp, int depth) {
		// pessimistic prefix doesn't match entirely, we have to branch
		// BAR, BAZ inserted, now inserting BOZ
		assert lcp < node.prefixLen && lcp < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT;

		int initialDepth = depth - lcp;

		// create new lazy leaf node for unmatched key?
		// IDEA: put context of "how much matched" into the LeafNode? for faster leaf key matching lookups?
		LeafNode leafNode = new LeafNode<>(keyBytes, key, value);

		// new node with updated prefix len, compressed path
		Node4 branchOut = new Node4();
		branchOut.prefixLen = lcp;
		// note: depth is the updated depth (initialDepth = depth - lcp)
		System.arraycopy(keyBytes, initialDepth, branchOut.prefixKeys, 0, lcp);
		branchOut.addChild(keyBytes[depth], leafNode);
		branchOut.addChild(node.prefixKeys[lcp], node); // reusing "this" node

		// remove lcp common prefix key from "this" node
		removePessimisticLCPFromCompressedPath(node, depth, lcp);
		return branchOut;
	}


	private static <K, V> void addTwoLazyLeavesToPathCompressedNode(LeafNode leaf, int lcp, byte[] keyBytes, Node4 pathCompressedNode, int depth, K key, V value) {
		// reuse current leaf node
		byte differ = leaf.getKeyBytes()[depth + lcp];
		// depth + lcp cannot be greater than leaf.getKey()'s length.
		// else that'd mean one is a prefix of the other.
		// but BinaryComparable ensures keys will never be prefixes of each other.

		pathCompressedNode
				.addChild(differ, leaf); // partialKey is the first differing partialKey between the new Node and leaf node

		// IDEA: optimisation, leaf nodes keep track of at what depth they got lazy stored
		// arrays are references in Java, then this would give us no benefit
		// in terms of storage.
		// but in terms of final leaf node level key comparison (search)
		// we'd benefit, since we only have to compare from depth to end of the referred key.

		// create new leaf node for this new key
		LeafNode newLeaf = new LeafNode<>(keyBytes, key, value);
		assert keyBytes.length > depth + lcp; // if false, would mean to-be-inserted key is a complete prefix of an already inserted key
		differ = keyBytes[depth + lcp];
		pathCompressedNode.addChild(differ, newLeaf);
	}

	static int setLongestCommonPrefix(LeafNode node, byte[] key, Node4 pathCompressedNode, int depth) {
		// both leaf node's key and new node's key should be compared from depth index
		int lcp = 0;
		byte[] leafKey = node.getKeyBytes(); // loadKey in paper
		for (; depth < leafKey.length && depth < key.length && leafKey[depth] == key[depth]; depth++, lcp++) {
			// this should be nicely branch predicted since PESSIMISTIC_PATH_COMPRESSION_LIMIT
			// is a constant
			if (lcp < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				pathCompressedNode.prefixKeys[lcp] = key[depth];
			}
		}
		pathCompressedNode.prefixLen = lcp;
		return lcp;
	}

	/*
		Returns null if the ART is empty
	 */
	@SuppressWarnings("unchecked")
	LeafNode<K, V> getFirstEntry() {
		if (isEmpty()) {
			return null;
		}
		return getFirstEntry(root);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> LeafNode<K, V> getFirstEntry(Node startFrom) {
		Node node = startFrom;
		Node next = node.first();
		while (next != null) {
			node = next;
			next = node.first();
		}
		return (LeafNode<K, V>) node;
	}

	/*
		Returns null if the ART is empty
	 */
	@SuppressWarnings("unchecked")
	LeafNode<K, V> getLastEntry() {
		if (isEmpty()) {
			return null;
		}
		return getLastEntry(root);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> LeafNode<K, V> getLastEntry(Node startFrom) {
		Node node = startFrom;
		Node next = node.last();
		while (next != null) {
			node = next;
			next = node.last();
		}
		return (LeafNode<K, V>) node;
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return exportEntry(getLowerEntry(key));
	}

	@Override
	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}

	@Override
	public K floorKey(K key) {
		return keyOrNull(getFloorEntry(key));
	}

	LeafNode<K, V> getLowerEntry(K k) {
		return getLowerOrFloorEntry(true, k);
	}

	LeafNode<K, V> getFloorEntry(K k) {
		return getLowerOrFloorEntry(false, k);
	}

	private LeafNode<K, V> getLowerOrFloorEntry(boolean lower, K k) {
		if (isEmpty()) {
			return null;
		}
		byte[] key = binaryComparable.get(k);
		int depth = 0;
		Node node = root;
		while (true) {
			if (node instanceof LeafNode) {
				// binary comparable comparison
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leafNode = (LeafNode<K, V>) node;
				byte[] leafKey = leafNode.getKeyBytes();
				if (compare(key, 0, key.length, leafKey, 0, leafKey.length) >= (lower ? 1 : 0)) {
					return leafNode;
				}
				return goUpAndFindLesser(depth, node, key);
			}
			// compare compressed path
			int compare = compareOptimisticCompressedPath((InnerNode) node, key, depth);
			if (compare == -1) { // lesser
				return getLastEntry(node);
			}
			else if (compare == 1) { // greater, that means all children of this node will be greater than key
				return goUpAndFindLesser(depth, node, key);
			}
			// compressed path matches completely
			depth += ((InnerNode) node).prefixLen;
			Node child = node.findChild(key[depth]);
			if (child == null) { // same child not found, can we find a lesser child at this node level itself?
				// CLEANUP: Node could also support a floor(partialKey) in this case to combine the findChild + lesser
				Node lesser = node.lesser(key[depth]);
				if (lesser != null) {
					return getLastEntry(lesser);
				}
				depth -= ((InnerNode) node).prefixLen;
				return goUpAndFindLesser(depth, node, key);
			}
			depth++;
			node = child;
		}
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return exportEntry(getCeilingEntry(key));
	}

	int compare(K k1, byte[] k2Bytes) {
		byte[] k1Bytes = binaryComparable.get(k1);
		return compare(k1Bytes, 0, k1Bytes.length, k2Bytes, 0, k2Bytes.length);
	}

	// 0 if a == b
	// -1 if a < b
	// 1 if a > b
	// note: aFrom, bFrom are exclusive bounds
	static int compare(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
		int i = aFrom, j = bFrom;
		for (; i < aTo && j < bTo && a[i] == b[j]; i++, j++) ;
		if (i == aTo && j == bTo) {
			return 0;
		}
		else if (i == aTo) {
			return -1;
		}
		else if (j == bTo) {
			return 1;
		}
		else {
			return a[i] < b[j] ? -1 : 1;
		}
	}

	// CLEANUP: rather than using depth, refactor to use node.parentKey() i.e. reuse successor(...) method
	private LeafNode<K, V> goUpAndFindGreater(int depth, Node node, byte[] key) {
		while ((node = node.parent()) != null) { // while you don't reach the root node
			depth--;
			Node next = node.greater(key[depth]);
			if (next != null) {
				// found a next, return first leaf
				return getFirstEntry(next);
			}
			// prepare to go up
			depth -= ((InnerNode) node).prefixLen;
		}
		return null;
	}

	private LeafNode<K, V> goUpAndFindLesser(int depth, Node node, byte[] key) {
		while ((node = node.parent()) != null) { // while you don't reach the root node
			depth--;
			Node lesser = node.lesser(key[depth]);
			if (lesser != null) {
				// found a lesser, return last leaf
				return getLastEntry(lesser);
			}
			// prepare to go up
			depth -= ((InnerNode) node).prefixLen;
		}
		return null;
	}

	@Override
	public K ceilingKey(K key) {
		return keyOrNull(getCeilingEntry(key));
	}

	/**
	 * Return key for entry, or null if null
	 * Note: taken from TreeMap
	 */
	static <K, V> K keyOrNull(Entry<K, V> e) {
		return (e == null) ? null : e.getKey();
	}

	LeafNode<K, V> getHigherEntry(K k) {
		return getHigherOrCeilEntry(false, k);
	}

	LeafNode<K, V> getCeilingEntry(K k) {
		return getHigherOrCeilEntry(true, k);
	}

	/*
		On level X match compressed path of "this" node
		if matches, then take follow on pointer and continue matching
		if doesn't, see if compressed path greater/smaller than key
			if greater, return the first node of the this level i.e. call first on this node and return.
			if lesser, go one level up (using parent link)
			and find the next partialKey greater than the uplinking partialKey on level X-1.
			if you got one, simply take the first child nodes at each down level and return
			 the leaf (left most traversal)
			if not, then we got to go on level X-2 and find the next greater
			and keep going level ups until we either find a next greater partialKey
			or we find root (which will have parent null and hence search ends).

		What if all compressed paths matched, then when taking the next follow on pointer,
		we reach a leafNode? or a null?
		if leafNode then it means, uptil now the leafNode has the same prefix as the provided key.
			if leafNode >= given key, then return leafNode
			if leafNode < given key, then take leafNode's parent uplink and find next
			greater partialKey than the uplinking partialKey on level leaf-1.
		if you reach a null, then it means key doesn't exist,
			but before taking this previous partialKey, the entire path did exist.
			Hence we come up a level from where we got the null.
			Find the next higher partialKey than which we took for null
			(no uplink from the null node, so we do it before the recursive call itself).

		so it seems the uplinking traversal is same in all cases
	  */
	private LeafNode<K, V> getHigherOrCeilEntry(boolean ceil, K k) {
		if (isEmpty()) {
			return null;
		}
		byte[] key = binaryComparable.get(k);
		int depth = 0;
		Node node = root;
		while (true) {
			if (node instanceof LeafNode) {
				// binary comparable comparison
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leafNode = (LeafNode<K, V>) node;
				byte[] leafKey = leafNode.getKeyBytes();
				if (compare(key, 0, key.length, leafKey, 0, leafKey.length) < (ceil ? 1 : 0)) {
					return leafNode;
				}
				return goUpAndFindGreater(depth, node, key);
			}
			// compare compressed path
			int compare = compareOptimisticCompressedPath((InnerNode) node, key, depth);
			if (compare == 1) { // greater
				return getFirstEntry(node);
			}
			else if (compare == -1) { // lesser, that means all children of this node will be lesser than key
				return goUpAndFindGreater(depth, node, key);
			}
			// compressed path matches completely
			depth += ((InnerNode) node).prefixLen;
			Node child = node.findChild(key[depth]);
			if (child == null) { // same child not found, can we find a greater child at this node level itself?
				// CLEANUP: Node could also support a ceil(partialKey) in this case to combine the findChild + next
				Node next = node.greater(key[depth]);
				if (next != null) {
					return getFirstEntry(next);
				}
				depth -= ((InnerNode) node).prefixLen;
				return goUpAndFindGreater(depth, node, key);
			}
			depth++;
			node = child;
		}
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return exportEntry(getHigherEntry(key));
	}

	@Override
	public K higherKey(K key) {
		return keyOrNull(getHigherOrCeilEntry(false, key));
	}

	@Override
	public Entry<K, V> firstEntry() {
		// we need a snapshot (i.e. immutable entry) as per NavigableMap's docs
		// also see Doug Lea's reply:
		// http://jsr166-concurrency.10961.n7.nabble.com/Immutable-Entry-objects-in-j-u-TreeMap-td3384.html
		// but why do we need a snapshot?
		return exportEntry(getFirstEntry());
	}

	/**
	 * Return SimpleImmutableEntry for entry, or null if null <br>
	 * Note: taken from TreeMap
	 */
	static <K, V> Map.Entry<K, V> exportEntry(Entry<K, V> e) {
		return (e == null) ? null :
				new AbstractMap.SimpleImmutableEntry<>(e);
	}

	@Override
	public Entry<K, V> lastEntry() {
		return exportEntry(getLastEntry());
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> km = descendingMap;
		return (km != null) ? km :
				(descendingMap = new DescendingSubMap<>(this,
						true, null, true,
						true, null, true));
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		KeySet<K> nks = navigableKeySet;
		return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
	}

	@Override
	public Set<K> keySet() {
		return navigableKeySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
			K toKey, boolean toInclusive) {
		return new AscendingSubMap<>(this,
				false, fromKey, fromInclusive,
				false, toKey, toInclusive);
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return new AscendingSubMap<>(this,
				true, null, true,
				false, toKey, inclusive);
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return new AscendingSubMap<>(this,
				false, fromKey, inclusive,
				true, null, true);
	}

	// QUES: why does comparator return ? super K?
	@Override
	public Comparator<? super K> comparator() {
		return null;
	}

	public BinaryComparable<K> binaryComparable() {
		return binaryComparable;
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return subMap(fromKey, true, toKey, false);
	}

	@Override

	public SortedMap<K, V> headMap(K toKey) {
		return headMap(toKey, false);
	}

	@Override

	public SortedMap<K, V> tailMap(K fromKey) {
		return tailMap(fromKey, true);
	}

	@Override
	public K firstKey() {
		return key(getFirstEntry());
	}

	/**
	 * Returns the key corresponding to the specified Entry.
	 * @throws NoSuchElementException if the Entry is null
	 * Note: taken from TreeMap
	 */
	static <K> K key(Entry<K, ?> e) {
		if (e == null)
			throw new NoSuchElementException();
		return e.getKey();
	}

	@Override
	public K lastKey() {
		return key(getLastEntry());
	}

	@Override
	public int size() {
		return size;
	}

	static <K, V> LeafNode<K, V> successor(LeafNode<K, V> of) {
		Node node = of; // LeafNode
		Node uplink;
		while ((uplink = node.parent()) != null) {
			Node greater = uplink.greater(node.uplinkKey());
			if (greater != null) {
				return getFirstEntry(greater);
			}
			node = uplink;
		}
		return null;
	}

	static <K, V> LeafNode<K, V> predecessor(LeafNode<K, V> of) {
		Node node = of; // LeafNode
		Node uplink;
		while ((uplink = node.parent()) != null) {
			Node lesser = uplink.lesser(node.uplinkKey());
			if (lesser != null) {
				return getLastEntry(lesser);
			}
			node = uplink;
		}
		return null;
	}

	// leaf should not be null
	// neither should tree be empty when calling this
	void deleteEntry(LeafNode<K, V> leaf) {
		size--;
		modCount++;
		Node parent = leaf.parent();
		if (parent == null) {
			// means root == leaf
			root = null;
			return;
		}
		parent.removeChild(leaf.uplinkKey());
		if (parent.shouldShrink()) {
			Node newParent = parent.shrink();
			// newParent should have copied the uplink to same grandParent of oldParent
			Node grandParent = newParent.parent();
			replace(newParent.uplinkKey(), grandParent, newParent);
		}
		else if (parent.size() == 1) {
			// this node can be path compressed
			// so now: grandParent --> partial key to parent --> partialKey to only leaf left
			// to path compress
			// grandParent --> same partial key, but now to leaf
			// leaf's compressed path updated to (parent's compressed path + partialKey to leaf + leaf's own compressed path)
			pathCompress((Node4) parent);
		}
	}

	/**
	 * Test two values for equality.  Differs from o1.equals(o2) only in
	 * that it copes with {@code null} o1 properly.
	 * Note: Taken from TreeMap
	 */
	static boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	Iterator<Map.Entry<K, V>> entryIterator() {
		return new EntryIterator<>(this, getFirstEntry());
	}

	Iterator<V> valueIterator() {
		return new ValueIterator<>(this, getFirstEntry());
	}

	Iterator<K> keyIterator() {
		return new KeyIterator<>(this, getFirstEntry());
	}

	Iterator<K> descendingKeyIterator() {
		return new DescendingKeyIterator<>(this, getLastEntry());
	}

}