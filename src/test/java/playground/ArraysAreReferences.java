package playground;

import org.junit.Assert;
import org.junit.Test;

public class ArraysAreReferences {

	@Test
	public void test(){
		int a[] = new int[1];
		modify(a);
		Assert.assertEquals(100, a[0]);
	}

	private void modify(int[] a){
		a[0] = 100;
	}
}
