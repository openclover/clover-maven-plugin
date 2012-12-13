import junit.framework.TestCase;

public class SimpleTests extends TestCase
{
    public void testSomeMethodA()
    {        
        Simple simple = new Simple();
        simple.someMethodCoveredByTest();
        simple.someMethodCoveredByBoth();
    }

    public void testSomeMethodB()
    {
        Simple simple = new Simple();
        simple.someMethodCoveredByTest();
        simple.someMethodCoveredByBoth();
    }
} 