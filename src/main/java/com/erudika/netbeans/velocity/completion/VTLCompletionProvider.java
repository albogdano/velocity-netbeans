/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import javax.swing.text.JTextComponent;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

/**
 * Completion provider for VTL (Velocity Template Language) files.
 * Registered for text/x-velocity MIME type.
 */
public class VTLCompletionProvider implements CompletionProvider
{
    @Override
    public CompletionTask createTask(final int queryType, final JTextComponent component)
    {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE)
        {
            return null;
        }
        return new AsyncCompletionTask(new VTLCompletionQuery());
    }

    @Override
    public int getAutoQueryTypes(final JTextComponent component, final String typedText)
    {
        if ("#".equals(typedText) || "$".equals(typedText))
        {
            return CompletionProvider.COMPLETION_QUERY_TYPE;
        }
        return 0;
    }
}
