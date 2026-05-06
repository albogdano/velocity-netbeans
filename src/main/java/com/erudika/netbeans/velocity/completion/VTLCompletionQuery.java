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
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;

public class VTLCompletionQuery extends AsyncCompletionQuery {

	@Override
	protected void query(CompletionResultSet resultSet, Document document, int caretOffset) {
		try {
			String textBefore = document.getText(0, caretOffset);
			int lineStart = findLineStart(textBefore);
			String line = textBefore.substring(lineStart);
			String prefix = extractPrefix(document, caretOffset);

			List<VTLCompletionItem> items = determineCompletions(line, prefix, caretOffset);

			for (VTLCompletionItem item : items) {
				resultSet.addItem(item);
			}

			resultSet.finish();
		} catch (BadLocationException ex) {
			resultSet.finish();
		}
	}

	private int findLineStart(String textBefore) {
		int lastNewline = textBefore.lastIndexOf('\n');
		return lastNewline >= 0 ? lastNewline + 1 : 0;
	}

	private String extractPrefix(Document document, int caretOffset) throws BadLocationException {
		StringBuilder sb = new StringBuilder();
		int pos = caretOffset - 1;
		while (pos >= 0) {
			char ch = document.getText(pos, 1).charAt(0);
			if (Character.isJavaIdentifierPart(ch) || ch == '#' || ch == '$') {
				sb.insert(0, ch);
				pos--;
			} else {
				break;
			}
		}
		return sb.toString();
	}

