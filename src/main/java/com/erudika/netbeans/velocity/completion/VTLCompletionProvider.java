/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ParameterInfo;
import org.netbeans.modules.csl.spi.ParserResult;

/**
 * Completion provider for VTL (Velocity Template Language) files.
 * Registered for text/x-velocity MIME type.
 */
public class VTLCompletionProvider implements CodeCompletionHandler
{
//    @Override
//    public CompletionTask createTask(final int queryType, final JTextComponent component)
//    {
//        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE)
//        {
//            return null;
//        }
//        return new AsyncCompletionTask(new VTLCompletionQuery());
//    }
//
//    @Override
//    public int getAutoQueryTypes(final JTextComponent component, final String typedText)
//    {
//        if ("#".equals(typedText) || "$".equals(typedText))
//        {
//            return CompletionProvider.COMPLETION_QUERY_TYPE;
//        }
//        return 0;
//    }

	@Override
	public CodeCompletionResult complete(CodeCompletionContext ccc) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String document(ParserResult pr, ElementHandle eh) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ElementHandle resolveLink(String string, ElementHandle eh) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getPrefix(ParserResult pr, int i, boolean bln) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public QueryType getAutoQuery(JTextComponent jtc, String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String resolveTemplateVariable(String string, ParserResult pr, int i, String string1, Map map) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<String> getApplicableTemplates(Document dcmnt, int i, int i1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ParameterInfo parameters(ParserResult pr, int i, CompletionProposal cp) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
