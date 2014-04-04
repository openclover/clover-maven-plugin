import org.junit.Test

import static org.junit.Assert.assertEquals

class GroovyTest {
    @Test
    void testHello() {
        assertEquals "hello-groovy", new GroovyMain().hello()
    }
}
