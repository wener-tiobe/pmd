/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd.internal;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.impl.antlr4.AntlrToken;
import net.sourceforge.pmd.lang.ast.impl.antlr4.AntlrTokenManager;
import net.sourceforge.pmd.lang.document.TextDocument;

/**
 * Generic implementation of a {@link Tokenizer} useful to any Antlr grammar.
 */
public abstract class AntlrTokenizer extends TokenizerBase<AntlrToken> {
    @Override
    protected final TokenManager<AntlrToken> makeLexerImpl(TextDocument doc) {
        CharStream charStream = CharStreams.fromString(doc.getText().toString(), doc.getDisplayName());
        return new AntlrTokenManager(getLexerForSource(charStream), doc);
    }

    protected abstract Lexer getLexerForSource(CharStream charStream);

}
