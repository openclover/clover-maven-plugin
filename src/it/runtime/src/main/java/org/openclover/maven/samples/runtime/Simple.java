package org.openclover.maven.samples.runtime;

public class Simple {
    public void someMethod() {
        assert true == true : "true was not true";

        int i = 0;
        if (i > 0) {
            i = i + 1;
        }
    }
}
