package com.atlassian.maven.plugin.clover.internal.scanner;

import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Source scanner which searches for main source code root plus one hardcoded 'src/main/groovy' location
 */
public class GroovyMainSourceScanner extends AbstractSourceScanner {

    public static final String SRC_MAIN_GROOVY = "src" + File.separator + "main" + File.separator + "groovy";

    /**
     * From a list of provided <code>sourceRoots</code> remove those which specific for this scanner
     * (SRC_MAIN_GROOVY).
     * @param sourceRoots
     * @see #SRC_MAIN_GROOVY
     * @see #getSourceFilesToInstrument()
     */
    public static void removeOwnSourceRoots(final Set/*<String>*/ sourceRoots) {
        for (final Iterator/*<String>*/ iter = sourceRoots.iterator(); iter.hasNext(); ) {
            final String sourceRoot = (String)iter.next();
            if (sourceRoot.endsWith(SRC_MAIN_GROOVY)) {
                iter.remove();
            }
        }
    }

    public GroovyMainSourceScanner(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        super(configuration, outputSourceDirectory);
    }

    /**
     * @return List&lt;String&gt; - 'src/main/groovy'
     */
    protected List/*<String>*/ getSourceRoots() {
        final List/*<String>*/ roots = new ArrayList/*<String>*/(getConfiguration().getProject().getCompileSourceRoots());
        roots.add(SRC_MAIN_GROOVY);
        return roots;
    }

    /**
     * @return String - usually 'src/main' in order to catch both 'src/main/java' and 'src/main/groovy'
     */
    protected String getSourceDirectory() {
        return new File(getConfiguration().getProject().getBuild().getSourceDirectory()).getParent();
    }
}
