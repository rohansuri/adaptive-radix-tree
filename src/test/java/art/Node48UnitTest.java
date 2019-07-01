package art;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class Node48UnitTest {

	@Test(expected = IllegalArgumentException.class)
	public void testUnderCapacityCreation() {
		Node16 node16 = Node16UnitTest.createNode16();
		Node48 node48 = new Node48(node16);
	}

	@Test
	public void testGrowingInto() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node16.NODE_SIZE];
		Node children[] = new Node[Node16.NODE_SIZE];
		for (int i = 1; i <= Node16.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(AbstractNode.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 7] = (byte) -(9 - i);
			children[i + 7] = Mockito.mock(AbstractNode.class);
		}

		Node16 node16 = new Node16(node4);

		for (int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++) {
			// add ith partial key
			node16.addChild(partialKeys[i], children[i]);
		}

		Node48 node48 = new Node48(node16);
		Assert.assertEquals(Node16.NODE_SIZE, node48.noOfChildren);
		for (int i = 0; i < node48.noOfChildren; i++) {
			// node16 entries are already kept sorted
			// and when growing into, it's a linear copy
			// so they must exist sorted in node48 as well
			Assert.assertEquals(children[i], node48.findChild(partialKeys[i]));
			Assert.assertEquals(children[i], node48.getChild()[i]);
			int unsignedIndexing = Byte.toUnsignedInt(partialKeys[i]);
			byte index = node48.getKeyIndex()[unsignedIndexing];
			Assert.assertNotEquals(Node48.ABSENT, index);
			Assert.assertEquals(children[i], node48.getChild()[index]);
		}
	}

	static Node48 createNode48() {
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node16.NODE_SIZE];
		Node children[] = new Node[Node16.NODE_SIZE];
		for (int i = 1; i <= Node16.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(AbstractNode.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 7] = (byte) -(9 - i);
			children[i + 7] = Mockito.mock(AbstractNode.class);
		}

		Node16 node16 = new Node16(node4);

		for (int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++) {
			// add ith partial key
			node16.addChild(partialKeys[i], children[i]);
		}

		Node48 node48 = new Node48(node16);
		return node48;
	}

	@Test
	public void testAddOnePartialKey() {
		Node48 node48 = createNode48();
		byte partialKey = 9;
		AbstractNode child = Mockito.mock(AbstractNode.class);
		Assert.assertTrue(node48.addChild(partialKey, child));
		Assert.assertEquals(17, node48.noOfChildren);
		Assert.assertEquals(child, node48.findChild(partialKey));

		// assert correct internal structure
		int keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)];
		Assert.assertNotEquals(Node48.ABSENT, keyIndex);
		Assert.assertEquals(child, node48.getChild()[keyIndex]);

		// assert up link
		Assert.assertEquals(partialKey, child.partialKey);
		Assert.assertEquals(node48, child.parent);

		partialKey = -9;
		child = Mockito.mock(AbstractNode.class);
		Assert.assertTrue(node48.addChild(partialKey, child));
		Assert.assertEquals(18, node48.noOfChildren);
		Assert.assertEquals(child, node48.findChild(partialKey));

		// assert correct internal structure even for negative bytes
		keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)];
		Assert.assertNotEquals(Node48.ABSENT, keyIndex);
		Assert.assertEquals(child, node48.getChild()[keyIndex]);

		// assert up link
		Assert.assertEquals(partialKey, child.partialKey);
		Assert.assertEquals(node48, child.parent);
	}

	@Test
	public void testFindForNonExistentPartialKey() {
		Node48 node48 = createNode48();
		Assert.assertNull(node48.findChild((byte) 9));
		Assert.assertNull(node48.findChild((byte) -9));
	}

	@Test
	public void testAddingTheSamePartialKeyAgain() {
		Node48 node48 = createNode48();
		Node child = Mockito.mock(AbstractNode.class);
		try {
			node48.addChild((byte) 1, child);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}

		try {
			node48.addChild((byte) -1, child);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testAddTillCapacity() {
		Node48 node48 = createNode48();
		Node child = Mockito.mock(AbstractNode.class);

		// add till capacity
		for (int i = Node16.NODE_SIZE / 2 + 1; i <= Node48.NODE_SIZE / 2; i++) {
			Assert.assertTrue(node48.addChild((byte) i, child));
			Assert.assertTrue(node48.addChild((byte) -i, child));
		}
		Assert.assertEquals(Node48.NODE_SIZE, node48.noOfChildren);

		// noOfChildren 48 reached, now adds will fail
		Assert.assertFalse(node48.addChild((byte) 25, child));
		Assert.assertFalse(node48.addChild((byte) -25, child));
	}

	@Test
	public void testGrow() {
		Node48 node48 = createNode48();
		// add till capacity
		for (int i = Node16.NODE_SIZE / 2 + 1; i <= Node48.NODE_SIZE / 2; i++) {
			Node child = Mockito.mock(AbstractNode.class);
			Assert.assertTrue(node48.addChild((byte) i, child));
			Assert.assertTrue(node48.addChild((byte) -i, child));
		}

		Assert.assertTrue(node48.isFull());
		Node node = node48.grow();

		Assert.assertTrue(node instanceof Node256);

		Node256 node256 = (Node256) node;
		Assert.assertEquals(Node48.NODE_SIZE, node256.noOfChildren);

		// both node 48 and node 256 have keyIndex
		// of size 256
		// so they must be exactly equal
		for (int i = 0; i < Node48.KEY_INDEX_SIZE; i++) {
			int keyIndex = node48.getKeyIndex()[i];
			if (keyIndex == Node48.ABSENT) {
				Assert.assertNull(node256.getChild()[i]);
			}
			else {
				Assert.assertNotNull(node256.getChild()[i]);
				Assert.assertEquals(node48.getChild()[keyIndex], node256.getChild()[i]);
			}
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testGrowBeforeFull() {
		Node48 node48 = createNode48();
		node48.grow();
	}

	@Test
	public void testReplace() {
		Node48 node48 = createNode48();
		byte partialKey = 1;
		AbstractNode oldChild = (AbstractNode) node48.findChild(partialKey);
		AbstractNode newChild = Mockito.mock(AbstractNode.class);
		// assert the same child index position is updated with new child
		int keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)];
		node48.replace(partialKey, newChild);
		Assert.assertEquals(newChild, node48.findChild(partialKey));
		Assert.assertEquals(keyIndex, node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)]);

		// assert up link
		Assert.assertNull(oldChild.parent);
		Assert.assertEquals(node48, newChild.parent);
		Assert.assertEquals(partialKey, newChild.partialKey);

		partialKey = -1;
		oldChild = (AbstractNode) node48.findChild(partialKey);
		newChild = Mockito.mock(AbstractNode.class);
		keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)];
		node48.replace(partialKey, newChild);
		Assert.assertEquals(newChild, node48.findChild(partialKey));
		Assert.assertEquals(keyIndex, node48.getKeyIndex()[Byte.toUnsignedInt(partialKey)]);

		// assert up link
		Assert.assertNull(oldChild.parent);
		Assert.assertEquals(node48, newChild.parent);
		Assert.assertEquals(partialKey, newChild.partialKey);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReplaceForNonExistentPartialKey() {
		Node48 node48 = createNode48();
		Node child = Mockito.mock(AbstractNode.class);
		node48.replace((byte) 25, child);
	}

	@Test
	public void testRemove() {
		Node48 node48 = createNode48();
		AbstractNode child1 = Mockito.mock(AbstractNode.class);
		AbstractNode child2 = Mockito.mock(AbstractNode.class);

		byte partialKey1 = 9;
		byte partialKey2 = -9;
		node48.addChild(partialKey1, child1);
		node48.addChild(partialKey2, child2);

		Assert.assertEquals(18, node48.noOfChildren);
		Assert.assertEquals(child1, node48.findChild(partialKey1));
		int keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey1)];
		node48.removeChild(partialKey1);
		Assert.assertNull(node48.findChild(partialKey1));
		Assert.assertEquals(17, node48.noOfChildren);
		// assert internal structure
		Assert.assertEquals(Node48.ABSENT, node48.getKeyIndex()[Byte.toUnsignedInt(partialKey1)]);
		Assert.assertNull(node48.getChild()[keyIndex]);

		Assert.assertEquals(child2, node48.findChild(partialKey2));
		keyIndex = node48.getKeyIndex()[Byte.toUnsignedInt(partialKey2)];
		node48.removeChild(partialKey2);
		Assert.assertNull(node48.findChild(partialKey2));
		Assert.assertEquals(16, node48.noOfChildren);

		// assert internal structure
		Assert.assertEquals(Node48.ABSENT, node48.getKeyIndex()[Byte.toUnsignedInt(partialKey2)]);
		Assert.assertNull(node48.getChild()[keyIndex]);

		// assert up link
		Assert.assertNull(child1.parent);
		Assert.assertNull(child2.parent);
	}

	@Test
	public void testRemoveForNonExistentPartialKey() {
		Node48 node48 = createNode48();
		byte partialKey = 25;
		try {
			node48.removeChild(partialKey);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}
		partialKey = -25;
		try {
			node48.removeChild(partialKey);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

}
