package art;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class Node4UnitTest {

	@Test
	// not needed
	public void testEmptyNode() {
		Node4 node4 = new Node4();
		assertEquals(0, node4.noOfChildren);
		assertEquals(4, node4.getKeys().length);
		assertEquals(4, node4.getChild().length);
	}


	@Test
	// done
	public void testAddPartialKey() {
		Node4 node4 = new Node4();
		Node children[] = new Node[Node4.NODE_SIZE];
		// bitwise lexicographic order: 1, 2, -2, -1
		byte partialKeys[] = new byte[] {1, 2, -2, -1};
		byte storedPartialKeys[] = new byte[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			AbstractNode child = Mockito.spy(AbstractNode.class);
			node4.addChild(partialKeys[i], child);

			// assert up links created
			assertEquals(node4, child.parent());
			assertEquals(partialKeys[i], child.uplinkKey());

			children[i] = child;
			// node4 stores all partialKeys as unsigned
			storedPartialKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
		}

		// assert all partial key mappings exist
		// and in the expected bitwise lexicographic sorted order
		assertEquals(Node4.NODE_SIZE, node4.noOfChildren);
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			assertEquals(children[i], node4.findChild(partialKeys[i]));
			assertEquals(children[i], node4.getChild()[i]);
			assertEquals(storedPartialKeys[i], node4.getKeys()[i]);
		}
	}

	private void testFindForNonExistentPartialKey(Node4 node4, byte partialKey) {
		assertNull(node4.findChild(partialKey));
	}

	@Test
	// tested in remove
	public void testFindForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		byte partialKey = -1;
		testFindForNonExistentPartialKey(node4, partialKey);
		partialKey = 1;
		testFindForNonExistentPartialKey(node4, partialKey);
	}

	private void testAddingTheSamePartialKeyAgain(Node4 node4, byte partialKey) {
		Node child = Mockito.mock(AbstractNode.class);
		assertTrue(node4.addChild(partialKey, child));
		// adding the same partial key would throw an exception
		try {
			node4.addChild(partialKey, child);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	// not required since we'll put assert instead of exceptions
	public void testAddingTheSamePartialKeyAgain() {
		Node4 node4 = new Node4();
		byte partialKey = 1;
		testAddingTheSamePartialKeyAgain(node4, partialKey);
		partialKey = -1;
		testAddingTheSamePartialKeyAgain(node4, partialKey);
	}


	@Test
	public void testAddTillCapacity() {
		Node4 node4 = new Node4();
		Node child = Mockito.mock(AbstractNode.class);

		// add till capacity
		assertTrue(node4.addChild((byte) 1, child));
		assertTrue(node4.addChild((byte) 2, child));
		assertTrue(node4.addChild((byte) -3, child));
		assertTrue(node4.addChild((byte) -4, child));
		assertEquals(4, node4.noOfChildren);

		// noOfChildren 4 reached, now adds will fail
		assertFalse(node4.addChild((byte) 5, child));
		assertFalse(node4.addChild((byte) -6, child));

	}

	@Test
	public void testGrow() {
		Node4 node4 = new Node4();
		Node children[] = new Node[Node4.NODE_SIZE];
		// bitwise lexicographic order: 1, 2, -2, -1
		byte partialKeys[] = new byte[] {1, 2, -2, -1};
		byte storedPartialKeys[] = new byte[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			Node child = Mockito.mock(AbstractNode.class);
			node4.addChild(partialKeys[i], child);
			children[i] = child;
			// node4 stores all partialKeys as unsigned
			storedPartialKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
		}

		assertTrue(node4.isFull());

		Node node = node4.grow();
		// assert we grow into next larger node type 16
		assertTrue(node instanceof Node16);
		Node16 node16 = (Node16) node;

		// assert all partial key mappings exist
		// and in the same sorted order
		assertEquals(Node4.NODE_SIZE, node16.noOfChildren);
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			assertEquals(children[i], node16.findChild(partialKeys[i]));
			assertEquals(children[i], node16.getChild()[i]);

			// we test Node16's order here, since the ctor of Node16 is a copy of Node4's keys, children
			// it's not noOfChildren times addChild
			assertEquals(storedPartialKeys[i], node16.getKeys()[i]);
		}
	}

	@Test
	public void testGrowBeforeFull() {
		Node4 node4 = new Node4();
		Assertions.assertThrows(IllegalStateException.class, node4::grow);
	}

	private void testReplace(Node4 node4, byte partialKey) {
		AbstractNode child1 = Mockito.spy(AbstractNode.class);
		AbstractNode child2 = Mockito.spy(AbstractNode.class);
		node4.addChild(partialKey, child1);
		assertEquals(child1, node4.findChild(partialKey));
		node4.replace(partialKey, child2);
		assertEquals(child2, node4.findChild(partialKey));

		// assert up links
		assertEquals(node4, child2.parent());
		assertEquals(partialKey, child2.uplinkKey());
	}

	@Test
	public void testReplace() {
		Node4 node4 = new Node4();
		byte partialKey = 1;
		testReplace(node4, partialKey);
		partialKey = -1;
		testReplace(node4, partialKey);
	}


	@Test
	public void testReplaceForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(AbstractNode.class);
		byte partialKey = 1;
		Assertions.assertThrows(IllegalArgumentException.class, () -> node4.replace(partialKey, child1));
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Descending() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic descending order
		byte partialKeys[] = new byte[] {-1, -2, 2, 1};
		byte storedKeys[] = new byte[Node4.NODE_SIZE];
		Node children[] = new Node[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			storedKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
			children[i] = Mockito.mock(AbstractNode.class);
		}
		// test those right shifts to create space for the next smaller element to be added
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			// add ith partial key
			node4.addChild(partialKeys[i], children[i]);

			// assert order (-1) (-2, -1) (2, -2, -1) (1, 2, -2, -1)
			List<Byte> reversedStoredKeys = Lists.newArrayList((Bytes.asList(storedKeys).subList(0, i + 1)));
			Collections.reverse(reversedStoredKeys);
			List<Node> reversedChildren = Lists.newArrayList((Arrays.asList(children).subList(0, i + 1)));
			Collections.reverse(reversedChildren);

			for (int j = 0; j <= i; j++) {
				byte key = node4.getKeys()[j];
				Node child = node4.getChild()[j];
				assertEquals((byte) reversedStoredKeys.get(j), key);
				assertEquals(reversedChildren.get(j), child);
			}
		}
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Ascending() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[] {1, 2, -2, -1};
		byte storedKeys[] = new byte[Node4.NODE_SIZE];
		Node children[] = new Node[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			storedKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
			children[i] = Mockito.mock(AbstractNode.class);
		}

		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			// add ith partial key
			node4.addChild(partialKeys[i], children[i]);

			// assert order (1) (1, 2) (1, 2, -2) (1, 2, -2, -1)
			for (int j = 0; j <= i; j++) {
				byte key = node4.getKeys()[j];
				Node child = node4.getChild()[j];
				assertEquals(storedKeys[j], key);
				assertEquals(children[j], child);
			}
		}
	}

	@Test
	public void testRemove() {
		Node4 node4 = new Node4();
		AbstractNode child1 = Mockito.mock(AbstractNode.class);
		AbstractNode child2 = Mockito.mock(AbstractNode.class);

		byte partialKey1 = 1;
		byte partialKey2 = -1;
		node4.addChild(partialKey1, child1);
		node4.addChild(partialKey2, child2);

		assertNotNull(node4.findChild(partialKey1));
		node4.removeChild(partialKey1);
		assertNull(node4.findChild(partialKey1));
		assertEquals(1, node4.noOfChildren);

		assertNotNull(node4.findChild(partialKey2));
		node4.removeChild(partialKey2);
		assertNull(node4.findChild(partialKey2));
		assertEquals(0, node4.noOfChildren);

		// assert up link removed
		assertNull(child1.parent());
		assertNull(child2.parent());
	}

	@Test
	public void testRemoveForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		byte partialKey = 1;
		try {
			node4.removeChild(partialKey);
			fail();
		}
		catch (IllegalArgumentException e) {
		}

		partialKey = -1;
		try {
			node4.removeChild(partialKey);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testRemoveMaintainsOrder_Ascending() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[] {1, 2, -2, -1};
		byte storedKeys[] = new byte[Node4.NODE_SIZE];
		Node children[] = new Node[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			storedKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
			children[i] = Mockito.mock(AbstractNode.class);
		}

		// add all
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			node4.addChild(partialKeys[i], children[i]);
		}

		// remove one by one
		// test those left shifts to cover up the removed element
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			// remove ith partial key
			node4.removeChild(partialKeys[i]);

			// assert order (1, 2, -2, -1)(2, -2, -1) (-2, -1) (-1)
			for (int j = 0; j < node4.noOfChildren; j++) {
				byte key = node4.getKeys()[j];
				Node child = node4.getChild()[j];
				assertEquals(storedKeys[j + i + 1], key);
				assertEquals(children[j + i + 1], child);
			}
		}

		assertEquals(0, node4.noOfChildren);
	}

	@Test
	public void testRemoveMaintainsOrder_Descending() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic descending order
		byte partialKeys[] = new byte[] {-1, -2, 2, 1};
		byte storedKeys[] = new byte[Node4.NODE_SIZE];
		Node children[] = new Node[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			storedKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
			children[i] = Mockito.mock(AbstractNode.class);
		}

		// add all
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			node4.addChild(partialKeys[i], children[i]);
		}

		// remove one by one
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			// remove ith partial key
			node4.removeChild(partialKeys[i]);

			// assert order (1, 2, -2, -1)(1, 2, -2) (1, 2) (1)
			List<Byte> reversedStoredKeys = Lists
					.newArrayList((Bytes.asList(storedKeys).subList(i + 1, Node4.NODE_SIZE)));
			Collections.reverse(reversedStoredKeys);
			List<Node> reversedChildren = Lists.newArrayList((Arrays.asList(children).subList(i + 1, Node4.NODE_SIZE)));
			Collections.reverse(reversedChildren);
			for (int j = 0; j < node4.noOfChildren; j++) {
				byte key = node4.getKeys()[j];
				Node child = node4.getChild()[j];
				assertEquals((byte) reversedStoredKeys.get(j), key);
				assertEquals(reversedChildren.get(j), child);
			}
		}

		assertEquals(0, node4.noOfChildren);
	}
}
