import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaExtraTest {
    @Test
    public void testHello() {
        assertEquals("hello-extra", new JavaExtraMain().hello());
    }
}
