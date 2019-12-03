package playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ModifyMapWhenIterating {

    @Test
    public void test() {
        Object o = new Object();
        NavigableMap<String, Object> m = new TreeMap<>();
        m.put("a", o);
        m.put("b", o);
        m.put("c", o);
        Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator();
        Assertions.assertThrows(ConcurrentModificationException.class, () -> {
            while (it.hasNext()) {
                it.next();
                // modify map while in iteration
                m.put("d", o); // should throw ConcurrentModificationException
            }
        });
    }
}
