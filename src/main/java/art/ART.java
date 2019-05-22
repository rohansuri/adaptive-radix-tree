package art;

import org.apache.commons.math3.analysis.function.Abs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ART<V> {

	// TODO: is it unusual for a data structure library to log?
	private Logger log = LoggerFactory.getLogger(ART.class);

	private Node root;

	public ART(){
		root = new Node4();
	}

	void insert(byte[] key, V value){
		insert(root, key, value, 0);
	}

	private void insert(Node node, byte[] key, V value, int depth){
		byte partialKey = key[depth];
		Node child = node.findChild(partialKey);
		if(child == null){
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
			if(!node.addChild(partialKey, leaf)){
				node = node.grow();
				assert node.addChild(partialKey, leaf);
			}
		}
		else if(child instanceof LeafNode){
			/*
			 	we reached a lazy expanded leaf node, we gotta expand it now.
			 	but how much should we expand?
			 	since we reached depth X, it means till now both leaf node and new node have same bytes.
			 	now what has been stored lazily is leaf node's key(depth, end).
			 	that's the part over which we need to compute longest common prefix.
			 	that's the part we can path compress.
			 	what is left over for both leaf, new node can be stored lazy expanded.
			  */
			LeafNode leaf = (LeafNode)child;
			int lcp = longestCommonPrefix(leaf, key, depth);

			// create path compressed node
			// make this path compressed node take the place of "child" for current on going partialKey
			// and add to it, two lazy expanded leaf nodes?
			Node pathCompressedNode = pathCompressAfterExpandingLazyLeaf(leaf, lcp, key, depth, value);
			node.replace(partialKey, pathCompressedNode);

		}
		else {
			insert(child, key, value, depth + 1);
		}
	}

	private Node pathCompressAfterExpandingLazyLeaf(LeafNode leaf, int lcp, byte[] key, int depth, V value){
		Node4 pathCompressedNode = new Node4();
		pathCompressedNode.setPrefix(lcp, key, depth);

		// reuse current leaf node
		assert leaf.getKey().length > depth + lcp;
		byte differ = leaf.getKey()[depth + lcp];
		// TODO: can depth + lcp be greater than leaf.getKey()'s length?

		pathCompressedNode.addChild(differ, leaf); // partialKey is the first differing partialKey between the new Node and leaf node

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

	private int longestCommonPrefix(LeafNode node , byte[] key, int depth){
		// both leaf node's key and new node's key should be compared from depth index
		int lcp;
		byte[] leafKey = node.getKey();
		for(lcp = 0; depth < leafKey.length && depth < key.length && leafKey[depth] == key[depth]; depth++, lcp++);
		log.debug("longest common prefix between new key {} and lazily stored leaf {} is {}", key, node.getKey(), lcp);
		return lcp;
	}
}

