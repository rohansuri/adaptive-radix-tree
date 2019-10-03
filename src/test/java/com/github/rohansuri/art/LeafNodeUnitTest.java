package com.github.rohansuri.art;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class LeafNodeUnitTest {

	private final String key = "foo";
	private final String value = "bar";
	private final LeafNode<String, String> node = new LeafNode<>(BinaryComparables.forUTF8().get(key), key, value);

	// although unnecessary but for coverage
	@Test
	public void testInnerNodeMethodsSupported() {
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.addChild((byte) 0, Mockito.mock(Node.class)));

		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.removeChild((byte) 0));

		Assertions.assertThrows(UnsupportedOperationException.class,
				node::grow);

		Assertions.assertThrows(UnsupportedOperationException.class,
				node::shrink);

		Assertions.assertThrows(UnsupportedOperationException.class,
				node::shouldShrink);

		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.replace((byte) 0, Mockito.mock(Node.class)));

		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.findChild((byte) 0));

		Assertions.assertThrows(UnsupportedOperationException.class,
				node::isFull);

		Assertions.assertThrows(UnsupportedOperationException.class,
				node::size);

		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.lesser((byte) 0));

		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> node.greater((byte) 0));
	}

	@Test
	public void testFirst() {
		Assertions.assertNull(node.first());
	}

	@Test
	public void testLast() {
		Assertions.assertNull(node.last());
	}

	@Test
	public void testEntry() {
		Assertions.assertEquals(key, node.getKey());
		Assertions.assertEquals(value, node.getValue());
		node.setValue("new value");
		Assertions.assertEquals("new value", node.getValue());
	}
}
