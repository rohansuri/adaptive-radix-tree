package art;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class Node16UnitTest {
	private Node16 createNode16(){
		return new Node16(createFullCapacityNode4());
	}

	private Node4 createFullCapacityNode4(){
		Node4 node4 = new Node4();
		node4.addChild((byte)1, Mockito.mock(Node.class));
		node4.addChild((byte)2, Mockito.mock(Node.class));
		node4.addChild((byte)3, Mockito.mock(Node.class));
		node4.addChild((byte)4, Mockito.mock(Node.class));
		return node4;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnderCapacityCreation(){
		Node4 node4 = new Node4();
		Node16 node16 = new Node16(node4);
	}

	@Test
	public void testGrowingInto(){
		Node4 node4 = new Node4();
		Node children[] = new Node[Node4.NODE_SIZE];
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			children[i] = Mockito.mock(Node.class);
			node4.addChild((byte)i, children[i]);
		}
		Node16 node16 = new Node16(node4);
		Assert.assertEquals(Node4.NODE_SIZE, node16.noOfChildren);
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			Assert.assertEquals(BinaryComparableUtils.unsigned((byte)i), node16.getKeys()[i]);
			Assert.assertEquals(children[i], node16.getChild()[i]);
		}
	}

	@Test
	public void testAddOnePartialKey(){
		Node16 node16 = createNode16();
		Node child = Mockito.mock(Node.class);
		Assert.assertTrue(node16.addChild((byte)0, child));
		Assert.assertEquals(5, node16.noOfChildren);
		Assert.assertEquals(child, node16.findChild((byte)0));

		// assert inserted at correct position
		Assert.assertEquals(BinaryComparableUtils.unsigned((byte)0), node16.getKeys()[0]);
		Assert.assertEquals(child, node16.getChild()[0]);
	}

	@Test
	public void testFindForNonExistentPartialKey(){
		Node16 node16 = createNode16();
		Assert.assertNull(node16.findChild((byte)5));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddingTheSamePartialKeyAgain(){
		Node16 node16 = createNode16();
		Node child = Mockito.mock(Node.class);
		node16.addChild((byte)1, child);
	}

	@Test
	public void testAddTillCapacity(){
		Node16 node16 = createNode16();
		Node child = Mockito.mock(Node.class);

		// add till capacity
		for(int i = Node4.NODE_SIZE + 1; i <= Node16.NODE_SIZE; i++){
			Assert.assertTrue(node16.addChild((byte)i, child));
		}
		Assert.assertEquals(Node16.NODE_SIZE, node16.noOfChildren);

		// noOfChildren 16 reached, now adds will fail
		Assert.assertFalse(node16.addChild((byte)17, child));
		Assert.assertFalse(node16.addChild((byte)18, child));

	}

	private Node[] getNode4sCopiedOverChildren(Node16 node16){
		Node children[] = new Node[Node16.NODE_SIZE];
		// copy node4's children
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			children[i] = node16.getChild()[i];
		}
		return children;
	}

	private Node[] fillNode16(Node16 node16){
		Node children[] = getNode4sCopiedOverChildren(node16);

		// add children upto remaining capacity
		for(int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++){
			Node child = Mockito.mock(Node.class);
			Assert.assertTrue(node16.addChild((byte)(i+1), child));
			children[i] = child;
		}
		return children;
	}

	@Test
	public void testGrow(){
		Node16 node16 = createNode16();
		Node children[] = fillNode16(node16);

		Node node = node16.grow();
		// assert we grow into next larger node type 48
		Assert.assertTrue(node instanceof Node48);
		Node48 node48 = (Node48)node;

		// assert all partial key mappings exist
		Assert.assertEquals(Node16.NODE_SIZE, node48.noOfChildren);
		for(int i = 0; i < Node16.NODE_SIZE; i++){
			Assert.assertEquals(children[i], node48.findChild((byte)(i+1)));

			// a bit of internal testing for Node48 here, that
			// it's keyIndex is correctly set
			Assert.assertNotEquals(Node48.ABSENT, node48.getKeyIndex()[i+1]);

			// Node48's ctor copies the children into first 16 free slots
			// which for a newly created Node48 would be the first 16 itself.
			// That's what we assert here.
			Assert.assertEquals(children[i], node48.getChild()[i]);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testGrowBeforeFull(){
		Node16 node16 = createNode16();
		node16.grow();
	}

	@Test
	public void testReplace(){
		Node16 node16 = createNode16();
		Node newChild = Mockito.mock(Node.class);

		node16.replace((byte)1, newChild);
		Assert.assertEquals(newChild, node16.findChild((byte)1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReplaceForNonExistentPartialKey(){
		Node16 node16 = createNode16();
		Node child5 = Mockito.mock(Node.class);
		node16.replace((byte)5, child5);
	}

	public void assertKeysInSortedOrder(Node16 node16, Node[] children){
		for(int i = 0; i < node16.noOfChildren; i++){
			Assert.assertEquals((byte)(i+1), node16.getKeys()[i]);
			Assert.assertEquals(children[i], node16.getChild()[i]);
		}
	}

	private Map<Integer, Node> addNode4sChildrenToMap(Node16 node16){
		Map<Integer, Node> map = new HashMap<>();
		for(int i = 0; i < Node4.NODE_SIZE; i++){
			map.put((int)BinaryComparableUtils.signed(node16.getKeys()[i]), node16.getChild()[i]);
		}
		return map;
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Descending(){
		Node4 node4 = new Node4();
		node4.addChild((byte)16, Mockito.mock(Node.class));
		node4.addChild((byte)15, Mockito.mock(Node.class));
		node4.addChild((byte)14, Mockito.mock(Node.class));
		node4.addChild((byte)13, Mockito.mock(Node.class));

		Node16 node16 = new Node16(node4);
		Map<Integer, Node> map = addNode4sChildrenToMap(node16);

		// test those right shifts to create space for the element added

		for(int i = Node16.NODE_SIZE - Node4.NODE_SIZE - 1; i >= 0; i--){
			Node child = Mockito.mock(Node.class);
			node16.addChild((byte)(i+1), child);
			map.put(i+1, child);

			for(int j = 0, k = i; j < node16.noOfChildren; j++, k++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)(k+1)), node16.getKeys()[j]);
				Assert.assertEquals(map.get(k+1), node16.getChild()[j]);
			}
		}
	}

	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Ascending(){
		Node4 node4 = new Node4();
		node4.addChild((byte)1, Mockito.mock(Node.class));
		node4.addChild((byte)2, Mockito.mock(Node.class));
		node4.addChild((byte)3, Mockito.mock(Node.class));
		node4.addChild((byte)4, Mockito.mock(Node.class));

		Node16 node16 = new Node16(node4);
		Map<Integer, Node> map = addNode4sChildrenToMap(node16);

		// test those right shifts to create space for the element added

		for(int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++){
			Node child = Mockito.mock(Node.class);
			node16.addChild((byte)(i+1), child);
			map.put(i+1, child);

			for(int j = 0; j < node16.noOfChildren; j++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)(j+1)), node16.getKeys()[j]);
				Assert.assertEquals(map.get(j+1), node16.getChild()[j]);
			}
		}
	}

	// TODO: Unnecessarily complicated, add a simpler test?
	@Test
	public void testSubsequentAddChildCallsMaintainOrder_Middle(){
		Node4 node4 = new Node4();
		node4.addChild((byte)2, Mockito.mock(Node.class));
		node4.addChild((byte)4, Mockito.mock(Node.class));
		node4.addChild((byte)6, Mockito.mock(Node.class));
		node4.addChild((byte)8, Mockito.mock(Node.class));

		Node16 node16 = new Node16(node4);
		Map<Integer, Node> map = addNode4sChildrenToMap(node16);

		for(int i = 10; i <= Node16.NODE_SIZE; i+=2){
			Node child = Mockito.mock(Node.class);
			node16.addChild((byte)i, child);
			map.put(i, child);
		}

		// start inserting in middle
		for(int i = 1; i < Node16.NODE_SIZE; i+=2){
			Node child = Mockito.mock(Node.class);
			map.put(i, child);
			node16.addChild((byte)i, child);

			// assert odd-even already added
			int j = 1;
			for(;j <= i; j++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)j), node16.getKeys()[j-1]);
				Assert.assertEquals(map.get(j), node16.getChild()[j-1]);
			}
			// assert even
			for(int k = j; k <= node16.noOfChildren; j+=2, k++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)j), node16.getKeys()[k-1]);
				Assert.assertEquals(map.get(j), node16.getChild()[k-1]);
			}
		}
	}

	@Test
	public void testRemove(){
		Node16 node16 = createNode16();
		Node child5 = Mockito.mock(Node.class);

		node16.addChild((byte)5, child5);
		Assert.assertEquals(5, node16.noOfChildren);
		Assert.assertEquals(child5, node16.findChild((byte)5));
		node16.removeChild((byte)5);
		Assert.assertNull(node16.findChild((byte)5));
		Assert.assertEquals(4, node16.noOfChildren);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveForNonExistentPartialKey(){
		Node16 node16 = createNode16();
		node16.removeChild((byte)5);
	}

	// TODO: Unnecessarily complicated, add a simpler test?
	@Test
	public void testRemoveMaintainsOrder_Middle(){
		Node16 node16 = createNode16();
		fillNode16(node16);
		Map<Integer, Node> map = new HashMap<>();
		for(int i = 0; i < Node16.NODE_SIZE; i++){
			map.put((int)BinaryComparableUtils.signed(node16.getKeys()[i]), node16.getChild()[i]);
		}

		// remove odds
		for(int i = 1; i < Node16.NODE_SIZE; i+=2){
			node16.removeChild((byte)i);

			// assert only evens
			int j = 0, k = 2;
			for(; k <= (i+1); j++, k+=2){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)k), node16.getKeys()[j]);
				Assert.assertEquals(map.get(k), node16.getChild()[j]);
			}

			// assert even-odd alternate
			for(k-- ; j < node16.noOfChildren; j++, k++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)k), node16.getKeys()[j]);
				Assert.assertEquals(map.get(k), node16.getChild()[j]);
			}
		}
	}

	@Test
	public void testRemoveMaintainsOrder_Ascending(){
		Node4 node4 = new Node4();
		node4.addChild((byte)1, Mockito.mock(Node.class));
		node4.addChild((byte)2, Mockito.mock(Node.class));
		node4.addChild((byte)3, Mockito.mock(Node.class));
		node4.addChild((byte)4, Mockito.mock(Node.class));

		Node16 node16 = new Node16(node4);
		Map<Integer, Node> map = addNode4sChildrenToMap(node16);
		for(int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++){
			Node child = Mockito.mock(Node.class);
			map.put(i+1, child);
			node16.addChild((byte)(i+1), child);
		}

		for(int i = 0; i < Node16.NODE_SIZE; i++){
			node16.removeChild((byte)(i+1));

			for(int j = 0, k = i+2; j < node16.noOfChildren; j++, k++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)(k)), node16.getKeys()[j]);
				Assert.assertEquals(map.get(k), node16.getChild()[j]);
			}
		}
	}
	@Test
	public void testRemoveMaintainsOrder_Descending(){
		Node4 node4 = new Node4();
		node4.addChild((byte)1, Mockito.mock(Node.class));
		node4.addChild((byte)2, Mockito.mock(Node.class));
		node4.addChild((byte)3, Mockito.mock(Node.class));
		node4.addChild((byte)4, Mockito.mock(Node.class));

		Node16 node16 = new Node16(node4);
		Map<Integer, Node> map = addNode4sChildrenToMap(node16);
		for(int i = Node4.NODE_SIZE; i < Node16.NODE_SIZE; i++){
			Node child = Mockito.mock(Node.class);
			map.put(i+1, child);
			node16.addChild((byte)(i+1), child);
		}

		for(int i = Node16.NODE_SIZE; i > 0; i--){
			node16.removeChild((byte)i);

			for(int j = 0; j < node16.noOfChildren; j++){
				Assert.assertEquals(BinaryComparableUtils.unsigned((byte)(j+1)), node16.getKeys()[j]);
				Assert.assertEquals(map.get(j+1), node16.getChild()[j]);
			}
		}
	}
}
