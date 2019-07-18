package art;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
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

	// TODO: offer a bulk create constructor

	public AdaptiveRadixTree(BinaryComparable<K> binaryComparable) {
		// TODO: null check for this, then key should implement bytes() method
		Objects.requireNonNull(binaryComparable, "Specifying a BinaryComparable is necessary. Support for having keys themselves"
				+ " being BinaryComparable i.e. sporting a bytes() method will come soon.");
		this.binaryComparable = binaryComparable;
	}

	// TODO: is it unusual for a data structure library to log?
	// TODO: replace with java.util.logging so that we have no runtime dependencies?
	private static final Logger log = LoggerFactory.getLogger(AdaptiveRadixTree.class);

	private Node root;

	public V put(K key, V value) {
		byte[] bytes = binaryComparable.get(key);
		return put(bytes, key, value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	private V put(byte[] keyBytes, K key, V value) {
		if (root == null) {
			// create leaf node and set root to that
			root = new LeafNode<>(keyBytes, key, value);
			log.trace("Tree empty, creating lazily stored leaf node for key {} and making it root", Arrays
					.toString(keyBytes));
			size = 1;
			return null;
		}
		return put(root, keyBytes, key, value, 0, null);
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
			LeafNode<K, V> leaf = (LeafNode<K, V>) root;
			if (!Arrays.equals(leaf.getKeyBytes(), key)) {
				return null; // we don't have a mapping for this key
			}
			root = null;
			size = 0;
			return leaf.getValue();
		}
		return remove(root, key, 0, null);
	}

	private V remove(Node node, byte[] key, int depth, InnerNode prevDepth) {
		while (true) {
			if (!matchesCompressedPathCompletely((InnerNode) node, key, depth)) {
				return null;
			}
			final int initialDepth = depth;
			depth = depth + ((InnerNode) node).prefixLen;
			Node nextNode = node.findChild(key[depth]);
			if (nextNode == null) {
				return null;
			}
			if (nextNode instanceof LeafNode) {
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leaf = (LeafNode<K, V>) nextNode;
				if (!Arrays.equals(leaf.getKeyBytes(), key)) {
					return null; // we don't have a mapping for this key
				}
				// TODO: call deleteEntry?
				node.removeChild(key[depth]);
				size--;
				if (node.shouldShrink()) {
					log.trace("shrinking {}", node.getClass());
					node = node.shrink();
					replace(initialDepth, key, prevDepth, node);
				}
				else if (((InnerNode) node).noOfChildren == 1) {
					pathCompress((Node4) node, prevDepth, key, initialDepth);
				}
				return leaf.getValue();
			}
			// set fields for next iteration
			prevDepth = (InnerNode) node;
			node = nextNode;
			depth++;
		}
	}

	// TODO: combine both versions of pathCompress?
	// do we really need prevDepth to be passed around in call stack?
	// if the uplinks have already been setup, then we could use them
	private void pathCompress(Node4 toCompress, Node prevDepth, byte[] key, int initialDepth) {
		updateCompressedPathOfOnlyChild(toCompress);
		Node onlyChild = toCompress.getChild()[0];
		replace(initialDepth, key, prevDepth, onlyChild);
	}

	private void pathCompress(Node4 toCompress) {
		updateCompressedPathOfOnlyChild(toCompress);
		Node onlyChild = toCompress.getChild()[0];
		replace(toCompress.uplinkKey(), toCompress.parent(), onlyChild);
	}

	// TODO: unit test this
	private void updateCompressedPathOfOnlyChild(Node4 toCompress) {
		Node onlyChild = toCompress.getChild()[0];
		assert onlyChild != null;
		if (!(onlyChild instanceof LeafNode)) {
			byte partialKeyToOnlyChild = toCompress.getOnlyChild();// toCompress.getKeys()[0]; // R
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

	private V get(Node node, byte[] key, int depth) {
		while (true) {
			if (node instanceof LeafNode) {
				// match key to leaf
				// TODO: this is where the complete matching can be optimized
				// if we keep track of what parts of key have already matched
				// because of optimistic path compression, it may not be necessary
				// that at depth D, first D bytes of key and this leaf node totally match.
				// but we could skip matching the pessimistic parts of the key
				// also the parts of the key that were directly taken traversed over (findChild)
				@SuppressWarnings("unchecked")
				LeafNode<K, V> leaf = (LeafNode<K, V>) node;
				if (Arrays.equals(leaf.getKeyBytes(), key)) {
					return leaf.getValue();
				}
				return null;
			}
			// match compressed path, if match completely
			// then skip over those many prefixLen bytes from key
			// and do findChild and continue search over that child.
			// if incomplete match, then we return null.
			if (!matchesCompressedPathCompletely((InnerNode) node, key, depth)) {
				log.trace("compressed path {} and key {} didn't match entirely", ((InnerNode) node)
						.getValidPrefixKey(), key);
				return null;
			}

			log.trace("compressed path {} and key {} matched entirely", ((InnerNode) node).getValidPrefixKey(), key);
			// complete match, continue search
			depth = depth + ((InnerNode) node).prefixLen;
			Node nextNode = node.findChild(key[depth]);
			if (nextNode == null) {
				log.trace("no follow on child pointer for partialKey {}", key[depth]);
				return null;
			}
			// set fields for next iteration
			depth++;
			node = nextNode;
		}
	}

	private boolean matchesCompressedPathCompletely(InnerNode node, byte[] key, int depth) {
		int lcp;
		byte[] prefix = node.prefixKeys;
		int upperLimitForPessimisticMatch = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		for (lcp = 0; lcp < upperLimitForPessimisticMatch
				&& depth < key.length
				&& prefix[lcp] == key[depth]; lcp++, depth++)
			;
		return (lcp == upperLimitForPessimisticMatch);
	}

	// is compressed path equal/more/lesser (0, 1, -1) than key
	private int compareCompressedPath(InnerNode node, byte[] key, int depth) {
		byte[] prefix = node.prefixKeys;
		int upperLimitForPessimisticMatch = Math.min(InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
		// limit key because if key length greater than compressed path
		// and all byte comparisons are same, then also we consider
		// compressed path == key length
		return compare(prefix, 0, upperLimitForPessimisticMatch, key, depth, Math
				.min(depth + upperLimitForPessimisticMatch, key.length));
	}

	private void replace(int depth, byte[] key, Node prevDepth, Node replaceWith) {
		if (prevDepth == null) {
			log.trace("replacing root");
			root = replaceWith;
			AbstractNode.replaceUplink(null, root);
		}
		else {
			assert depth > 0;
			prevDepth.replace(key[depth - 1], replaceWith);
		}
	}

	// replace down link
	// TODO: combine with other replace version?
	private void replace(byte partialKey, Node prevDepth, Node replaceWith) {
		if (prevDepth == null) {
			log.trace("replacing root");
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
				Node pathCompressedNode = createPathCompressedNodeAfterExpandLazyLeaf(leaf, keyBytes, key, value, depth);
				if (pathCompressedNode == node) {
					// key already exists
					log.trace("key already exists, replacing value");
					V oldValue = leaf.getValue();
					leaf.setValue(value);
					return oldValue;
				}
				// we gotta replace the prevDepth's child pointer to this new node
				replace(depth, keyBytes, prevDepth, pathCompressedNode);
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
		TODO: By keeping the left over part of the key in the leaf?
		We could use byte buffer to do this (for index manipulation).
		The paper suggests storing complete key.
		Let's see, we'll refactor if we face trouble later.
		Or rather let's keep the entire key's reference?
	*/
	private void addChild(Node node, byte partialKey, byte[] keyBytes, K key, V value, int depth, Node prevDepth) {
		Node leaf = new LeafNode<>(keyBytes, key, value);
		// TODO: check isFull before calling addChild? to be consistent with paper?
		if (!node.addChild(partialKey, leaf)) {
			log.trace("growing node");
			node = node.grow();
			assert node.addChild(partialKey, leaf);

			// Important NOTE: depth != height of tree
			// depth is the depth/index in partialKey
			replace(depth, keyBytes, prevDepth, node);
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
	private Node createPathCompressedNodeAfterExpandLazyLeaf(LeafNode<K, V> leaf, byte[] keyBytes, K key, V value, int depth) {
		// we refactored creation of path compressed node before knowing if it's the same key or not
		// so that early copying of path compressed node can be done.
		// but what if it is the same key?
		// then we unnecessarily have created this node.
		// what is worse? having to copy the path compressed before and later discarding it
		// or having to recopy the common prefix?
		Node4 pathCompressedNode = new Node4();
		int lcp = longestCommonPrefix(leaf, keyBytes, pathCompressedNode, depth);
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

	private void updateCompressedPath(InnerNode node, int lcp) {
		// lcp th byte was the differing one, so we start shifting from lcp + 1
		// from the lcp th + 1 index till whatever prefix key is left, shift that to left
		for (int i = lcp + 1, j = 0; i < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && i < node.prefixLen; i++, j++) {
			node.prefixKeys[j] = node.prefixKeys[i];
		}
		node.prefixLen = node.prefixLen - lcp - 1;
		log.trace("updated compressed path {}", node.getValidPrefixKey());
	}

	private int matchCompressedPath(InnerNode node, byte[] keyBytes, K key, V value, int depth, Node prevDepth) {
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
		for (; lcp < node.prefixLen && depth < keyBytes.length && lcp < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT /*8 */ && keyBytes[depth] == node.prefixKeys[lcp]; lcp++, depth++)
			;

		log.trace("LCP of key {} and compressed path {} is {}",
				Arrays.toString(keyBytes),
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

		if (lcp <= InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT && lcp == node.prefixLen) {
			log.trace("pessimistic match");
			return depth;
		}
		else if (lcp == InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
			log.trace("optimistic match");
			return depth + (node.prefixLen - InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
		}
		else {
			branchOut(node, keyBytes, key, value, lcp, depth, prevDepth);
			size++;
			return -1; // we've already inserted the leaf node, caller needs to do nothing more
		}
	}

	// TODO: write a unit test to assert on before and after node structures (after branching out)
	private void branchOut(InnerNode node, byte[] keyBytes, K key, V value, int lcp, int depth, Node prevDepth) {
		// pessimistic prefix doesn't match entirely, we have to branch
		// BAR, BAZ inserted, now inserting BOZ

		int initialDepth = depth - lcp;
		log.trace("initial depth calculated {}", initialDepth);

		// create new lazy leaf node for unmatched key?
		// TODO: put context of "how much matched" into the LeafNode? for faster leaf key matching lookups?
		LeafNode leafNode = new LeafNode<>(keyBytes, key, value);

		log.trace("Entire compressed path didn't match, branching out on partialKey {} and {}",
				new String(new byte[] {keyBytes[depth]}), new String(new byte[] {node.prefixKeys[depth]}));

		// new node with updated prefix len, compressed path
		Node4 branchOut = new Node4();
		branchOut.prefixLen = lcp;
		// note: depth is the updated depth (initialDepth = depth - lcp)
		System.arraycopy(keyBytes, initialDepth, branchOut.prefixKeys, 0, lcp);
		branchOut.addChild(keyBytes[depth], leafNode);
		branchOut.addChild(node.prefixKeys[lcp], node); // reusing "this" node
		log.trace("added follow on pointer for partialKey {}", keyBytes[depth]);
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
		replace(initialDepth, keyBytes, prevDepth, branchOut);
	}


	private void addTwoLazyLeavesToPathCompressedNode(LeafNode leaf, int lcp, byte[] keyBytes, Node4 pathCompressedNode, int depth, K key, V value) {
		// reuse current leaf node
		byte differ = leaf.getKeyBytes()[depth + lcp];
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
		LeafNode newLeaf = new LeafNode<>(keyBytes, key, value);
		assert keyBytes.length > depth + lcp; // if false, would mean to-be-inserted key is a complete prefix of an already inserted key
		differ = keyBytes[depth + lcp];
		pathCompressedNode.addChild(differ, newLeaf);
	}

	private int longestCommonPrefix(LeafNode node, byte[] key, Node4 pathCompressedNode, int depth) {
		// both leaf node's key and new node's key should be compared from depth index
		int lcp = 0;
		byte[] leafKey = node.getKeyBytes(); // loadKey in paper
		for (; depth < leafKey.length && depth < key.length && leafKey[depth] == key[depth]; depth++, lcp++) {
			// this should be nicely branch predicated since PESSIMISTIC_PATH_COMPRESSION_LIMIT
			// is a constant
			if (lcp < InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				pathCompressedNode.prefixKeys[lcp] = key[depth];
			}
		}
		pathCompressedNode.prefixLen = lcp;
		log.trace("longest common prefix between new key {} and lazily stored leaf {} is {}", Arrays
				.toString(key), Arrays.toString(node
				.getKeyBytes()), lcp);
		return lcp;
	}

	/*
		Returns null if the ART is empty
	 */
	@SuppressWarnings("unchecked")
	private Entry<K, V> getFirstEntry() {
		if (isEmpty()) {
			return null;
		}
		return getFirstEntry(root);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Entry<K, V> getFirstEntry(Node startFrom) {
		Node node = startFrom;
		Node next = node.first();
		while (next != null) {
			node = next;
			next = node.first();
		}
		return (Entry<K, V>) node;
	}

	/*
		Returns null if the ART is empty
	 */
	@SuppressWarnings("unchecked")
	private Entry<K, V> getLastEntry() {
		if (isEmpty()) {
			return null;
		}
		return getLastEntry(root);
	}

	@SuppressWarnings("unchecked")
	private Entry<K, V> getLastEntry(Node startFrom) {
		Node node = startFrom;
		Node next = node.last();
		while (next != null) {
			node = next;
			next = node.last();
		}
		return (Entry<K, V>) node;
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return exportEntry(getLowerOrFloorEntry(true, key));
	}

	@Override
	public K lowerKey(K key) {
		return keyOrNull(getLowerOrFloorEntry(true, key));
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return exportEntry(getLowerOrFloorEntry(false, key));
	}

	@Override
	public K floorKey(K key) {
		return keyOrNull(getLowerOrFloorEntry(false, key));
	}

	private Entry<K, V> getLowerOrFloorEntry(boolean lower, K k) {
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
			int compare = compareCompressedPath((InnerNode) node, key, depth);
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
				// TODO: Node could also support a floor(partialKey) in this case to combine the findChild + lesser
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
		return exportEntry(getHigherOrCeilEntry(true, key));
	}

	// 0 if a == b
	// -1 if a < b
	// 1 if a > b
	private int compare(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
		int i = aFrom, j = bFrom;
		for (; i < aTo && j < bTo && a[i] == b[i]; i++, j++) ;
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
			return a[i] < b[i] ? -1 : 1;
		}
	}

	// TODO: rather than using depth, refactor to use node.parentKey() i.e. reuse successor(...) method
	private Entry<K, V> goUpAndFindGreater(int depth, Node node, byte[] key) {
		while ((node = node.parent()) != null) { // while you don't reach the root node
			depth--;
			log.info("finding greater for partialKey {} on level {}", key[depth], depth);
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

	private Entry<K, V> goUpAndFindLesser(int depth, Node node, byte[] key) {
		while ((node = node.parent()) != null) { // while you don't reach the root node
			depth--;
			log.info("finding lesser for partialKey {} on level {}", key[depth], depth);
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
		return keyOrNull(getHigherOrCeilEntry(true, key));
	}

	/**
	 * Return key for entry, or null if null
	 * Note: taken from TreeMap
	 */
	private static <K, V> K keyOrNull(Entry<K, V> e) {
		return (e == null) ? null : e.getKey();
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
	private Entry<K, V> getHigherOrCeilEntry(boolean ceil, K k) {
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
			int compare = compareCompressedPath((InnerNode) node, key, depth);
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
				// TODO: Node could also support a ceil(partialKey) in this case to combine the findChild + next
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
		return exportEntry(getHigherOrCeilEntry(false, key));
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
	private static <K, V> Map.Entry<K, V> exportEntry(Entry<K, V> e) {
		return (e == null) ? null :
				new AbstractMap.SimpleImmutableEntry<>(e);
	}

	@Override
	public Entry<K, V> lastEntry() {
		return exportEntry(getLastEntry());
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

	private static <K, V> Entry<K, V> successor(Entry<K, V> of) {
		Node node = (Node) of; // LeafNode
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

	// e should not be null
	// neither should tree be empty when calling this
	private void deleteEntry(Entry<K, V> e) {
		size--;
		Node leaf = (Node) e;
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

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		/*
			offer efficient implementations of these:
			(for example calling remove on AbstractSet by default iterates over all entries
			to find the entry to remove)

			contains
			remove
			size (done)
			iterator (done)
			spliterator
			clear
		 */

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

		private Entry<K, V> next;
		// to support removing the entry that was returned on the previous next() call
		private Entry<K, V> lastReturned;

		LeafNodeIterator() {
			next = getFirstEntry();
		}

		// TODO: implement iterator remove?
		// entrySet removal depends on it

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Entry<K, V> next() {
			if (next == null)
				throw new NoSuchElementException();
			lastReturned = next;
			next = successor(lastReturned);
			return lastReturned;
		}

		@Override
		public void remove() {
			if (lastReturned == null) {
				throw new IllegalStateException();
			}
			deleteEntry(lastReturned);
			lastReturned = null;
		}
	}
}
