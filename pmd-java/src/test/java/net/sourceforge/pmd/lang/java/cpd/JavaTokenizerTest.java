/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.cpd;

import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.cpd.CpdLanguageProperties;
import net.sourceforge.pmd.cpd.test.CpdTextComparisonTest;
import net.sourceforge.pmd.cpd.test.LanguagePropertyConfig;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;

class JavaTokenizerTest extends CpdTextComparisonTest {

    JavaTokenizerTest() {
        super(JavaLanguageModule.getInstance(), ".java");
    }

    @Test
    void testCommentsIgnored() {
        doTest("simpleClassWithComments");
    }

    @Test
    void testDiscardedElements() {
        doTest("discardedElements", "_ignore_annots", ignoreAnnotations());
    }

    @Test
    void testDiscardedElementsExceptAnnots() {
        doTest("discardedElements", "_no_ignore_annots");
    }

    @Test
    void testIgnoreBetweenSpecialComments() {
        doTest("specialComments");
    }

    @Test
    void testIgnoreBetweenSpecialAnnotation() {
        doTest("ignoreSpecialAnnotations");
    }

    @Test
    void testIgnoreBetweenSpecialAnnotationAndIgnoreAnnotations() {
        doTest("ignoreSpecialAnnotations", "_ignore_annots", ignoreAnnotations());
    }

    @Test
    void testIgnoreIdentifiersDontAffectConstructors() {
        doTest("ignoreIdentsPreservesCtor", "", ignoreIdents());
    }

    @Test
    void testIgnoreIdentifiersHandlesEnums() {
        doTest("ignoreIdentsPreservesEnum", "", ignoreIdents());
    }

    @Test
    void testIgnoreIdentifiersWithClassKeyword() {
        doTest("ignoreIdentsPreservesClassLiteral", "", ignoreIdents());
    }

    @Test
    void testIgnoreLiterals() {
        doTest("ignoreLiterals", "", ignoreLiterals());
    }

    @Test
    void testNoIgnoreLiterals() {
        doTest("ignoreLiterals", "_noignore");
    }

    @Test
    void testTabWidth() {
        doTest("tabWidth");
    }


    private static LanguagePropertyConfig ignoreAnnotations() {
        return properties(true, false, false);
    }

    private static LanguagePropertyConfig ignoreIdents() {
        return properties(false, false, true);
    }

    private static LanguagePropertyConfig ignoreLiterals() {
        return properties(false, true, false);
    }


    @Override
    public LanguagePropertyConfig defaultProperties() {
        return properties(false, false, false);
    }

    private static LanguagePropertyConfig properties(boolean ignoreAnnotations,
                                                     boolean ignoreLiterals,
                                                     boolean ignoreIdents) {
        return properties -> {
            properties.setProperty(CpdLanguageProperties.CPD_IGNORE_METADATA, ignoreAnnotations);
            properties.setProperty(CpdLanguageProperties.CPD_ANONYMIZE_IDENTIFIERS, ignoreIdents);
            properties.setProperty(CpdLanguageProperties.CPD_ANONYMIZE_LITERALS, ignoreLiterals);
        };
    }


}
