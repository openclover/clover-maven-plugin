public class Simple
{
    public void someMethodNotCovered()
    {
        System.out.println("Never called");
    }

    public void someMethodCoveredByTest()
    {
        System.out.println("Called by Maven-Surefire-Plugin in 'test' phase");
    }

    public void someMethodCoveredByIntegrationTest()
    {                
        System.out.println("Called by Maven-Failsafe-Plugin in 'integration-test' phase");
    }

    public void someMethodCoveredByBoth() {
        System.out.println("Called twice");
    }
} 