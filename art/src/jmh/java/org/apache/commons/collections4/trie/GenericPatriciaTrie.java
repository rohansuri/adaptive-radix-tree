package org.apache.commons.collections4.trie;

import org.apache.commons.collections4.trie.KeyAnalyzer;

public class GenericPatriciaTrie<K, V> extends org.apache.commons.collections4.trie.AbstractPatriciaTrie<K, V> {
	public GenericPatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer) {
		super(keyAnalyzer);
	}
}
