/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.ParameterInfo;
import org.netbeans.modules.csl.spi.ParserResult;

public class VTLCompletionProvider implements CodeCompletionHandler {

	@Override
	public CodeCompletionResult complete(CodeCompletionContext ccc) {
		try {
			ParserResult pr = ccc.getParserResult();
			int caretOffset = ccc.getCaretOffset();
			Document doc = pr.getSnapshot().getSource().getDocument(false);
			if (doc == null) {
				return CodeCompletionResult.NONE;
			}
			String textBefore = doc.getText(0, caretOffset);
			int lineStart = findLineStart(textBefore);
			String line = textBefore.substring(lineStart);
			String prefix = getPrefix(pr, caretOffset, false);

			List<CompletionProposal> proposals = determineCompletions(line, prefix, caretOffset);

			VTLCompletionResult result = new VTLCompletionResult(proposals);
			return result;
		} catch (BadLocationException ex) {
			return CodeCompletionResult.NONE;
		}
	}

	@Override
	public String getPrefix(ParserResult pr, int offset, boolean caseSensitive) {
		try {
			Document doc = pr.getSnapshot().getSource().getDocument(false);
			if (doc == null) return "";
			StringBuilder sb = new StringBuilder();
			int pos = offset - 1;
			while (pos >= 0) {
				char ch = doc.getText(pos, 1).charAt(0);
				if (Character.isJavaIdentifierPart(ch) || ch == '#' || ch == '$') {
					sb.insert(0, ch);
					pos--;
				} else {
					break;
				}
			}
			return sb.toString();
		} catch (BadLocationException ex) {
			return "";
		}
	}

	@Override
	public QueryType getAutoQuery(JTextComponent jtc, String typedText) {
		if ("#".equals(typedText) || "$".equals(typedText)) {
			return QueryType.COMPLETION;
		}
		return QueryType.NONE;
	}

	@Override public String document(ParserResult pr, ElementHandle eh) { return null; }
	@Override public ElementHandle resolveLink(String s, ElementHandle eh) { return null; }
	@Override public String resolveTemplateVariable(String s, ParserResult pr, int i, String s1, Map map) { return null; }
	@Override public Set<String> getApplicableTemplates(Document d, int i, int i1) { return null; }
	@Override public ParameterInfo parameters(ParserResult pr, int i, CompletionProposal cp) { return null; }

	private int findLineStart(String textBefore) {
		int lastNewline = textBefore.lastIndexOf('\n');
		return lastNewline >= 0 ? lastNewline + 1 : 0;
	}

	private List<CompletionProposal> determineCompletions(String line, String prefix, int caretOffset) {
		List<CompletionProposal> items = new ArrayList<>();
		if (line.endsWith("#") || line.endsWith("#{") || startsWithDirectivePrefix(prefix)) {
			addDirectives(items, prefix, caretOffset);
		} else if (line.endsWith("$") || startsWithReferencePrefix(prefix)) {
			addReferences(items, prefix, caretOffset);
		} else if (isInExpression(line)) {
			addReferences(items, prefix, caretOffset);
			addKeywords(items, prefix, caretOffset);
			addOperators(items, prefix, caretOffset);
			addBooleanLiterals(items, prefix, caretOffset);
		} else if (isInForeach(line)) {
			addKeywordIn(items, prefix, caretOffset);
		} else {
			addDirectives(items, prefix, caretOffset);
		}
		return items;
	}

	private boolean startsWithDirectivePrefix(String filter) {
		return filter.startsWith("#") && filter.length() > 1;
	}

	private boolean startsWithReferencePrefix(String filter) {
		return filter.startsWith("$") && filter.length() > 1;
	}

	private boolean isInExpression(String line) {
		String t = line.trim();
		return t.contains("=") || t.contains("==") || t.contains("!=") ||
			   t.contains("<") || t.contains(">") || t.contains("&&") || t.contains("||");
	}

	private boolean isInForeach(String line) {
		return line.contains("#foreach") && !line.contains("#end");
	}

