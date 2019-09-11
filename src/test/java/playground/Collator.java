package playground;

import java.text.CollationKey;
import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class Collator {

	@Test
	public void testICU(){
		String str = "hello";
		java.text.Collator col = java.text.Collator.getInstance(Locale.US);;
		CollationKey ck = col.getCollationKey(str);
		byte[] bytes = ck.toByteArray();
		System.out.println("collation key bytes length: " + bytes.length);
		System.out.println("collation key bytes: " + Arrays.toString(bytes));
		System.out.println("collation key as string:" + new String(bytes));
		System.out.println("original string: " + Arrays.toString(str.getBytes()));
	}

	@Test
	public void testUTF8SortingDefault(){
		String barca = "bárca";
		String barcelona = "barcelona";

		String s[] = new String[]{barca, barcelona};
		Arrays.sort(s);
		System.out.println(Arrays.toString(s));
	}

	@Test
	public void testUTF8SortingICU(){
		String barca = "bárca";
		String barcelona = "barcelona";

		java.text.Collator col = java.text.Collator.getInstance(Locale.US);

		String s[] = new String[]{barca, barcelona};
		Arrays.sort(s, col);
		System.out.println(Arrays.toString(s));
	}
}
