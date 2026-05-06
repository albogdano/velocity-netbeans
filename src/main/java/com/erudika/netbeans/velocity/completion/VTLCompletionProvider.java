/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import com.erudika.netbeans.velocity.parser.VTLParser;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

@MimeRegistration(mimeType = VTLParser.VTL_MIME_TYPE, service = CompletionProvider.class)
public class VTLCompletionProvider implements CompletionProvider {

	@Override
	public CompletionTask createTask(int queryType, JTextComponent component) {
		if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
			return null;
		}
		return new AsyncCompletionTask(new VTLCompletionQuery(), component);
	}

	@Override
	public int getAutoQueryTypes(JTextComponent component, String typedText) {
		if ("#".equals(typedText) || "$".equals(typedText)) {
			return CompletionProvider.COMPLETION_QUERY_TYPE;
		}
		return 0;
	}
}