	private void addDirectives(List<CompletionProposal> items, String filter, int anchorOffset) {
		boolean hasHash = filter.startsWith("#");
		String search = hasHash ? filter.substring(1) : filter;
		int p = 10;
		if (match("if", search))
			items.add(new VTLCompletionProposal("#if", "#if()\n\n#end", "Conditional directive", ElementKind.OTHER, p, anchorOffset));
		if (match("else", search))
			items.add(new VTLCompletionProposal("#else", "Else branch", ElementKind.OTHER, p + 1));
		if (match("elseif", search))
			items.add(new VTLCompletionProposal("#elseif", "#elseif()\n", "Else if branch", ElementKind.OTHER, p + 2, anchorOffset));
		if (match("end", search))
			items.add(new VTLCompletionProposal("#end", "End block", ElementKind.OTHER, p + 3));
		if (match("foreach", search))
			items.add(new VTLCompletionProposal("#foreach", "#foreach($item in $list)\n\n#end", "Loop directive", ElementKind.OTHER, p + 4, anchorOffset));
		if (match("set", search))
			items.add(new VTLCompletionProposal("#set", "#set($var = value)", "Variable assignment", ElementKind.OTHER, p + 5, anchorOffset));
		if (match("macro", search))
			items.add(new VTLCompletionProposal("#macro", "#macro(name $arg)\n\n#end", "Macro definition", ElementKind.OTHER, p + 6, anchorOffset));
		if (match("include", search))
			items.add(new VTLCompletionProposal("#include", "#include(\"template.vm\")", "Include template", ElementKind.OTHER, p + 7, anchorOffset));
		if (match("parse", search))
			items.add(new VTLCompletionProposal("#parse", "#parse(\"template.vm\")", "Parse and include template", ElementKind.OTHER, p + 8, anchorOffset));
		if (match("evaluate", search))
			items.add(new VTLCompletionProposal("#evaluate", "#evaluate($expr)", "Evaluate expression", ElementKind.OTHER, p + 9, anchorOffset));
		if (match("define", search))
			items.add(new VTLCompletionProposal("#define", "#define($var)\n\n#end", "Define block", ElementKind.OTHER, p + 10, anchorOffset));
		if (match("stop", search))
			items.add(new VTLCompletionProposal("#stop", "Stop rendering", ElementKind.OTHER, p + 11));
	}

	private void addReferences(List<CompletionProposal> items, String filter, int anchorOffset) {
		boolean hasDollar = filter.startsWith("$");
		String search = hasDollar ? filter.substring(1) : filter;
		if (match("velocityCount", search))
			items.add(new VTLCompletionProposal("$velocityCount", "Loop counter (deprecated)", ElementKind.VARIABLE, 20));
		if (match("velocityHasNext", search))
			items.add(new VTLCompletionProposal("$velocityHasNext", "Loop hasNext flag (deprecated)", ElementKind.VARIABLE, 21));
	}

	private void addKeywords(List<CompletionProposal> items, String filter, int anchorOffset) {
		if (match("in", filter))
			items.add(new VTLCompletionProposal("in", "Foreach keyword", ElementKind.KEYWORD, 50));
		if (match("and", filter))
			items.add(new VTLCompletionProposal("and", "Logical AND (alternative)", ElementKind.KEYWORD, 51));
		if (match("or", filter))
			items.add(new VTLCompletionProposal("or", "Logical OR (alternative)", ElementKind.KEYWORD, 52));
		if (match("not", filter))
			items.add(new VTLCompletionProposal("not", "Logical NOT (alternative)", ElementKind.KEYWORD, 53));
		if (match("eq", filter))
			items.add(new VTLCompletionProposal("eq", "Equals (alternative)", ElementKind.KEYWORD, 54));
		if (match("ne", filter))
			items.add(new VTLCompletionProposal("ne", "Not equals (alternative)", ElementKind.KEYWORD, 55));
		if (match("lt", filter))
			items.add(new VTLCompletionProposal("lt", "Less than (alternative)", ElementKind.KEYWORD, 56));
		if (match("le", filter))
			items.add(new VTLCompletionProposal("le", "Less or equal (alternative)", ElementKind.KEYWORD, 57));
		if (match("gt", filter))
			items.add(new VTLCompletionProposal("gt", "Greater than (alternative)", ElementKind.KEYWORD, 58));
		if (match("ge", filter))
			items.add(new VTLCompletionProposal("ge", "Greater or equal (alternative)", ElementKind.KEYWORD, 59));
	}

	private void addKeywordIn(List<CompletionProposal> items, String filter, int anchorOffset) {
		if (match("in", filter))
			items.add(new VTLCompletionProposal("in", "Foreach keyword", ElementKind.KEYWORD, 5));
	}

	private void addOperators(List<CompletionProposal> items, String filter, int anchorOffset) {
		if (filter.isEmpty() || filter.startsWith("!"))
			items.add(new VTLCompletionProposal("!", "Logical NOT", ElementKind.OTHER, 40, anchorOffset));
		if (filter.isEmpty() || filter.startsWith("&&"))
			items.add(new VTLCompletionProposal("&&", "Logical AND", ElementKind.OTHER, 41, anchorOffset));
		if (filter.isEmpty() || filter.startsWith("||"))
			items.add(new VTLCompletionProposal("||", "Logical OR", ElementKind.OTHER, 42, anchorOffset));
		if (filter.isEmpty() || filter.startsWith("=="))
			items.add(new VTLCompletionProposal("==", "Equals", ElementKind.OTHER, 43, anchorOffset));
		if (filter.isEmpty() || filter.startsWith("!="))
			items.add(new VTLCompletionProposal("!=", "Not equals", ElementKind.OTHER, 44, anchorOffset));
	}

	private void addBooleanLiterals(List<CompletionProposal> items, String filter, int anchorOffset) {
		if (match("true", filter))
			items.add(new VTLCompletionProposal("true", "Boolean true", ElementKind.KEYWORD, 60, anchorOffset));
		if (match("false", filter))
			items.add(new VTLCompletionProposal("false", "Boolean false", ElementKind.KEYWORD, 61, anchorOffset));
	}

	private boolean match(String item, String filter) {
		return item.toLowerCase().startsWith(filter.toLowerCase());
	}
}
