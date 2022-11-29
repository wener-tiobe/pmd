/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.internal;

import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.symbols.JMethodSymbol;
import net.sourceforge.pmd.lang.java.symbols.SymbolicValue;
import net.sourceforge.pmd.lang.java.symbols.SymbolicValue.SymAnnot;

/**
 * Pretends to be an annotation with no explicit attributes.
 *
 * @author Clément Fournier
 */
public class FakeSymAnnot implements SymAnnot {

    private final JClassSymbol annotationClass;

    public FakeSymAnnot(JClassSymbol annotationClass) {
        this.annotationClass = annotationClass;
        assert annotationClass.isAnnotation() : "Not an annotation " + annotationClass;
    }

    @Override
    public @Nullable SymbolicValue getAttribute(String attrName) {
        return annotationClass.getDeclaredMethods().stream().filter(it -> it.nameEquals(attrName))
                              .findFirst()
                              .map(JMethodSymbol::getDefaultAnnotationValue).orElse(null);
    }

    @Override
    public Set<String> getAttributeNames() {
        return Collections.emptySet();
    }

    @Override
    public String getBinaryName() {
        return annotationClass.getBinaryName();
    }

    @Override
    public String getSimpleName() {
        return annotationClass.getSimpleName();
    }

    @Override
    public RetentionPolicy getRetention() {
        return annotationClass.getAnnotationRetention();
    }

    @Override
    public String toString() {
        return "@" + annotationClass.getCanonicalName();
    }
}
