import junit.framework.TestCase;

public class SimpleIT extends TestCase
{
    public void testIntegration()
    {        
        Simple simple = new Simple();
        simple.someMethodCoveredByIntegrationTest();
        simple.someMethodCoveredByBoth();
    }
}