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

import com.erudika.netbeans.velocity.jcclexer.Directive;
import com.erudika.netbeans.velocity.jcclexer.ParseException;
import com.erudika.netbeans.velocity.jcclexer.SimpleCharStream;
import com.erudika.netbeans.velocity.jcclexer.Token;
import com.erudika.netbeans.velocity.jcclexer.TokenMgrError;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserTokenManager;
import com.erudika.netbeans.velocity.jcclexer.node.ASTForEachStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTIdentifier;
import com.erudika.netbeans.velocity.jcclexer.node.ASTMacroStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTReference;
import com.erudika.netbeans.velocity.jcclexer.node.ASTSetDirective;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

final class VTLCompletionEngine {

	private static final List<DirectiveTemplate> DIRECTIVES = List.of(
			new DirectiveTemplate("#set", "#set($var = value)", "Variable assignment"),
			new DirectiveTemplate("#macro", "#macro( $arg)\n\n#end", "Macro definition"),
			new DirectiveTemplate("#if", "#if()\n\n#end", "Conditional directive"),
			new DirectiveTemplate("#else", "#else", "Else branch"),
			new DirectiveTemplate("#elseif", "#elseif()", "Else-if branch"),
			new DirectiveTemplate("#end", "#end", "End block"),
			new DirectiveTemplate("#foreach", "#foreach($item in $list)\n\n#end", "Loop directive"),
			new DirectiveTemplate("#include", "#include(\"template.vm\")", "Include template"),
			new DirectiveTemplate("#parse", "#parse(\"template.vm\")", "Parse template"),
			new DirectiveTemplate("#evaluate", "#evaluate($expr)", "Evaluate expression"),
			new DirectiveTemplate("#define", "#define($var)\n\n#end", "Define block"),
			new DirectiveTemplate("#break", "#break", "Break directive"),
			new DirectiveTemplate("#stop", "#stop", "Stop rendering"),
			new DirectiveTemplate("#comment #* .. *#", "#*  *# ", "Comment block")
	);

	private static final List<String> KEYWORDS = List.of("in", "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge");
	private static final List<String> BOOLEANS = List.of("true", "false");
	private static final List<String> OPERATORS = List.of("!", "&&", "||", "==", "!=", "<", "<=", ">", ">=");
	private static final List<VelocityContextSymbol> BUILT_IN_REFERENCES = List.of(
			new VelocityContextSymbol("$foreach.count", "Loop counter"),
			new VelocityContextSymbol("$foreach.index", "Loop index"),
			new VelocityContextSymbol("$foreach.first", "Loop first"),
			new VelocityContextSymbol("$foreach.last", "Loop last"),
			new VelocityContextSymbol("$foreach.parent", "Loop parent"),
			new VelocityContextSymbol("$foreach.topmost", "Loop topmost"),
			new VelocityContextSymbol("$foreach.hasNext", "Loop hasNext flag"));

	private VTLCompletionEngine() {
	}

	static List<VTLCompletionProposal> complete(String text, int caretOffset, FileObject fileObject, boolean allowInCurrentContext) {
		CompletionContext context = CompletionContext.analyze(text, caretOffset);
		if (!allowInCurrentContext && !context.hasVelocityPrefix()) {
			return List.of();
		}

		TemplateSymbols symbols = collectSymbols(text);
		LinkedHashMap<String, VTLCompletionProposal> proposals = new LinkedHashMap<String, VTLCompletionProposal>();

		switch (context.mode()) {
			case DIRECTIVE -> addDirectiveProposals(proposals, context, symbols);
			case REFERENCE -> addReferenceProposals(proposals, context, symbols, fileObject);
			case FOREACH_IN -> addKeywordProposal(proposals, "in", "Keyword", context, 10);
			case EXPRESSION -> {
				addReferenceProposals(proposals, context, symbols, fileObject);
				addKeywordProposals(proposals, context);
				addOperatorProposals(proposals, context);
				addBooleanProposals(proposals, context);
			}
			case GENERAL -> {
				addDirectiveProposals(proposals, context, symbols);
				addReferenceProposals(proposals, context, symbols, fileObject);
			}
		}

//		ArrayList<VTLCompletionProposal> values = new ArrayList<VTLCompletionProposal>(proposals.values());
//		values.sort(Comparator.comparingInt(VTLCompletionProposal::getSortPriority)
//				.thenComparing(VTLCompletionProposal::getName, String.CASE_INSENSITIVE_ORDER));
//		return values;
		return proposals.values().stream().distinct().toList();
	}

