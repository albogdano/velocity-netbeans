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
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.netbeans.velocity.completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.spi.DefaultCompletionResult;
import org.openide.util.Exceptions;

/**
 * VTLCompletionResult.
 */
public class VTLCompletionResult extends DefaultCompletionResult {

	private final CodeCompletionContext context;

	public VTLCompletionResult(CodeCompletionContext completionContext) {
		super(new ArrayList<CompletionProposal>(), false);
		context = completionContext;
	}

	public void addAll(final Collection<CompletionProposal> proposals) {
		list.addAll(proposals);
	}

	public void add(CompletionProposal proposal) {
		list.add(proposal);
	}

	@Override
	public void afterInsert(CompletionProposal cp) {
		Document doc = context.getParserResult().getSnapshot().getSource().getDocument(true);
		try {
			// Remove typed characters
			Logger.getLogger(VTLCompletionResult.class.getName()).finest(doc.getText(cp.getAnchorOffset() - context.getPrefix().length(), context.getPrefix().length()));
			doc.remove(cp.getAnchorOffset() - context.getPrefix().length(), context.getPrefix().length());
		} catch (BadLocationException ex) {
			Exceptions.printStackTrace(ex);
		}
	}

}
