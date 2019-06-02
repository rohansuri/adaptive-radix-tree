package art;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class Node4UnitTest {

	@Test
	public void testEmptyNode(){
		Node4 node4 = new Node4();
		Assert.assertEquals(0, node4.noOfChildren);
		Assert.assertEquals(4, node4.getKeys().length);
		Assert.assertEquals(4, node4.getChild().length);
	}

	@Test
	public void testAddOnePartialKey(){
		Node4 node4 = new Node4();
		Node child = Mockito.mock(Node.class);
		Assert.assertTrue(node4.addChild((byte)1, child));
		Assert.assertEquals(1, node4.noOfChildren);
		Assert.assertEquals(child, node4.findChild((byte)1));

		// assert inserted at correct position
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child, node4.getChild()[0]);
	}

	@Test
	public void testFindForNonExistentPartialKey(){
		Node4 node4 = new Node4();
		Assert.assertNull(node4.findChild((byte)1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddingTheSamePartialKeyAgain(){
		Node4 node4 = new Node4();
		Node child = Mockito.mock(Node.class);
		Assert.assertTrue(node4.addChild((byte)1, child));
		node4.addChild((byte)1, child);
	}

	@Test
	public void testAddTillCapacity(){
		Node4 node4 = new Node4();
		Node child = Mockito.mock(Node.class);

		// add till capacity
		Assert.assertTrue(node4.addChild((byte)1, child));
		Assert.assertTrue(node4.addChild((byte)2, child));
		Assert.assertTrue(node4.addChild((byte)3, child));
		Assert.assertTrue(node4.addChild((byte)4, child));
		Assert.assertEquals(4, node4.noOfChildren);

		// noOfChildren 4 reached, now adds will fail
		Assert.assertFalse(node4.addChild((byte)5, child));
		Assert.assertFalse(node4.addChild((byte)6, child));

	}

	@Test
	public void testGrow(){
		Node4 node4 = new Node4();
		Node children[] = new Node[Node4.NODE_SIZE];
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			Node child = Mockito.mock(Node.class);
			node4.addChild((byte)(i+1), child);
			children[i] = child;
		}

		Node node = node4.grow();
		// assert we grow into next larger node type 16
		Assert.assertTrue(node instanceof Node16);
		Node16 node16 = (Node16)node;

		// assert all partial key mappings exist
		// and in the same sorted order
		Assert.assertEquals(Node4.NODE_SIZE, node16.noOfChildren);
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			Assert.assertEquals(children[i], node16.findChild((byte)(i+1)));
			Assert.assertEquals(children[i], node16.getChild()[i]);

			// we test Node16's order here, since the ctor of Node16 is a copy of Node4's keys, children
			// it's not noOfChildren times addChild
			Assert.assertEquals(i+1, node16.getKeys()[i]);
		}
 	}

	@Test(expected = IllegalStateException.class)
	public void testGrowBeforeFull(){
		Node4 node4 = new Node4();
		node4.grow();
	}

	@Test
	public void testReplace(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);

		node4.addChild((byte)1, child1);
		Assert.assertEquals(child1, node4.findChild((byte)1));
		node4.replace((byte)1, child2);
		Assert.assertEquals(child2, node4.findChild((byte)1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReplaceForNonExistentPartialKey(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		node4.replace((byte)1, child1);
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Descending(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);
		Node child4 = Mockito.mock(Node.class);

		// test those right shifts to create space for the element added

		node4.addChild((byte)4, child4);
		Assert.assertEquals(4, node4.getKeys()[0]);
		Assert.assertEquals(child4, node4.getChild()[0]);

		node4.addChild((byte)3, child3);
		Assert.assertEquals(3, node4.getKeys()[0]);
		Assert.assertEquals(child3, node4.getChild()[0]);
		Assert.assertEquals(4, node4.getKeys()[1]);
		Assert.assertEquals(child4, node4.getChild()[1]);

		node4.addChild((byte)2, child2);
		Assert.assertEquals(2, node4.getKeys()[0]);
		Assert.assertEquals(child2, node4.getChild()[0]);
		Assert.assertEquals(3, node4.getKeys()[1]);
		Assert.assertEquals(child3, node4.getChild()[1]);
		Assert.assertEquals(4, node4.getKeys()[2]);
		Assert.assertEquals(child4, node4.getChild()[2]);

		node4.addChild((byte)1, child1);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);
		Assert.assertEquals(3, node4.getKeys()[2]);
		Assert.assertEquals(child3, node4.getChild()[2]);
		Assert.assertEquals(4, node4.getKeys()[3]);
		Assert.assertEquals(child4, node4.getChild()[3]);
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Ascending(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);
		Node child4 = Mockito.mock(Node.class);

		// test those right shifts to create space for the element added

		node4.addChild((byte)1, child1);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);

		node4.addChild((byte)2, child2);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);

		node4.addChild((byte)3, child3);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);
		Assert.assertEquals(3, node4.getKeys()[2]);
		Assert.assertEquals(child3, node4.getChild()[2]);

		node4.addChild((byte)4, child4);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);
		Assert.assertEquals(3, node4.getKeys()[2]);
		Assert.assertEquals(child3, node4.getChild()[2]);
		Assert.assertEquals(4, node4.getKeys()[3]);
		Assert.assertEquals(child4, node4.getChild()[3]);
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Middle(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);
		Node child4 = Mockito.mock(Node.class);

		// test those right shifts to create space for the element added

		node4.addChild((byte)1, child1);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);

		node4.addChild((byte)4, child4);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(4, node4.getKeys()[1]);
		Assert.assertEquals(child4, node4.getChild()[1]);

		node4.addChild((byte)3, child3);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(3, node4.getKeys()[1]);
		Assert.assertEquals(child3, node4.getChild()[1]);
		Assert.assertEquals(4, node4.getKeys()[2]);
		Assert.assertEquals(child4, node4.getChild()[2]);

		node4.addChild((byte)2, child2);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);
		Assert.assertEquals(3, node4.getKeys()[2]);
		Assert.assertEquals(child3, node4.getChild()[2]);
		Assert.assertEquals(4, node4.getKeys()[3]);
		Assert.assertEquals(child4, node4.getChild()[3]);
	}

	@Test
	public void testRemove(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);

		node4.addChild((byte)1, child1);
		Assert.assertNotNull(node4.findChild((byte)1));
		node4.removeChild((byte)1);
		Assert.assertNull(node4.findChild((byte)1));
		Assert.assertEquals(0, node4.noOfChildren);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveForNonExistentPartialKey(){
		Node4 node4 = new Node4();
		node4.removeChild((byte)1);
	}

	@Test
	public void testRemoveMaintainsOrder_Middle(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);

		node4.addChild((byte)1, child1);
		node4.addChild((byte)2, child2);
		node4.addChild((byte)3, child3);

		// test those left shifts to cover up the removed element

		node4.removeChild((byte)2);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(3, node4.getKeys()[1]);
		Assert.assertEquals(child3, node4.getChild()[1]);

		node4.removeChild((byte)1);
		Assert.assertEquals(3, node4.getKeys()[0]);
		Assert.assertEquals(child3, node4.getChild()[0]);
	}

	@Test
	public void testRemoveMaintainsOrder_Ascending(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);

		node4.addChild((byte)1, child1);
		node4.addChild((byte)2, child2);
		node4.addChild((byte)3, child3);

		// test those left shifts to cover up the removed element

		node4.removeChild((byte)1);
		Assert.assertEquals(2, node4.getKeys()[0]);
		Assert.assertEquals(child2, node4.getChild()[0]);
		Assert.assertEquals(3, node4.getKeys()[1]);
		Assert.assertEquals(child3, node4.getChild()[1]);

		node4.removeChild((byte)2);
		Assert.assertEquals(3, node4.getKeys()[0]);
		Assert.assertEquals(child3, node4.getChild()[0]);
	}
	@Test
	public void testRemoveMaintainsOrder_Descending(){
		Node4 node4 = new Node4();
		Node child1 = Mockito.mock(Node.class);
		Node child2 = Mockito.mock(Node.class);
		Node child3 = Mockito.mock(Node.class);

		node4.addChild((byte)1, child1);
		node4.addChild((byte)2, child2);
		node4.addChild((byte)3, child3);

		// test those left shifts to cover up the removed element

		node4.removeChild((byte)3);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
		Assert.assertEquals(2, node4.getKeys()[1]);
		Assert.assertEquals(child2, node4.getChild()[1]);

		node4.removeChild((byte)2);
		Assert.assertEquals(1, node4.getKeys()[0]);
		Assert.assertEquals(child1, node4.getChild()[0]);
	}
}
