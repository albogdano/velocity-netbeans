/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import com.erudika.netbeans.velocity.parser.VTLParser;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.openide.filesystems.FileObject;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

public class VTLCompletionProvider implements CompletionProvider {

	@Override
	public CompletionTask createTask(int queryType, JTextComponent component) {
		if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE
				&& queryType != CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {
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

	static boolean isVelocityDocument(JTextComponent component) {
		if (component == null) {
			return false;
		}
		return isVelocityDocument(component.getDocument());
	}

	static boolean isVelocityDocument(Document document) {
		if (document == null) {
			return false;
		}

		Object mimeType = document.getProperty("mimeType");
		if (VTLParser.VTL_MIME_TYPE.equals(mimeType)) {
			return true;
		}

		Object streamDescription = document.getProperty(Document.StreamDescriptionProperty);
		if (streamDescription instanceof org.openide.loaders.DataObject dataObject) {
			FileObject primaryFile = dataObject.getPrimaryFile();
			return primaryFile != null && VTLParser.VTL_MIME_TYPE.equals(primaryFile.getMIMEType());
		}
		if (streamDescription instanceof FileObject fileObject) {
			return VTLParser.VTL_MIME_TYPE.equals(fileObject.getMIMEType());
		}

		return false;
	}
}
