/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.erudika.netbeans.velocity.completion;

import com.erudika.netbeans.velocity.lexer.VTLTokenId;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.filesystems.FileObject;

final class VTLCompletionQuery extends AsyncCompletionQuery {

	@Override
	protected void query(CompletionResultSet resultSet, Document document, int caretOffset) {
		try {
			String text = document.getText(0, document.getLength());
			String prefix = VTLCompletionEngine.extractPrefix(text, caretOffset);
			boolean allowInCurrentContext = shouldOfferVelocityCompletion(document, caretOffset, prefix);
			FileObject fileObject = extractFileObject(document);

			for (VTLCompletionProposal proposal : VTLCompletionEngine.complete(text, caretOffset, fileObject, allowInCurrentContext)) {
				resultSet.addItem(new VTLCompletionItem(proposal));
			}
		} catch (BadLocationException ex) {
			// Return no completions on invalid offsets.
		} finally {
			resultSet.finish();
		}
	}

	private boolean shouldOfferVelocityCompletion(Document document, int caretOffset, String prefix) {
		if (prefix.startsWith("#") || prefix.startsWith("$")) {
			return true;
		}
		if (isLikelyVelocityContext(document, caretOffset)) {
			return true;
		}

		TokenHierarchy<?> tokenHierarchy = TokenHierarchy.get(document);
		if (tokenHierarchy == null) {
			return true;
		}

		int offset = Math.max(0, caretOffset - 1);
		var sequences = tokenHierarchy.embeddedTokenSequences(offset, false);
		if (sequences.isEmpty()) {
			TokenSequence<?> root = tokenHierarchy.tokenSequence();
			return root != null && root.language() == VTLTokenId.getLanguage();
		}

		TokenSequence<?> deepest = sequences.get(sequences.size() - 1);
		return deepest.language() == VTLTokenId.getLanguage();
	}

	private boolean isLikelyVelocityContext(Document document, int caretOffset) {
		try {
			int start = Math.max(0, caretOffset - 64);
			String context = document.getText(start, caretOffset - start);

			for (int i = context.length() - 1; i >= 0; i--) {
				char ch = context.charAt(i);
				if (ch == '$' || ch == '#') {
					return true;
				}
				if (Character.isWhitespace(ch) || ch == '<' || ch == '>' || ch == '"' || ch == '\'' || ch == ';') {
					return false;
				}
			}
		} catch (BadLocationException ex) {
			return false;
		}

		return false;
	}

	private FileObject extractFileObject(Document document) {
		Object streamDescription = document.getProperty(Document.StreamDescriptionProperty);
		if (streamDescription instanceof org.openide.loaders.DataObject dataObject) {
			return dataObject.getPrimaryFile();
		}
		if (streamDescription instanceof FileObject fileObject) {
			return fileObject;
		}

		Source source = Source.create(document);
		return source != null ? source.getFileObject() : null;
	}
}
