package art;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveRadixTree<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
	private final BinaryComparable<K> binaryComparable;
	private transient AdaptiveRadixTree.EntrySet entrySet;
	private transient int size = 0;

	public AdaptiveRadixTree(BinaryComparable<K> binaryComparable) {
		// TODO: null check for this, then key should implement bytes() method
		Objects.requireNonNull(binaryComparable, "Specifying a BinaryComparable is necessary. Support for having keys themselves"
				+ " being BinaryComparable i.e. sporting a bytes() method will come soon.");
		this.binaryComparable = binaryComparable;
	}

	// TODO: is it unusual for a data structure library to log?
	// TODO: replace with java.util.logging so that we have no runtime dependencies?
	private Logger log = LoggerFactory.getLogger(AdaptiveRadixTree.class);

	private Node root;

	public V put(K key, V value) {
		byte[] bytes = binaryComparable.get(key);
		return put(bytes, value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	private V put(byte[] key, V value) {
		if (root == null) {
			// create leaf node and set root to that
			root = new LeafNode<V>(key, value);
			log.trace("Tree empty, creating lazily stored leaf node for key {} and making it root", Arrays
					.toString(key));
			size = 1;
			return null;
		}
		return put(root, key, value, 0, null);
	}

	@Override
	public V get(Object key) {
		@SuppressWarnings("unchecked")
		K k = (K) key;
		byte[] bytes = binaryComparable.get(k);
		return get(bytes);
	}

	private V get(byte[] key) {
		log.trace("getting {}", Arrays.toString(key));
		if (root == null) { // empty tree
			return null;
		}
		return get(root, key, 0);
	}

	@Override
	public V remove(Object key) {
		@SuppressWarnings("unchecked")
		K k = (K) key;
		byte[] bytes = binaryComparable.get(k);
		return remove(bytes);
	}

	private V remove(byte[] key) {
		if (root == null) {
			return null;
		}
		else if (root instanceof LeafNode) {
			@SuppressWarnings("unchecked")
			LeafNode<V> leaf = (LeafNode<V>) root;
			if (!Arrays.equals(leaf.getKey(), key)) {
				return null; // we don't have a mapping for this key
			}
			root = null;
			size = 0;
			return leaf.getValue();
		}
		return remove(root, key, 0, null);
	}

	private V remove(Node node, byte[] key, int depth, AbstractNode prevDepth) {
		while(true){
			if (!matchesCompressedPathCompletely((AbstractNode) node, key, depth)) {
				return null;
			}
			final int initialDepth = depth;
			depth = depth + ((AbstractNode) node).prefixLen;
			Node nextNode = node.findChild(key[depth]);
			if (nextNode == null) {
				return null;
			}
			if (nextNode instanceof LeafNode) {
				@SuppressWarnings("unchecked")
				LeafNode<V> leaf = (LeafNode) nextNode;
				if (!Arrays.equals(leaf.getKey(), key)) {
					return null; // we don't have a mapping for this key
				}
				node.removeChild(key[depth]);
				size--;
				if (node.shouldShrink()) {
					log.trace("shrinking {}", node.getClass());
					node = node.shrink();
					replace(initialDepth, key, prevDepth, node);
				}
				else if (((AbstractNode) node).noOfChildren == 1) {
					pathCompress((Node4) node, prevDepth, key, initialDepth);
				}
				return leaf.getValue();
			}
			// set fields for next iteration
			prevDepth = (AbstractNode) node;
			node = nextNode;
			depth++;
		}
	}

	private void pathCompress(Node4 toCompress, Node prevDepth, byte[] key, int initialDepth) {
		Node onlyChild = toCompress.getChild()[0];
		assert onlyChild != null;
		if (!(onlyChild instanceof LeafNode)) {
			byte partialKeyToOnlyChild = toCompress.getOnlyChild();// toCompress.getKeys()[0]; // R
			AbstractNode oc = (AbstractNode) onlyChild;
			// update nextNode's compressed path with toCompress'
			int toCopy = Math.min(AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, toCompress.prefixLen + 1);
			int leftForMe = AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT - toCopy;
			int iHave = Math.min(AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, oc.prefixLen);

			// make space
			System.arraycopy(oc.prefixKeys, 0, oc.prefixKeys, toCopy, Math.min(leftForMe, iHave));

			int toCopyFromToCompress = Math.min(AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, toCompress.prefixLen);
			System.arraycopy(toCompress.prefixKeys, 0, oc.prefixKeys, 0, toCopyFromToCompress);
			if (toCopyFromToCompress < AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				// we got space left for the partialKey to only child
				oc.prefixKeys[toCopyFromToCompress] = partialKeyToOnlyChild;
			}
			oc.prefixLen += toCompress.prefixLen + 1;
		}
		replace(initialDepth, key, prevDepth, onlyChild);
	}

	private V get(Node node, byte[] key, int depth) {
		if (node instanceof LeafNode) {
			// match key to leaf
			// TODO: this is where the complete matching can be optimized
			// if we keep track of what parts of key have already matched
			// because of optimistic path compression, it may not be necessary
			// that at depth D, first D bytes of key and this leaf node totally match.
			// but we could skip matching the pessimistic parts of the key
			// also the parts of the key that were directly taken traversed over (findChild)
			@SuppressWarnings("unchecked")
			LeafNode<V> leaf = (LeafNode<V>) node;
			if (Arrays.equals(leaf.getKey(), key)) {
				return leaf.getValue();
			}
			return null;
		}
		// match compressed path, if match completely
		// then skip over those many prefixLen bytes from key
		// and do findChild and continue search over that child.
		// if incomplete match, then we return null.
		if (!matchesCompressedPathCompletely((AbstractNode) node, key, depth)) {
			log.trace("compressed path {} and key {} didn't match entirely", ((AbstractNode) node)
					.getValidPrefixKey(), key);
			return null;
		}

		log.trace("compressed path {} and key {} matched entirely", ((AbstractNode) node).getValidPrefixKey(), key);
		// complete match, continue search
		depth = depth + ((AbstractNode) node).prefixLen;
		Node nextNode = node.findChild(key[depth]);
		if (nextNode == null) {
			log.trace("no follow on child pointer for partialKey {}", key[depth]);
			return null;
		}
		return get(nextNode, key, depth + 1);
	}

	private boolean matchesCompressedPathCompletely(AbstractNode node, byte[] key, int depth) {
		int lcp;
		byte[] prefix = node.prefixKeys;
		int upperLimitForPessimisticMatch = Math.min(AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		for (lcp = 0; lcp < upperLimitForPessimisticMatch
				&& depth < key.length
				&& prefix[lcp] == key[depth]; lcp++, depth++)
			;
		return (lcp == upperLimitForPessimisticMatch);
	}

	private void replace(int depth, byte[] key, Node prevDepth, Node replaceWith) {
		if (prevDepth == null) {
			log.trace("replacing root");
			root = replaceWith;
		}
		else {
			assert depth > 0;
			prevDepth.replace(key[depth - 1], replaceWith);
		}
	}

	private V put(Node node, byte[] key, V value, int depth, Node prevDepth) {

		if (node instanceof LeafNode) {
			@SuppressWarnings("unchecked")
			LeafNode<V> leaf = (LeafNode<V>) node;
			Node pathCompressedNode = createPathCompressedNodeAfterExpandLazyLeaf(leaf, key, value, depth);
			if (pathCompressedNode == node) {
				// key already exists
				log.trace("key already exists, replacing value");
				V oldValue = leaf.getValue();
				leaf.setValue(value);
				return oldValue;
			}
			// we gotta replace the prevDepth's child pointer to this new node
			replace(depth, key, prevDepth, pathCompressedNode);
			size++;
			return null;
		}

		/*
			before doing the find child, we gotta match the current node's prefix?
			i.e. the compressed path it has?
			only once that completely matches, we go ahead and skip over those many matched bytes
			in partial key and then do a findChild for the next byte partial key.
			so that means, when doing this we change our depths and jump to lower levels in the search tree.
			again compressed paths can totally match --- easy then
			differ at a point -- we do the same splitting and update the compressed path.
			TODO: analyze if code be shared for this split?
		 */

		// compare with compressed path
		int newDepth = matchCompressedPath((AbstractNode) node, key, value, depth, prevDepth);
		if (newDepth == -1) { // matchCompressedPath already inserted the leaf node for us
			return null;
		}

		// we're now at line 26 in paper

		byte partialKey = key[newDepth];
		Node child = node.findChild(partialKey);
		if (child == null) {
			addChild(node, partialKey, key, value, depth, prevDepth);
			return null;
		}
		else {
			return put(child, key, value, newDepth + 1, node);
		}
	}

	/*
		create leaf node and add it lazy expanded?
		why do we say add it lazy expanded?
		because even if we're left with X partial keys (each of 1 byte),
		we're not going to branch down and create X new levels down the road.
		Nope. We reduce the height of the tree by lazy expanding.
		TODO: By keeping the left over part of the key in the leaf?
		We could use byte buffer to do this (for index manipulation).
		The paper suggests storing complete key.
		Let's see, we'll refactor if we face trouble later.
		Or rather let's keep the entire key's reference?
	*/
	private void addChild(Node node, byte partialKey, byte[] key, V value, int depth, Node prevDepth) {
		Node leaf = new LeafNode<V>(key, value);
		// TODO: check isFull before calling addChild? to be consistent with paper?
		if (!node.addChild(partialKey, leaf)) {
			log.trace("growing node");
			node = node.grow();
			assert node.addChild(partialKey, leaf);

			// Important NOTE: depth != height of tree
			// depth is the depth/index in partialKey
			replace(depth, key, prevDepth, node);
		}
		size++;
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
	private Node createPathCompressedNodeAfterExpandLazyLeaf(LeafNode<V> leaf, byte[] key, V value, int depth) {
		// we refactored creation of path compressed node before knowing if it's the same key or not
		// so that early copying of path compressed node can be done.
		// but what if it is the same key?
		// then we unnecessarily have created this node.
		// what is worse? having to copy the path compressed before and later discarding it
		// or having to recopy the common prefix?
		Node4 pathCompressedNode = new Node4();
		int lcp = longestCommonPrefix(leaf, key, pathCompressedNode, depth);
		// why both conditions needed?
		// think of BAR present as lazily stored and we inserting BARCA
		// lcp = 3 and depth + lcp == leaf.getKey().length i.e 0 + 3 == len(BAR) = 3
		// this only confirms that leaf is a prefix of the key to be inserted (which we forbid).
		// similarly if BARCA exists and we insert BAR
		// lcp = 3, depth + lcp != leaf.getKey().length, but depth + lcp = key.length
		// so it means we're trying to insert a prefix this time (which we forbid).
		// for exact key match (i.e. key already exists), both these conditions need to be true
		if (depth + lcp == key.length && key.length == leaf.getKey().length) {
			// we're referring to a key that already exists, replace value
			// and return current
			return leaf;
		}

		// if these fail, that means:
		assert depth + lcp != key.length; // prefix is being attempted to be inserted
		assert depth + lcp != leaf.getKey().length; // current leaf will become prefix of to be inserted key

		// create path compressed node
		// make this path compressed node take the place of "child" for current on going partialKey
		// and add to it, two lazy expanded leaf nodes?
		addTwoLazyLeavesToPathCompressedNode(leaf, lcp, key, pathCompressedNode, depth, value);
		return pathCompressedNode;
	}

	private void updateCompressedPath(AbstractNode node, int lcp) {
		// lcp th byte was the differing one, so we start shifting from lcp + 1
		// from the lcp th + 1 index till whatever prefix key is left, shift that to left
		for (int i = lcp + 1, j = 0; i < AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && i < node.prefixLen; i++, j++) {
			node.prefixKeys[j] = node.prefixKeys[i];
		}
		node.prefixLen = node.prefixLen - lcp - 1;
		log.trace("updated compressed path {}", node.getValidPrefixKey());
	}

	private int matchCompressedPath(AbstractNode node, byte[] key, V value, int depth, Node prevDepth) {
		// what if prefixLen is 0?
		// could that be the case?
		// I think so! What if keys inserted are BAR, BOZ, BBC?
		// with nothing common?
		if (node.prefixLen < 1) { // compressed path empty
			return depth;
		}

		log.trace("depth before lcp {}", depth);
		// match pessimistic compressed path
		int lcp = 0;
		// it is important to have both prefixLen and prefixKeys.Length checks
		// the first one would incorporate the optimistic prefixLen as well
		// where it is more than 8 (prefixKeys size)
		// therefore we need to constraint with both when matching for pessimistic compressed path
		for (; lcp < node.prefixLen && depth < key.length && lcp < AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT /*8 */ && key[depth] == node.prefixKeys[lcp]; lcp++, depth++)
			;

		log.trace("LCP of key {} and compressed path {} is {}",
				Arrays.toString(key),
				Arrays.toString(node.getValidPrefixKey()), lcp);
		// can lcp be 0? yes
		// consider BAZ, BAR already inserted
		// and we want to insert BOZ?
		// so prefixLen is 1, but lcp is 0.

		// 1) pessimistic path matched entirely, key has nothing left (can't happen, else they'd be prefixes)
		// 2) pessimistic path matched entirely, key has bytes left, prefixLen <= 8, no need to switch to optimistic,
		//    do a findChild on this node for next partial key (depth + lcp + 1)
		// 3) pessimistic path matched entirely, key has bytes left, move to optimistic and skip over prefixLen bytes
		// 4) pessimistic path did not match, we have to split
		// purpose of pessimistic prefixKeys match is to serve as safety net and early return.

		if (lcp <= AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && lcp == node.prefixLen) {
			log.trace("pessimistic match");
			return depth;
		}
		else if (lcp == AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
			log.trace("optimistic match");
			return depth + (node.prefixLen - AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
		}
		else {
			branchOut(node, key, value, lcp, depth, prevDepth);
			size++;
			return -1; // we've already inserted the leaf node, caller needs to do nothing more
		}
	}

	// TODO: write a unit test to assert on before and after node structures (after branching out)
	private void branchOut(AbstractNode node, byte[] key, V value, int lcp, int depth, Node prevDepth) {
		// pessimistic prefix doesn't match entirely, we have to branch
		// BAR, BAZ inserted, now inserting BOZ

		int initialDepth = depth - lcp;
		log.trace("initial depth calculated {}", initialDepth);

		// create new lazy leaf node for unmatched key?
		// TODO: put context of "how much matched" into the LeafNode? for faster leaf key matching lookups?
		LeafNode leafNode = new LeafNode<V>(key, value);

		log.trace("Entire compressed path didn't match, branching out on partialKey {} and {}",
				new String(new byte[] {key[depth]}), new String(new byte[] {node.prefixKeys[depth]}));

		// new node with updated prefix len, compressed path
		Node4 branchOut = new Node4();
		branchOut.prefixLen = lcp;
		// note: depth is the updated depth (initialDepth = depth - lcp)
		System.arraycopy(key, initialDepth, branchOut.prefixKeys, 0, lcp);
		branchOut.addChild(key[depth], leafNode);
		branchOut.addChild(node.prefixKeys[lcp], node); // reusing "this" node
		log.trace("added follow on pointer for partialKey {}", key[depth]);
		log.trace("added follow on pointer for partialKey {}", node.prefixKeys[lcp]);
		// log.trace("new node contains following keys {}", Arrays.toString(branchOut.getKeys()));

		log.trace("Branched out node's prefixLen {}, prefixKey {}", branchOut.prefixLen, new String(branchOut
				.getValidPrefixKey()));

		// remove lcp common prefix key from "this" node
		updateCompressedPath(node, lcp);

		// replace "this" node with newNode
		// initialDepth can be zero even if prefixLen is not zero.
		// the root node could have a prefix too, for example after insertions of
		// BAR, BAZ? prefix would be BA kept in the root node itself
		replace(initialDepth, key, prevDepth, branchOut);
	}


	private void addTwoLazyLeavesToPathCompressedNode(LeafNode leaf, int lcp, byte[] key, Node4 pathCompressedNode, int depth, V value) {
		// reuse current leaf node
		byte differ = leaf.getKey()[depth + lcp];
		// TODO: can depth + lcp be greater than leaf.getKey()'s length?
		// shouldn't be? else that'd mean one is a prefix of the other?
		// but we said that shouldn't be the case?
		// and input itself should have a separating character? 0 byte ASCII for strings for example?
		// we should give a string parameterized put? that adds a 0 ourselves?
		// but then to give it back as a string
		// we will have to use generics. so that caller can stay type safe
		// and doesn't have to do the work of turning null terminated bytes into String again

		pathCompressedNode
				.addChild(differ, leaf); // partialKey is the first differing partialKey between the new Node and leaf node

		// TODO: optimisation, leaf nodes keep track of at what depth they got lazy stored
		// arrays are references in Java, then this would give us no benefit
		// in terms of storage.
		// but in terms of final leaf node level key comparison (search)
		// we'd benefit, since we only have to compare from depth to end of the referred key.

		// create new leaf node for this new key
		LeafNode newLeaf = new LeafNode<V>(key, value);
		assert key.length > depth + lcp; // if false, would mean to-be-inserted key is a complete prefix of an already inserted key
		differ = key[depth + lcp];
		pathCompressedNode.addChild(differ, newLeaf);
	}

	private int longestCommonPrefix(LeafNode node, byte[] key, Node4 pathCompressedNode, int depth) {
		// both leaf node's key and new node's key should be compared from depth index
		int lcp = 0;
		byte[] leafKey = node.getKey(); // loadKey in paper
		for (; depth < leafKey.length && depth < key.length && leafKey[depth] == key[depth]; depth++, lcp++) {
			// this should be nicely branch predicated since PESSIMISTIC_PATH_COMPRESSION_LIMIT
			// is a constant
			if (lcp < AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				pathCompressedNode.prefixKeys[lcp] = key[depth];
			}
		}
		pathCompressedNode.prefixLen = lcp;
		log.trace("longest common prefix between new key {} and lazily stored leaf {} is {}", Arrays
				.toString(key), Arrays.toString(node
				.getKey()), lcp);
		return lcp;
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return null;
	}

	@Override
	public K lowerKey(K key) {
		return null;
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return null;
	}

	@Override
	public K floorKey(K key) {
		return null;
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return null;
	}

	@Override
	public K ceilingKey(K key) {
		return null;
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return null;
	}

	@Override
	public K higherKey(K key) {
		return null;
	}

	@Override
	public Entry<K, V> firstEntry() {
		return null;
	}

	@Override
	public Entry<K, V> lastEntry() {
		return null;
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		return null;
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		return null;
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		return null;
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return null;
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return null;
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return null;
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return null;
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return null;
	}

	@Override
	public Comparator<? super K> comparator() {
		return null;
	}

	@Override
	@Nonnull
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return null;
	}

	@Override
	@Nonnull
	public SortedMap<K, V> headMap(K toKey) {
		return null;
	}

	@Override
	@Nonnull
	public SortedMap<K, V> tailMap(K fromKey) {
		return null;
	}

	@Override
	public K firstKey() {
		return null;
	}

	@Override
	public K lastKey() {
		return null;
	}

	@Override
	public int size(){
		return size;
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		@Nonnull
		public Iterator<Entry<K, V>> iterator() {
			return new LeafNodeIterator();
		}

		@Override
		public int size() {
			return AdaptiveRadixTree.this.size();
		}
	}

	private class LeafNodeIterator implements Iterator<Entry<K, V>> {

		private Entry<K, V> lastReturned;
		private Entry<K, V> next;

		LeafNodeIterator(){
			next = firstEntry();
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Entry<K, V> next() {
			return null;
		}
	}
}
