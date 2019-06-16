package art;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Locale;

import static art.BinaryComparableUtils.terminated;
import static art.BinaryComparableUtils.unsigned;

public interface BinaryComparable<K> {
	byte[] get(K key);

	BinaryComparable<Integer> INTEGER = (key) -> unsigned(ByteBuffer.allocate(Integer.BYTES).putInt(key).array());
	BinaryComparable<Long> LONG = (key) -> unsigned(ByteBuffer.allocate(Long.BYTES).putLong(key).array());
	BinaryComparable<Short> SHORT = (key) -> unsigned(ByteBuffer.allocate(Short.BYTES).putShort(key).array());
	BinaryComparable<Byte> BYTE = (key) -> unsigned(ByteBuffer.allocate(Byte.BYTES).put(key).array());
	BinaryComparable<String> ASCII = (key) -> terminated(key.getBytes(StandardCharsets.US_ASCII));

	BinaryComparable<String> UTF8 = new BinaryComparable<String>() {
		private Collator collator = Collator.getInstance(Locale.US);

		public byte[] get(String key) {
			CollationKey ck = collator.getCollationKey(key);
			return terminated(ck.toByteArray());
		}
	};

}