	static String extractPrefix(String text, int caretOffset) {
		StringBuilder prefix = new StringBuilder();
		int index = caretOffset - 1;
		while (index >= 0) {
			char ch = text.charAt(index);
			if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '$' || ch == '#' || ch == '!') {
				prefix.insert(0, ch);
				index--;
			} else {
				break;
			}
		}
		return prefix.toString();
	}

	private static void addDirectiveProposals(Map<String, VTLCompletionProposal> proposals, CompletionContext context,
			TemplateSymbols symbols) {
		String filter = stripPrefixMarker(context.prefix(), '#');
		for (DirectiveTemplate directive : DIRECTIVES) {
			if (!matches(directive.name(), filter)) {
				continue;
			}

			int priority = switch (directive.name()) {
				case "#end" -> context.hasOpenBlock() ? 5 : 30;
				case "#else", "#elseif" -> context.isInsideIfBlock() ? 6 : 31;
				default -> 20;
			};
			put(proposals, directive.name(), new VTLCompletionProposal(directive.name(), directive.insertText(),
					directive.description(), VTLCompletionItem.ItemType.DIRECTIVE, priority,
					context.replaceOffset(), context.replaceLength()));
		}

		for (String macroName : symbols.macros()) {
			String proposalName = "#" + macroName;
			if (matches(proposalName, filter)) {
				put(proposals, proposalName,
						new VTLCompletionProposal(proposalName, proposalName + "()", "Velocimacro",
								VTLCompletionItem.ItemType.DIRECTIVE, 35, context.replaceOffset(), context.replaceLength()));
			}
		}
	}

	private static void addReferenceProposals(Map<String, VTLCompletionProposal> proposals, CompletionContext context,
			TemplateSymbols symbols, FileObject fileObject) {
		String filter = stripPrefixMarker(context.prefix(), '$');
		addReferenceSymbols(proposals, symbols.declaredReferences(), "Variable", 10, filter, context);
		addReferenceSymbols(proposals, symbols.observedReferences(), "Context variable", 20, filter, context);

		for (VelocityContextSymbol builtIn : BUILT_IN_REFERENCES) {
			String normalized = normalizeReference(builtIn.name());
			if (normalized != null && matches(normalized, filter)) {
				put(proposals, normalized, new VTLCompletionProposal(normalized, normalized, builtIn.description(),
						VTLCompletionItem.ItemType.REFERENCE, 25, context.replaceOffset(), context.replaceLength()));
			}
		}

		for (String configuredSymbol : VTLCompletionSettings.getConfiguredSymbols()) {
			String normalized = normalizeReference(configuredSymbol);
			if (normalized != null && matches(normalized, filter)) {
				put(proposals, normalized, new VTLCompletionProposal(normalized, normalized, "Context variable",
						VTLCompletionItem.ItemType.REFERENCE, 30, context.replaceOffset(), context.replaceLength()));
			}
		}

		for (VelocityContextSymbolProvider provider : Lookup.getDefault().lookupAll(VelocityContextSymbolProvider.class)) {
			Collection<VelocityContextSymbol> provided = provider.getSymbols(fileObject);
			if (provided == null) {
				continue;
			}
			for (VelocityContextSymbol symbol : provided) {
				String normalized = normalizeReference(symbol.name());
				if (normalized != null && matches(normalized, filter)) {
					put(proposals, normalized, new VTLCompletionProposal(normalized, normalized,
							symbol.description() != null ? symbol.description() : "App Context variable",
							VTLCompletionItem.ItemType.REFERENCE, 30, context.replaceOffset(), context.replaceLength()));
				}
			}
		}
	}

	private static void addReferenceSymbols(Map<String, VTLCompletionProposal> proposals, Set<String> symbols,
			String description, int priority, String filter, CompletionContext context) {
		for (String symbol : symbols) {
			if (matches(symbol, filter)) {
				put(proposals, symbol, new VTLCompletionProposal(symbol, symbol, description,
						VTLCompletionItem.ItemType.REFERENCE, priority, context.replaceOffset(), context.replaceLength()));
			}
		}
	}

	private static void addKeywordProposals(Map<String, VTLCompletionProposal> proposals, CompletionContext context) {
		String filter = context.prefix();
		for (String keyword : KEYWORDS) {
			if (matches(keyword, filter)) {
				put(proposals, keyword, new VTLCompletionProposal(keyword, keyword, "Keyword",
						VTLCompletionItem.ItemType.KEYWORD, 40, context.replaceOffset(), context.replaceLength()));
			}
		}
	}

	private static void addOperatorProposals(Map<String, VTLCompletionProposal> proposals, CompletionContext context) {
		String filter = context.prefix();
		for (String operator : OPERATORS) {
			if (filter.isEmpty() || operator.startsWith(filter)) {
				put(proposals, operator, new VTLCompletionProposal(operator, operator, "Operator",
						VTLCompletionItem.ItemType.OPERATOR, 50, context.replaceOffset(), context.replaceLength()));
			}
		}
	}

	private static void addBooleanProposals(Map<String, VTLCompletionProposal> proposals, CompletionContext context) {
		String filter = context.prefix();
		for (String value : BOOLEANS) {
			if (matches(value, filter)) {
				put(proposals, value, new VTLCompletionProposal(value, value, "Boolean",
						VTLCompletionItem.ItemType.KEYWORD, 45, context.replaceOffset(), context.replaceLength()));
			}
		}
	}

	private static void addKeywordProposal(Map<String, VTLCompletionProposal> proposals, String keyword,
			String description, CompletionContext context, int priority) {
		if (matches(keyword, context.prefix())) {
			put(proposals, keyword, new VTLCompletionProposal(keyword, keyword, description,
					VTLCompletionItem.ItemType.KEYWORD, priority, context.replaceOffset(), context.replaceLength()));
		}
	}

	private static void put(Map<String, VTLCompletionProposal> proposals, String key, VTLCompletionProposal proposal) {
		VTLCompletionProposal existing = proposals.get(key);
		if (existing == null || proposal.getSortPriority() < existing.getSortPriority()) {
			proposals.put(key, proposal);
		}
	}

	private static TemplateSymbols collectSymbols(String text) {
		VelocityParser parser = new VelocityParser();
		parser.addDirective("parse", new Directive(Directive.LINE));
		parser.addDirective("evaluate", new Directive(Directive.LINE));
		parser.addDirective("define", new Directive(Directive.BLOCK));

		TemplateSymbols symbols = new TemplateSymbols();
		try {
			SimpleNode root = parser.parse(new StringReader(text), "completion.vm");
			if (root != null) {
				SymbolCollector collector = new SymbolCollector(symbols);
				collector.openTransaction();
				collector.visit(root, null);
				collector.commitTransaction();
			}
		} catch (ParseException ex) {
			// Completion should degrade gracefully if the document is mid-edit.
		}
		collectLexicalSymbols(text, symbols);
		return symbols;
	}

	private static void collectLexicalSymbols(String text, TemplateSymbols symbols) {
		try {
			VelocityParserTokenManager tokenManager = new VelocityParserTokenManager(
					new SimpleCharStream(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), 1, 1));
			boolean expectMacroName = false;
			boolean expectForeachVar = false;
			boolean expectSetTarget = false;
			Token token;
			do {
				token = tokenManager.getNextToken();
				switch (token.kind) {
					case VelocityParserConstants.MACRO_DIRECTIVE:
						expectMacroName = true;
						expectForeachVar = false;
						expectSetTarget = false;
						break;
					case VelocityParserConstants.FOREACH_DIRECTIVE:
						expectForeachVar = true;
						expectMacroName = false;
						expectSetTarget = false;
						break;
					case VelocityParserConstants.SET_DIRECTIVE:
						expectSetTarget = true;
						expectMacroName = false;
						expectForeachVar = false;
						break;
					case VelocityParserConstants.WORD:
						if (expectMacroName && token.image != null && !token.image.isBlank()) {
							symbols.macros.add(token.image.trim());
							expectMacroName = false;
						}
						break;
					case VelocityParserConstants.IDENTIFIER:
						String reference = normalizeReference(token.image);
						if (reference != null) {
							symbols.observedReferences.add(reference);
							if (expectForeachVar || expectSetTarget) {
								symbols.declaredReferences.add(reference);
							}
						}
						expectForeachVar = false;
						expectSetTarget = false;
						break;
					case VelocityParserConstants.RPAREN:
					case VelocityParserConstants.REFMOD2_RPAREN:
					case VelocityParserConstants.NEWLINE:
						expectMacroName = false;
						expectForeachVar = false;
						expectSetTarget = false;
						break;
					default:
						break;
				}
			} while (token.kind != VelocityParserConstants.EOF);
		} catch (TokenMgrError ex) {
			// Ignore incomplete tokens while collecting lexical symbols.
		}
	}

	private static boolean matches(String candidate, String rawFilter) {
		String filter = rawFilter == null ? "" : rawFilter.toLowerCase(Locale.ROOT);
		String comparable = candidate.toLowerCase(Locale.ROOT);
		if (filter.isEmpty()) {
			return true;
		}
		if (comparable.startsWith(filter)) {
			return true;
		}
		if (comparable.startsWith("#") || comparable.startsWith("$")) {
			return comparable.substring(1).startsWith(filter);
		}
		return false;
	}

	private static String stripPrefixMarker(String prefix, char marker) {
		if (!prefix.isEmpty() && prefix.charAt(0) == marker) {
			String stripped = prefix.substring(1);
			return stripped.startsWith("!") ? stripped.substring(1) : stripped;
		}
		return prefix;
	}

	private static String normalizeReference(String raw) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim();
		if (value.isEmpty()) {
			return null;
		}
		if (value.charAt(0) != '$') {
			return "$" + value.replaceFirst("^\\$+", "");
		}

		int index = 1;
		if (index < value.length() && value.charAt(index) == '!') {
			index++;
		}
		if (index < value.length() && value.charAt(index) == '{') {
			index++;
		}

		StringBuilder normalized = new StringBuilder("$");
		while (index < value.length()) {
			char ch = value.charAt(index);
			if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.') {
				normalized.append(ch);
				index++;
			} else {
				break;
			}
		}
		return normalized.length() > 1 ? normalized.toString() : null;
	}

	private record DirectiveTemplate(String name, String insertText, String description) {
	}

	private enum CompletionMode {
		DIRECTIVE,
		REFERENCE,
		FOREACH_IN,
		EXPRESSION,
		GENERAL
	}

	private record CompletionContext(String prefix, int replaceOffset, int replaceLength, CompletionMode mode,
			Integer openBlockKind) {

		static CompletionContext analyze(String text, int caretOffset) {
			String prefix = extractPrefix(text, caretOffset);
			int replaceOffset = caretOffset - prefix.length();
			String textBeforeCaret = text.substring(0, caretOffset);
			String currentLine = extractCurrentLine(textBeforeCaret);
			Integer openBlockKind = findOpenBlock(textBeforeCaret);

			CompletionMode mode;
			if (prefix.startsWith("#")) {
				mode = CompletionMode.DIRECTIVE;
			} else if (prefix.startsWith("$")) {
				mode = CompletionMode.REFERENCE;
			} else if (isForeachInContext(currentLine)) {
				mode = CompletionMode.FOREACH_IN;
			} else if (isExpressionContext(currentLine)) {
				mode = CompletionMode.EXPRESSION;
			} else {
				mode = CompletionMode.GENERAL;
			}

			return new CompletionContext(prefix, replaceOffset, prefix.length(), mode, openBlockKind);
		}

		boolean hasVelocityPrefix() {
			return prefix.startsWith("#") || prefix.startsWith("$");
		}

		boolean hasOpenBlock() {
			return openBlockKind != null;
		}

		boolean isInsideIfBlock() {
			return openBlockKind != null && openBlockKind.intValue() == VelocityParserConstants.IF_DIRECTIVE;
		}

		private static String extractCurrentLine(String textBeforeCaret) {
			int lastNewline = Math.max(textBeforeCaret.lastIndexOf('\n'), textBeforeCaret.lastIndexOf('\r'));
			return lastNewline >= 0 ? textBeforeCaret.substring(lastNewline + 1) : textBeforeCaret;
		}

		private static boolean isForeachInContext(String currentLine) {
			String trimmed = currentLine.trim();
			return trimmed.matches(".*#foreach\\s*\\(\\s*\\$[A-Za-z_][A-Za-z0-9_-]*\\s*");
		}

		private static boolean isExpressionContext(String currentLine) {
			String trimmed = currentLine.trim();
			if (trimmed.isEmpty()) {
				return false;
			}
			return trimmed.contains("#if(")
					|| trimmed.contains("#elseif(")
					|| trimmed.contains("#set(")
					|| trimmed.contains("#foreach(")
					|| trimmed.endsWith("=")
					|| trimmed.endsWith("&&")
					|| trimmed.endsWith("||")
					|| trimmed.endsWith("!")
					|| trimmed.endsWith("==")
					|| trimmed.endsWith("!=")
					|| trimmed.endsWith("<")
					|| trimmed.endsWith("<=")
					|| trimmed.endsWith(">")
					|| trimmed.endsWith(">=");
		}

		private static Integer findOpenBlock(String textBeforeCaret) {
			Deque<Integer> stack = new ArrayDeque<Integer>();
			try {
				VelocityParserTokenManager tokenManager = new VelocityParserTokenManager(
						new SimpleCharStream(new ByteArrayInputStream(textBeforeCaret.getBytes(StandardCharsets.UTF_8)), 1, 1));
				Token token;
				do {
					token = tokenManager.getNextToken();
					switch (token.kind) {
						case VelocityParserConstants.IF_DIRECTIVE:
						case VelocityParserConstants.FOREACH_DIRECTIVE:
						case VelocityParserConstants.MACRO_DIRECTIVE:
							stack.push(Integer.valueOf(token.kind));
							break;
						case VelocityParserConstants.END:
							if (!stack.isEmpty()) {
								stack.pop();
							}
							break;
						default:
							break;
					}
				} while (token.kind != VelocityParserConstants.EOF);
			} catch (TokenMgrError ex) {
				// Ignore incomplete tokens while typing and use the last stable stack state.
			}

			return stack.peek();
		}
	}

	private static final class TemplateSymbols {
		private final LinkedHashSet<String> macros = new LinkedHashSet<String>();
		private final LinkedHashSet<String> declaredReferences = new LinkedHashSet<String>();
		private final LinkedHashSet<String> observedReferences = new LinkedHashSet<String>();

		Set<String> macros() {
			return macros;
		}

		Set<String> declaredReferences() {
			return declaredReferences;
		}

		Set<String> observedReferences() {
			return observedReferences;
		}
	}

	private static final class SymbolCollector extends VelocityAnalyser {
		private final TemplateSymbols symbols;

		private SymbolCollector(TemplateSymbols symbols) {
			this.symbols = symbols;
		}

		@Override
		public Object visit(ASTMacroStatement node, Object data) {
			Token macroName = nthToken(node.getFirstToken(), 2);
			if (macroName != null && macroName.image != null && !macroName.image.isBlank()) {
				symbols.macros.add(macroName.image.trim());
			}

			for (int i = 0; i < node.jjtGetNumChildren(); i++) {
				if (node.jjtGetChild(i) instanceof ASTIdentifier child) {
					String reference = normalizeReference(child.getFirstToken() != null ? child.getFirstToken().image : null);
					if (reference != null) {
						symbols.declaredReferences.add(reference);
					}
				}
			}
			return node.childrenAccept(this, data);
		}

		@Override
		public Object visit(ASTForEachStatement node, Object data) {
			if (node.jjtGetNumChildren() > 0 && node.jjtGetChild(0) instanceof SimpleNode child) {
				String reference = normalizeReference(child.getFirstToken() != null ? child.getFirstToken().image : null);
				if (reference != null) {
					symbols.declaredReferences.add(reference);
				}
			}
			return node.childrenAccept(this, data);
		}

		@Override
		public Object visit(ASTSetDirective node, Object data) {
			if (node.jjtGetNumChildren() > 0 && node.jjtGetChild(0) instanceof SimpleNode child) {
				String reference = normalizeReference(child.getFirstToken() != null ? child.getFirstToken().image : null);
				if (reference != null) {
					symbols.declaredReferences.add(reference);
				}
			}
			return node.childrenAccept(this, data);
		}

		@Override
		public Object visit(ASTReference node, Object data) {
			String reference = normalizeReference(node.getFirstToken() != null ? node.getFirstToken().image : null);
			if (reference != null) {
				symbols.observedReferences.add(reference);
			}
			return node.childrenAccept(this, data);
		}

		@Override
		public void openTransaction() {
		}

		@Override
		public void commitTransaction() {
		}

		private Token nthToken(Token token, int offset) {
			Token current = token;
			for (int i = 0; i < offset && current != null; i++) {
				current = current.next;
			}
			return current;
		}
	}
}
