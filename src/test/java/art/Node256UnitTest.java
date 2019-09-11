package art;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Node256UnitTest extends NodeUnitTest {

	Node256UnitTest(){
		super(Node48.NODE_SIZE);
	}

	@Test
	@Override
	public void testGrow(){
		Assertions.assertThrows(IllegalStateException.class, () -> node.grow());
	}
}
