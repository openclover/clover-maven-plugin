import junit.framework.TestCase;
import com.sun.enterprise.config.serverbeans.AccessLog;

/**
 */
public class JaxbTest extends TestCase {

    public void testJaxb() {
        AccessLog accessLog = new AccessLog();
        assertEquals("", accessLog.getFormat());
    }

}
