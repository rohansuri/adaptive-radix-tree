package art;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class Node48UnitTest {

	@Test(expected = IllegalArgumentException.class)
	public void testUnderCapacityCreation(){
		Node4 node4 = new Node4();
		Node16 node16 = new Node16(node4);
		Node48 node48 = new Node48(node16);
	}

	@Test
	public void testGrowingInto(){
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node16.NODE_SIZE];
		Node children[] = new Node[Node16.NODE_SIZE];
		for (int i = 1; i <= Node16.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(Node.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 7] = (byte) -(9 - i);
			children[i + 7] = Mockito.mock(Node.class);
		}

		Node16 node16 = new Node16(node4);

		for (int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++) {
			// add ith partial key
			node16.addChild(partialKeys[i], children[i]);
		}

		Node48 node48 = new Node48(node16);
		Assert.assertEquals(Node16.NODE_SIZE, node48.noOfChildren);
		for(int i = 0; i < node48.noOfChildren; i++){
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

	Node48 createNode48(){
		Node4 node4 = new Node4();

		// partialKeys in lexicographic ascending order
		byte partialKeys[] = new byte[Node16.NODE_SIZE];
		Node children[] = new Node[Node16.NODE_SIZE];
		for (int i = 1; i <= Node16.NODE_SIZE / 2; i++) {
			partialKeys[i - 1] = (byte) i;
			children[i - 1] = Mockito.mock(Node.class);
			// insert first 4 in lexicographic order (-1, -2, -3, -4)
			if (i <= 4) {
				node4.addChild(partialKeys[i - 1], children[i - 1]);
			}

			partialKeys[i + 7] = (byte) -(9 - i);
			children[i + 7] = Mockito.mock(Node.class);
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
	public void testAddOnePartialKey(){
		Node48 node48 = createNode48();
		byte partialKey = 9;
		Node child = Mockito.mock(Node.class);
		Assert.assertTrue(node48.addChild(partialKey, child));
		Assert.assertEquals(17, node48.noOfChildren);
		Assert.assertEquals(child, node48.findChild(partialKey));

		partialKey = -9;
		child = Mockito.mock(Node.class);
		Assert.assertTrue(node48.addChild(partialKey, child));
		Assert.assertEquals(18, node48.noOfChildren);
		Assert.assertEquals(child, node48.findChild(partialKey));
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
		Node child = Mockito.mock(Node.class);
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
		Node child = Mockito.mock(Node.class);

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

}
