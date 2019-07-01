package art;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class Node4UnitTest {

	@Test
	public void testEmptyNode() {
		Node4 node4 = new Node4();
		Assert.assertEquals(0, node4.noOfChildren);
		Assert.assertEquals(4, node4.getKeys().length);
		Assert.assertEquals(4, node4.getChild().length);
	}


	@Test
	public void testAddPartialKey() {
		Node4 node4 = new Node4();
		Node children[] = new Node[Node4.NODE_SIZE];
		// bitwise lexicographic order: 1, 2, -2, -1
		byte partialKeys[] = new byte[] {1, 2, -2, -1};
		byte storedPartialKeys[] = new byte[Node4.NODE_SIZE];
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			AbstractNode child = Mockito.mock(AbstractNode.class);
			node4.addChild(partialKeys[i], child);

			// assert up links created
			Assert.assertEquals(node4, child.parent);
			Assert.assertEquals(partialKeys[i], child.partialKey);

			children[i] = child;
			// node4 stores all partialKeys as unsigned
			storedPartialKeys[i] = BinaryComparableUtils.unsigned(partialKeys[i]);
		}

		// assert all partial key mappings exist
		// and in the expected bitwise lexicographic sorted order
		Assert.assertEquals(Node4.NODE_SIZE, node4.noOfChildren);
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			Assert.assertEquals(children[i], node4.findChild(partialKeys[i]));
			Assert.assertEquals(children[i], node4.getChild()[i]);
			Assert.assertEquals(storedPartialKeys[i], node4.getKeys()[i]);
		}
	}

	private void testFindForNonExistentPartialKey(Node4 node4, byte partialKey) {
		Assert.assertNull(node4.findChild(partialKey));
	}

	@Test
	public void testFindForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		byte partialKey = -1;
		testFindForNonExistentPartialKey(node4, partialKey);
		partialKey = 1;
		testFindForNonExistentPartialKey(node4, partialKey);
	}

	private void testAddingTheSamePartialKeyAgain(Node4 node4, byte partialKey) {
		Node child = Mockito.mock(AbstractNode.class);
		Assert.assertTrue(node4.addChild(partialKey, child));
		// adding the same partial key would throw an exception
		try {
			node4.addChild(partialKey, child);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
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
		Assert.assertTrue(node4.addChild((byte) 1, child));
		Assert.assertTrue(node4.addChild((byte) 2, child));
		Assert.assertTrue(node4.addChild((byte) -3, child));
		Assert.assertTrue(node4.addChild((byte) -4, child));
		Assert.assertEquals(4, node4.noOfChildren);

		// noOfChildren 4 reached, now adds will fail
		Assert.assertFalse(node4.addChild((byte) 5, child));
		Assert.assertFalse(node4.addChild((byte) -6, child));

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

		Assert.assertTrue(node4.isFull());

		Node node = node4.grow();
		// assert we grow into next larger node type 16
		Assert.assertTrue(node instanceof Node16);
		Node16 node16 = (Node16) node;

		// assert all partial key mappings exist
		// and in the same sorted order
		Assert.assertEquals(Node4.NODE_SIZE, node16.noOfChildren);
		for (int i = 0; i < Node4.NODE_SIZE; i++) {
			Assert.assertEquals(children[i], node16.findChild(partialKeys[i]));
			Assert.assertEquals(children[i], node16.getChild()[i]);

			// we test Node16's order here, since the ctor of Node16 is a copy of Node4's keys, children
			// it's not noOfChildren times addChild
			Assert.assertEquals(storedPartialKeys[i], node16.getKeys()[i]);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testGrowBeforeFull() {
		Node4 node4 = new Node4();
		node4.grow();
	}

	private void testReplace(Node4 node4, byte partialKey) {
		AbstractNode child1 = Mockito.mock(AbstractNode.class);
		AbstractNode child2 = Mockito.mock(AbstractNode.class);
		node4.addChild(partialKey, child1);
		Assert.assertEquals(child1, node4.findChild(partialKey));
		node4.replace(partialKey, child2);
		Assert.assertEquals(child2, node4.findChild(partialKey));

		// assert up links
		Assert.assertNull(child1.parent);
		Assert.assertEquals(node4, child2.parent);
		Assert.assertEquals(partialKey, child2.partialKey);
	}

	@Test
	public void testReplace() {
		Node4 node4 = new Node4();
		byte partialKey = 1;
		testReplace(node4, partialKey);
		partialKey = -1;
		testReplace(node4, partialKey);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testReplaceForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(AbstractNode.class);
		byte partialKey = 1;
		node4.replace(partialKey, child1);
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
				Assert.assertEquals((byte) reversedStoredKeys.get(j), key);
				Assert.assertEquals(reversedChildren.get(j), child);
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
				Assert.assertEquals(storedKeys[j], key);
				Assert.assertEquals(children[j], child);
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

		Assert.assertNotNull(node4.findChild(partialKey1));
		node4.removeChild(partialKey1);
		Assert.assertNull(node4.findChild(partialKey1));
		Assert.assertEquals(1, node4.noOfChildren);

		Assert.assertNotNull(node4.findChild(partialKey2));
		node4.removeChild(partialKey2);
		Assert.assertNull(node4.findChild(partialKey2));
		Assert.assertEquals(0, node4.noOfChildren);

		// assert up link removed
		Assert.assertNull(child1.parent);
		Assert.assertNull(child2.parent);
	}

	@Test
	public void testRemoveForNonExistentPartialKey() {
		Node4 node4 = new Node4();
		byte partialKey = 1;
		try {
			node4.removeChild(partialKey);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
		}

		partialKey = -1;
		try {
			node4.removeChild(partialKey);
			Assert.fail();
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
				Assert.assertEquals(storedKeys[j+i+1], key);
				Assert.assertEquals(children[j+i+1], child);
			}
		}

		Assert.assertEquals(0, node4.noOfChildren);
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
				Assert.assertEquals((byte) reversedStoredKeys.get(j), key);
				Assert.assertEquals(reversedChildren.get(j), child);
			}
		}

		Assert.assertEquals(0, node4.noOfChildren);
	}
}
