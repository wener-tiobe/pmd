/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.perl;

import net.sourceforge.pmd.cpd.AnyTokenizer;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.lang.LanguagePropertyBundle;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.impl.CpdOnlyLanguageModuleBase;

public class PerlLanguageModule extends CpdOnlyLanguageModuleBase {
    private static final String ID = "perl";

    public PerlLanguageModule() {
        super(LanguageMetadata.withId(ID).name("Perl").extensions("pm", "pl", "t"));
    }

    public static PerlLanguageModule getInstance() {
        return (PerlLanguageModule) LanguageRegistry.CPD.getLanguageById(ID);
    }

    @Override
    public Tokenizer createCpdTokenizer(LanguagePropertyBundle bundle) {
        return new AnyTokenizer("#");
    }
}
