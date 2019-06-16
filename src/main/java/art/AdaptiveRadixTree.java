package art;

public class AdaptiveRadixTree<K, V>  {
	private final BinaryComparable<K> binaryComparable;
	private final ART<V> art;

	public AdaptiveRadixTree(BinaryComparable<K> binaryComparable){
		this.binaryComparable = binaryComparable;
		this.art = new ART<>();
	}

	public V put(K key, V value){
		byte[] bytes = binaryComparable.get(key);
		return art.put(bytes, value);
	}

	public V get(K key){
		byte[] bytes = binaryComparable.get(key);
		return art.get(bytes);
	}

	public V remove(K key){
		byte[] bytes = binaryComparable.get(key);
		return art.remove(bytes);
	}
}
