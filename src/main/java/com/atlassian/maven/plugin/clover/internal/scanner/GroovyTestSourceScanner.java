package com.atlassian.maven.plugin.clover.internal.scanner;

import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Source scanner which searches for test code source roots plus one hardcoded 'src/test/groovy'.
 */
public class GroovyTestSourceScanner extends AbstractSourceScanner {

    public static final String SRC_TEST_GROOVY = "src" + File.separator + "test" + File.separator + "groovy";

    /**
     * From a list of provided <code>sourceRoots</code> remove those which specific for this scanner
     * (SRC_TEST_GROOVY).
     * @param sourceRoots
     * @see #SRC_TEST_GROOVY
     * @see #getSourceFilesToInstrument()
     */
    public static void removeOwnSourceRoots(final Set/*<String>*/ sourceRoots) {
        for (final Iterator/*<String>*/ iter = sourceRoots.iterator(); iter.hasNext(); ) {
            final String sourceRoot = (String)iter.next();
            if (sourceRoot.endsWith(SRC_TEST_GROOVY)) {
                iter.remove();
            }
        }
    }

    public GroovyTestSourceScanner(final CompilerConfiguration configuration, final String outputDirectory) {
        super(configuration, outputDirectory);
    }

    protected List/*<String>*/ getSourceRoots() {
        final List/*<String>*/ roots = new ArrayList/*<String>*/(getConfiguration().getProject().getTestCompileSourceRoots());
        roots.add(SRC_TEST_GROOVY);
        return roots;
    }

    protected String getSourceDirectory() {
        return getConfiguration().getProject().getBuild().getTestSourceDirectory();
    }
}
