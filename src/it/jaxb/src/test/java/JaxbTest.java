import org.junit.Test;
import org.openclover.xml.schema.Project;

import static org.junit.Assert.assertEquals;

public class JaxbTest {
    @Test
    public void testJaxb() {
        Project project = new Project();
        assertEquals(null, project.getName());
    }
}
