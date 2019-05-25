package art;

import org.apache.commons.math3.analysis.function.Abs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ART<V> {

	// TODO: is it unusual for a data structure library to log?
	private Logger log = LoggerFactory.getLogger(ART.class);

	private Node root;

	public ART() {
		root = new Node4();
	}

	public void insert(byte[] key, V value) {
		insert(root, key, value, 0, null);
	}

	private void insert(Node node, byte[] key, V value, int depth, Node prevDepth) {
		/*
			before doing the find child, we gotta match the current node's prefix?
			i.e. the compressed path it has?
			only once that completely matches, we go ahead and skip over those many matched bytes
			in partial key and then do a findChild for the next byte partial key.
			so that means, when doing this we change our depths and jump to lower levels in the search tree.
			again compressed paths can totally match --- easy then
			differ at a point -- we do the same splitting and update the compressed path.
		 */

		// compare with compressed path
		depth = matchCompressedPath((AbstractNode) node, key, value, depth, prevDepth);
		if (depth == -1) { // matchCompressedPath already inserted the leaf node for us
			return;
		}

		byte partialKey = key[depth];
		Node child = node.findChild(partialKey);
		if (child == null) {
			/* create leaf node and add it lazy expanded?
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
			Node leaf = new LeafNode<V>(key, value);
			if (!node.addChild(partialKey, leaf)) {
				node = node.grow();
				assert node.addChild(partialKey, leaf);
			}
		}
		else if (child instanceof LeafNode) {
			/*
			 	we reached a lazy expanded leaf node, we gotta expand it now.
			 	but how much should we expand?
			 	since we reached depth X, it means till now both leaf node and new node have same bytes.
			 	now what has been stored lazily is leaf node's key(depth, end).
			 	that's the part over which we need to compute longest common prefix.
			 	that's the part we can path compress.
			 	what is left over for both leaf, new node can be stored lazy expanded.
			  */
			LeafNode leaf = (LeafNode) child;
			int lcp = longestCommonPrefix(leaf, key, depth);

			// create path compressed node
			// make this path compressed node take the place of "child" for current on going partialKey
			// and add to it, two lazy expanded leaf nodes?
			Node pathCompressedNode = pathCompressAfterExpandingLazyLeaf(leaf, lcp, key, depth, value);
			node.replace(partialKey, pathCompressedNode);

		}
		else {
			insert(child, key, value, depth + 1, node);
		}
	}

	// return new depth
	private int matchCompressedPath(AbstractNode node, byte[] key, V value, int depth, Node prevDepth) {
		// what if prefixLen is 0?
		// could that be the case?
		// I think so! What if keys inserted are BAR, BOZ, BBC?
		// with nothing common?
		if (node.prefixLen < 1) { // compressed path empty
			return depth;
		}

		final int initialDepth = depth;

		// match pessimistic compressed path
		int lcp;
		// it is important to have both prefixLen and prefixKeys.Length checks
		// the first one would incorporate the optimistic prefixLen as well
		// where it is more than 8 (prefixKeys size)
		// therefore we need to constraint with both when matching for pessimistic compressed path
		for (lcp = 0; lcp < node.prefixLen && depth < key.length && lcp < AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT /*8 */ && key[depth] == node.prefixKeys[lcp]; lcp++, depth++)
			;

		// 1) pessimistic path matched entirely, key has nothing left (can't happen, else they'd be prefixes)
		// 2) pessimistic path matched entirely, key has bytes left, prefixLen <= 8, no need to switch to optimistic,
		//    do a findChild on this node for next partial key (depth + lcp + 1)
		// 3) pessimistic path matched entirely, key has bytes left, move to optimistic and skip over prefixLen bytes
		// 4) pessimistic path did not match, we have to split
		// purpose of pessimistic prefixKeys match is to serve as safety net and early return.

		if (lcp == node.prefixLen) {
			if (node.prefixLen <= AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
				// stay on pessimistic
				return depth;
			}
			else {
				// switch to optimistic
				return depth + (node.prefixLen - AbstractNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
			}
		}
		else { // pessimistic prefix doesn't match entirely, we have to branch
			// BAR, BAZ inserted, now inserting BOZ

			// create new lazy leaf node for unmatched key?
			// TODO: put context of "how much matched" into the LeafNode? for faster leaf key matching lookups?
			LeafNode leafNode = new LeafNode<V>(key, value);

			// new node with updated prefix len, compressed path
			Node4 branchOut = new Node4();
			branchOut.prefixLen = lcp;
			System.arraycopy(key, depth - lcp, branchOut.prefixKeys, 0, lcp);
			branchOut.addChild(key[depth], leafNode);
			branchOut.addChild(node.prefixKeys[lcp], node); // reusing "this" node

			// remove lcp'th prefix key from "this" node
			removeArrayHead(node);

			// replace "this" node with newNode
			// initialDepth should never be zero, because if it is, prefixLen would be zero too
			// and we'd have made an early exit from this method itself
			assert initialDepth > 0;
			prevDepth.replace(key[initialDepth - 1], branchOut);
			return -1; // we've already inserted the leaf node, caller needs to do nothing more
		}
	}

	private void removeArrayHead(AbstractNode node) {
		// shift all elements left by one
		for (int i = 0; i < node.prefixLen - 1; i++) {
			node.prefixKeys[i] = node.prefixKeys[i + 1];
		}
		node.prefixLen--;
	}


	private Node pathCompressAfterExpandingLazyLeaf(LeafNode leaf, int lcp, byte[] key, int depth, V value) {
		Node4 pathCompressedNode = new Node4();
		pathCompressedNode.setPrefix(lcp, key, depth);

		// reuse current leaf node
		assert leaf.getKey().length > depth + lcp; // if equal, that mean leaf is prefix of to-be-inserted key!
		byte differ = leaf.getKey()[depth + lcp];
		// TODO: can depth + lcp be greater than leaf.getKey()'s length?
		// shouldn't be? else that'd mean one is a prefix of the other?
		// but we said that shouldn't be the case?
		// and input itself should have a separating character? 0 byte ASCII for strings for example?
		// we should give a string parameterized insert? that adds a 0 ourselves?
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
		assert key.length > depth + lcp;
		differ = key[depth + lcp];
		pathCompressedNode.addChild(differ, newLeaf);
		return pathCompressedNode;
	}

	private int longestCommonPrefix(LeafNode node, byte[] key, int depth) {
		// both leaf node's key and new node's key should be compared from depth index
		int lcp;
		byte[] leafKey = node.getKey();
		for (lcp = 0; depth < leafKey.length && depth < key.length && leafKey[depth] == key[depth]; depth++, lcp++) ;
		log.debug("longest common prefix between new key {} and lazily stored leaf {} is {}", key, node.getKey(), lcp);
		return lcp;
	}
}

