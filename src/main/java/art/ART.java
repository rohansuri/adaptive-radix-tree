package art;

public class ART<V> {

	Node root;

	public ART(){
		root = new Node4();
	}

	void insert(byte[] key, V value){
		int depth = 0; // index into key
		byte partialKey = key[depth];
		Node child = root.findChild(partialKey);
		if(child == null){
			/* create leaf node and add it lazy expanded?
				why do we say add it lazy expanded?
				because even if we're left with X partial keys (each of 1 byte),
				we're not going to branch down and create X new levels down the road.
				Nope. We reduce the height of the tree by lazy expanding.
				By keeping the left over part of the key in the leaf?
				The paper suggests storing complete key.
				Let's see, we'll refactor if we face trouble later.
				Or rather let's keep the entire key's reference?
			*/
			Node leaf = new LeafNode<V>(key, value);
			if(!root.addChild(partialKey, leaf)){
				root = root.grow();
				assert root.addChild(partialKey, leaf);
			}
		}
		else {

		}
	}

	void _insert(){

	}
}
