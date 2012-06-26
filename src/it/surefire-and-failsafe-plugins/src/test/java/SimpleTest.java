import junit.framework.TestCase;

public class SimpleTest extends TestCase
{
    public void testSomeMethod()
    {        
        Simple simple = new Simple();
        simple.someMethodCoveredByTest();
        simple.someMethodCoveredByBoth();
    }
} 