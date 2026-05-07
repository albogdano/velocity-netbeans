/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.embedding;

import com.erudika.netbeans.velocity.lexer.VTLTokenId;
import com.erudika.netbeans.velocity.parser.VTLParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;

/**
 * Embedding provider that creates HTML embeddings for VTL TEXT tokens.
 * This enables HTML syntax highlighting, code completion, and other
 * HTML editor features within Velocity template files.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
@EmbeddingProvider.Registration(mimeType = VTLParser.VTL_MIME_TYPE, targetMimeType = "text/html")
public class HTMLEmbeddingProvider extends EmbeddingProvider
{
    public static final String TARGET_MIME_TYPE = "text/html";

    /**
     * {@inheritDoc}
     * Scans the VTL token hierarchy for TEXT tokens and creates
     * HTML embeddings for each one.
     */
    @Override
    public List<Embedding> getEmbeddings(final Snapshot snapshot)
    {
        final TokenHierarchy<?> tokenHierarchy = snapshot.getTokenHierarchy();
        final TokenSequence<?> ts = tokenHierarchy.tokenSequence();

        if (ts == null || !ts.isValid())
        {
            return Collections.emptyList();
        }

        final List<Embedding> embeddings = new ArrayList<Embedding>();

        ts.moveStart();
        try
        {
            while (ts.moveNext())
            {
                final Token<?> token = ts.token();

                if (token.id() instanceof VTLTokenId)
                {
                    final VTLTokenId vtlId = (VTLTokenId) token.id();
                    if ("TEXT".equals(vtlId.name()) && token.text() != null && token.text().length() > 0)
                    {
                        final int offset = ts.offset();
                        final int length = token.length();

                        final CharSequence fullText = snapshot.getText();
                        final CharSequence text = fullText.subSequence(offset, offset + length);
                        if (HTMLEmbeddingSupport.containsHtmlContent(text))
                        {
                            embeddings.add(snapshot.create(offset, length, TARGET_MIME_TYPE));
                        }
                    }
                }
            }
        }
        catch (final Exception ex)
        {
            return Collections.emptyList();
        }

        if (embeddings.isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.singletonList(Embedding.create(embeddings));
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority()
    {
        return 100;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel()
    {
    }
}