	private List<VTLCompletionItem> determineCompletions(String line, String prefix, int caretOffset) {
		List<VTLCompletionItem> items = new ArrayList<>();
		if (line.endsWith("#") || line.endsWith("#{") || startsWithDirectivePrefix(prefix)) {
			addDirectives(items, prefix);
		} else if (line.endsWith("$") || startsWithReferencePrefix(prefix)) {
			addReferences(items, prefix);
		} else if (isInExpression(line)) {
			addReferences(items, prefix);
			addKeywords(items, prefix);
			addOperators(items, prefix);
			addBooleanLiterals(items, prefix);
		} else if (isInForeach(line)) {
			addKeywordIn(items, prefix);
		} else {
			addDirectives(items, prefix);
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

	private void addDirectives(List<VTLCompletionItem> items, String filter) {
		boolean hasHash = filter.startsWith("#");
		String search = hasHash ? filter.substring(1) : filter;
		if (match("if", search))
			items.add(new VTLCompletionItem("#if", "#if()\n\n#end", "Conditional directive", VTLCompletionItem.ItemType.DIRECTIVE, 10));
		if (match("else", search))
			items.add(new VTLCompletionItem("#else", "Else branch", VTLCompletionItem.ItemType.DIRECTIVE, 11));
		if (match("elseif", search))
			items.add(new VTLCompletionItem("#elseif", "#elseif()\n", "Else if branch", VTLCompletionItem.ItemType.DIRECTIVE, 12));
		if (match("end", search))
			items.add(new VTLCompletionItem("#end", "End block", VTLCompletionItem.ItemType.DIRECTIVE, 13));
		if (match("foreach", search))
			items.add(new VTLCompletionItem("#foreach", "#foreach($item in $list)\n\n#end", "Loop directive", VTLCompletionItem.ItemType.DIRECTIVE, 14));
		if (match("set", search))
			items.add(new VTLCompletionItem("#set", "#set($var = value)", "Variable assignment", VTLCompletionItem.ItemType.DIRECTIVE, 15));
		if (match("macro", search))
			items.add(new VTLCompletionItem("#macro", "#macro(name $arg)\n\n#end", "Macro definition", VTLCompletionItem.ItemType.DIRECTIVE, 16));
		if (match("include", search))
			items.add(new VTLCompletionItem("#include", "#include(\"template.vm\")", "Include template", VTLCompletionItem.ItemType.DIRECTIVE, 17));
		if (match("parse", search))
			items.add(new VTLCompletionItem("#parse", "#parse(\"template.vm\")", "Parse and include template", VTLCompletionItem.ItemType.DIRECTIVE, 18));
		if (match("evaluate", search))
			items.add(new VTLCompletionItem("#evaluate", "#evaluate($expr)", "Evaluate expression", VTLCompletionItem.ItemType.DIRECTIVE, 19));
		if (match("define", search))
			items.add(new VTLCompletionItem("#define", "#define($var)\n\n#end", "Define block", VTLCompletionItem.ItemType.DIRECTIVE, 20));
		if (match("stop", search))
			items.add(new VTLCompletionItem("#stop", "Stop rendering", VTLCompletionItem.ItemType.DIRECTIVE, 21));
	}

	private void addReferences(List<VTLCompletionItem> items, String filter) {
		boolean hasDollar = filter.startsWith("$");
		String search = hasDollar ? filter.substring(1) : filter;
		if (match("velocityCount", search))
			items.add(new VTLCompletionItem("$velocityCount", "Loop counter (deprecated)", VTLCompletionItem.ItemType.REFERENCE, 30));
		if (match("velocityHasNext", search))
			items.add(new VTLCompletionItem("$velocityHasNext", "Loop hasNext flag (deprecated)", VTLCompletionItem.ItemType.REFERENCE, 31));
	}

	private void addKeywords(List<VTLCompletionItem> items, String filter) {
		if (match("in", filter))
			items.add(new VTLCompletionItem("in", "Foreach keyword", VTLCompletionItem.ItemType.KEYWORD, 50));
		if (match("and", filter))
			items.add(new VTLCompletionItem("and", "Logical AND (alternative)", VTLCompletionItem.ItemType.KEYWORD, 51));
		if (match("or", filter))
			items.add(new VTLCompletionItem("or", "Logical OR (alternative)", VTLCompletionItem.ItemType.KEYWORD, 52));
		if (match("not", filter))
			items.add(new VTLCompletionItem("not", "Logical NOT (alternative)", VTLCompletionItem.ItemType.KEYWORD, 53));
		if (match("eq", filter))
			items.add(new VTLCompletionItem("eq", "Equals (alternative)", VTLCompletionItem.ItemType.KEYWORD, 54));
		if (match("ne", filter))
			items.add(new VTLCompletionItem("ne", "Not equals (alternative)", VTLCompletionItem.ItemType.KEYWORD, 55));
		if (match("lt", filter))
			items.add(new VTLCompletionItem("lt", "Less than (alternative)", VTLCompletionItem.ItemType.KEYWORD, 56));
		if (match("le", filter))
			items.add(new VTLCompletionItem("le", "Less or equal (alternative)", VTLCompletionItem.ItemType.KEYWORD, 57));
		if (match("gt", filter))
			items.add(new VTLCompletionItem("gt", "Greater than (alternative)", VTLCompletionItem.ItemType.KEYWORD, 58));
		if (match("ge", filter))
			items.add(new VTLCompletionItem("ge", "Greater or equal (alternative)", VTLCompletionItem.ItemType.KEYWORD, 59));
	}

	private void addKeywordIn(List<VTLCompletionItem> items, String filter) {
		if (match("in", filter))
			items.add(new VTLCompletionItem("in", "Foreach keyword", VTLCompletionItem.ItemType.KEYWORD, 5));
	}

	private void addOperators(List<VTLCompletionItem> items, String filter) {
		if (filter.isEmpty() || filter.startsWith("!"))
			items.add(new VTLCompletionItem("!", "Logical NOT", VTLCompletionItem.ItemType.OPERATOR, 40));
		if (filter.isEmpty() || filter.startsWith("&&"))
			items.add(new VTLCompletionItem("&&", "Logical AND", VTLCompletionItem.ItemType.OPERATOR, 41));
		if (filter.isEmpty() || filter.startsWith("||"))
			items.add(new VTLCompletionItem("||", "Logical OR", VTLCompletionItem.ItemType.OPERATOR, 42));
		if (filter.isEmpty() || filter.startsWith("=="))
			items.add(new VTLCompletionItem("==", "Equals", VTLCompletionItem.ItemType.OPERATOR, 43));
		if (filter.isEmpty() || filter.startsWith("!="))
			items.add(new VTLCompletionItem("!=", "Not equals", VTLCompletionItem.ItemType.OPERATOR, 44));
	}

	private void addBooleanLiterals(List<VTLCompletionItem> items, String filter) {
		if (match("true", filter))
			items.add(new VTLCompletionItem("true", "Boolean true", VTLCompletionItem.ItemType.KEYWORD, 60));
		if (match("false", filter))
			items.add(new VTLCompletionItem("false", "Boolean false", VTLCompletionItem.ItemType.KEYWORD, 61));
	}

	private boolean match(String item, String filter) {
		return item.toLowerCase().startsWith(filter.toLowerCase());
	}
}
