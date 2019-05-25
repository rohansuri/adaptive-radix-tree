package playground;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class IsDefaultCtorChainedByDefault {

	@Test
	public void Test(){
		Foo f1 = new Foo(1);
		Assert.assertEquals(1, f1.msgs.size());
	}

	private class Foo {
		List<String> msgs = new ArrayList<>();

		public Foo(){
			msgs.add("default ctor called");
		}

		public Foo(int arg){
			msgs.add("ctor with some arg");
		}
	}

}
