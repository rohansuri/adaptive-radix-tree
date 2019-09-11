package art;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Node4UnitTest extends NodeUnitTest {

	Node4UnitTest() {
		super(2);
	}

	@Override
	@Test
	public void testShrink() {
		Assertions.assertThrows(IllegalStateException.class, () -> node.shrink());
	}
}
