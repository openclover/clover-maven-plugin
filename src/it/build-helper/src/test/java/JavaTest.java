import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaTest {
    @Test
    public void testHello() {
        assertEquals("hello", new JavaMain().hello());
    }
}
