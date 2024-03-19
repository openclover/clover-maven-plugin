package org.openclover.maven.plugin.internal.configuration;

/**
 * Something which has a name, annotations and javadocs. Such as a class or a method.
 */
public class AbstractJavaEntity {

    /** A regex on which to match the name. Optional. */
    private String name;

    /** A regex on which to match the annotation. Optional. */
    private String annotation;

    /** A regex on which to match the javadoc tags. Optional. */
    private String tag;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
