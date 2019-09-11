package art;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class Node256UnitTest {
	@Test
	public void testUnderCapacityCreation() {
		Node48 node48 = Node48UnitTest.createNode48();
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Node256(node48));
	}

	@Test
	public void testGrowingInto() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node48.NODE_SIZE];
		Node children[] = new Node[Node48.NODE_SIZE];
		for (int i = 1; i <= Node48.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(AbstractNode.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 23] = (byte) -(25 - i);
			children[i + 23] = Mockito.mock(AbstractNode.class);
		}

		Node16 node16 = new Node16(node4);

		for (int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++) {
			// add ith partial key
			node16.addChild(partialKeys[i], children[i]);
		}

		Node48 node48 = new Node48(node16);
		for (int i = Node16.NODE_SIZE; i < Node48.NODE_SIZE; i++) {
			// add ith partial key
			node48.addChild(partialKeys[i], children[i]);
		}
		Node256 node256 = new Node256(node48);
		for (int i = 0; i < Node256.NODE_SIZE; i++) {
			int keyIndex = node48.getKeyIndex()[i];
			if (keyIndex == Node48.ABSENT) {
				assertNull(node256.getChild()[i]);
			}
			else {
				assertEquals(node48.getChild()[keyIndex], node256.getChild()[i]);
			}
		}
	}

	private Node256 createNode256() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node48.NODE_SIZE];
		Node children[] = new Node[Node48.NODE_SIZE];
		for (int i = 1; i <= Node48.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(AbstractNode.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 23] = (byte) -(25 - i);
			children[i + 23] = Mockito.mock(AbstractNode.class);
		}

		Node16 node16 = new Node16(node4);

		for (int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++) {
			// add ith partial key
			node16.addChild(partialKeys[i], children[i]);
		}

		Node48 node48 = new Node48(node16);
		for (int i = Node16.NODE_SIZE; i < Node48.NODE_SIZE; i++) {
			// add ith partial key
			node48.addChild(partialKeys[i], children[i]);
		}
		Node256 node256 = new Node256(node48);
		return node256;
	}

	@Test
	public void testAddOnePartialKey() {
		Node256 node256 = createNode256();
		byte partialKey = 25;
		AbstractNode child = Mockito.spy(AbstractNode.class);
		assertTrue(node256.addChild(partialKey, child));
		assertEquals(49, node256.noOfChildren);
		assertEquals(child, node256.findChild(partialKey));

		// assert up link
		assertEquals(node256, child.parent());
		assertEquals(partialKey, child.uplinkKey());

		// assert correct internal structure
		int keyIndex = Byte.toUnsignedInt(partialKey);
		assertEquals(child, node256.getChild()[keyIndex]);

		partialKey = -25;
		child = Mockito.spy(AbstractNode.class);
		assertTrue(node256.addChild(partialKey, child));
		assertEquals(50, node256.noOfChildren);
		assertEquals(child, node256.findChild(partialKey));

		// assert up link
		assertEquals(node256, child.parent());
		assertEquals(partialKey, child.uplinkKey());

		// assert correct internal structure even for negative bytes
		keyIndex = Byte.toUnsignedInt(partialKey);
		assertEquals(child, node256.getChild()[keyIndex]);
	}

	@Test
	public void testFindForNonExistentPartialKey() {
		Node256 node256 = createNode256();
		assertNull(node256.findChild((byte) 25));
		assertNull(node256.findChild((byte) -25));
	}

	@Test
	public void testAddingTheSamePartialKeyAgain() {
		Node256 node256 = createNode256();
		Node child = Mockito.mock(AbstractNode.class);
		try {
			node256.addChild((byte) 1, child);
			fail();
		}
		catch (IllegalArgumentException e) {
		}

		try {
			node256.addChild((byte) -1, child);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testAddTillCapacity() {
		Node256 node256 = createNode256();
		Node child = Mockito.mock(AbstractNode.class);

		// add till capacity
		for (int i = Node48.NODE_SIZE / 2 + 1; i <= Node256.NODE_SIZE / 2; i++) {
			if (i == Node256.NODE_SIZE / 2) {
				assertTrue(node256.addChild((byte) 0, child));
				assertTrue(node256.addChild((byte) -i, child));
			}
			else {
				assertTrue(node256.addChild((byte) i, child));
				assertTrue(node256.addChild((byte) -i, child));
			}
		}
		assertTrue(node256.isFull());
		assertEquals(Node256.NODE_SIZE, node256.noOfChildren);

		// noOfChildren 256 reached, now adds will fail
		assertFalse(node256.addChild((byte) 129, child));
		assertFalse(node256.addChild((byte) -129, child));
	}

	@Test
	public void testGrowIsUnsupported() {
		Node256 node256 = createNode256();
		Assertions.assertThrows(IllegalStateException.class, node256::grow);
	}

	@Test
	public void testReplace() {
		Node256 node256 = createNode256();
		byte partialKey = 1;
		AbstractNode oldChild = (AbstractNode) node256.findChild(partialKey);
		AbstractNode newChild = Mockito.spy(AbstractNode.class);
		// assert the same child index position is updated with new child
		int keyIndex = Byte.toUnsignedInt(partialKey);
		node256.replace(partialKey, newChild);
		assertEquals(newChild, node256.findChild(partialKey));
		assertEquals(newChild, node256.getChild()[keyIndex]);

		// assert up link
		assertEquals(node256, newChild.parent());
		assertEquals(partialKey, newChild.uplinkKey());
		assertNull(oldChild.parent());

		partialKey = -1;
		oldChild = (AbstractNode) node256.findChild(partialKey);
		newChild = Mockito.spy(AbstractNode.class);
		keyIndex = Byte.toUnsignedInt(partialKey);
		node256.replace(partialKey, newChild);
		assertEquals(newChild, node256.findChild(partialKey));
		assertEquals(newChild, node256.getChild()[keyIndex]);

		// assert up link
		assertEquals(node256, newChild.parent());
		assertEquals(partialKey, newChild.uplinkKey());
		assertNull(oldChild.parent());
	}

	@Test
	public void testReplaceForNonExistentPartialKey() {
		Node256 node256 = createNode256();
		Node child = Mockito.mock(AbstractNode.class);
		Assertions.assertThrows(IllegalArgumentException.class, ()->node256.replace((byte) 25, child));
	}

	@Test
	public void testRemove() {
		Node256 node256 = createNode256();
		AbstractNode child1 = Mockito.mock(AbstractNode.class);
		AbstractNode child2 = Mockito.mock(AbstractNode.class);

		byte partialKey1 = 25;
		byte partialKey2 = -25;
		node256.addChild(partialKey1, child1);
		node256.addChild(partialKey2, child2);

		assertEquals(50, node256.noOfChildren);
		assertEquals(child1, node256.findChild(partialKey1));
		int keyIndex = Byte.toUnsignedInt(partialKey1);
		node256.removeChild(partialKey1);
		assertNull(node256.findChild(partialKey1));
		assertEquals(49, node256.noOfChildren);
		// assert internal structure
		assertNull(node256.getChild()[keyIndex]);

		assertEquals(child2, node256.findChild(partialKey2));
		keyIndex = Byte.toUnsignedInt(partialKey2);
		node256.removeChild(partialKey2);
		assertNull(node256.findChild(partialKey2));
		assertEquals(48, node256.noOfChildren);

		// assert internal structure
		assertNull(node256.getChild()[keyIndex]);

		// assert up link
		assertNull(child1.parent());
		assertNull(child2.parent());
	}

	@Test
	public void testRemoveForNonExistentPartialKey() {
		Node256 node256 = createNode256();
		byte partialKey = 25;
		try {
			node256.removeChild(partialKey);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		partialKey = -25;
		try {
			node256.removeChild(partialKey);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
}
