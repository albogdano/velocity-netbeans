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

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Velocity dot-expressions into structured components for type resolution.
 *
 * Examples:
 * <ul>
 *   <li>"$user." → base="$user", chain=[], filter=""</li>
 *   <li>"$user.na" → base="$user", chain=[], filter="na"</li>
 *   <li>"$user.getName()." → base="$user", chain=["getName()"], filter=""</li>
 *   <li>"$user.getName().to" → base="$user", chain=["getName()"], filter="to"</li>
 *   <li>"$!{user.getAddress().getCity()}" → base="$user", chain=["getAddress()","getCity()"], filter=""</li>
 * </ul>
 */
public final class DotExpressionParser {

	private DotExpressionParser() {
	}

	/**
	 * Attempts to parse a dot-expression from the text before the caret.
	 * Returns null if the text doesn't represent a valid dot-expression.
	 *
	 * @param textBeforeCaret all text from the start of the document to the caret position
	 * @param caretOffset the absolute caret offset in the document
	 * @return a parsed DotExpression, or null if not a dot-expression context
	 */
	public static DotExpression parse(String textBeforeCaret, int caretOffset) {
		if (textBeforeCaret == null || textBeforeCaret.isEmpty()) {
			return null;
		}

		// Walk backwards from end to find the start of the reference expression
		int end = textBeforeCaret.length();
		int pos = end - 1;

		// Collect the filter (partial text being typed after the last dot)
		StringBuilder filterBuilder = new StringBuilder();
		while (pos >= 0 && isIdentChar(textBeforeCaret.charAt(pos))) {
			filterBuilder.insert(0, textBeforeCaret.charAt(pos));
			pos--;
		}
		String filter = filterBuilder.toString();

		// We expect a dot before the filter (or at the end if filter is empty)
		if (pos < 0 || textBeforeCaret.charAt(pos) != '.') {
			return null;
		}
		pos--; // skip the dot

		// Now parse the chain going backwards: each segment is either
		// "methodName()" or "propertyName", separated by dots
		List<String> chainReversed = new ArrayList<>();

		while (pos >= 0) {
			// Skip closing paren if present (method call)
			if (pos >= 0 && textBeforeCaret.charAt(pos) == ')') {
				// Find matching open paren (handle simple cases, no nested parens with content)
				int parenDepth = 1;
				pos--;
				while (pos >= 0 && parenDepth > 0) {
					char c = textBeforeCaret.charAt(pos);
					if (c == ')') {
						parenDepth++;
					} else if (c == '(') {
						parenDepth--;
					}
					pos--;
				}
				// pos is now before the '('
			}

			// Collect identifier (method name or property name)
			StringBuilder segmentBuilder = new StringBuilder();
			while (pos >= 0 && isIdentChar(textBeforeCaret.charAt(pos))) {
				segmentBuilder.insert(0, textBeforeCaret.charAt(pos));
				pos--;
			}

			String segment = segmentBuilder.toString();
			if (segment.isEmpty()) {
				break;
			}

			// Check if it was a method call (had parens)
			// Re-check: look ahead in original text for the paren
			int segStart = pos + 1;
			int segEnd = segStart + segment.length();
			if (segEnd < textBeforeCaret.length() && textBeforeCaret.charAt(segEnd) == '(') {
				chainReversed.add(segment + "()");
			} else {
				chainReversed.add(segment);
			}

			// Check for dot separator (to continue the chain)
			if (pos >= 0 && textBeforeCaret.charAt(pos) == '.') {
				pos--; // skip the dot, continue
			} else {
				break;
			}
		}

		if (chainReversed.isEmpty()) {
			return null;
		}

		// The last item in chainReversed is the base variable (or its first property access)
		// We need to find the $ that starts the reference
		// Actually, we need to determine where the $ prefix is.
		// Let's check if the remaining text (at pos) starts with $, $!, ${, $!{
		String baseVar = null;

		// Skip any remaining chars that could be part of the VTL reference prefix
		if (pos >= 0 && textBeforeCaret.charAt(pos) == '{') {
			pos--;
		}
		if (pos >= 0 && textBeforeCaret.charAt(pos) == '!') {
			pos--;
		}
		if (pos >= 0 && textBeforeCaret.charAt(pos) == '$') {
			// Found the start of the reference
			String lastSegment = chainReversed.get(chainReversed.size() - 1);
			// Strip () for the base var name
			String baseVarName = lastSegment.endsWith("()") ? lastSegment.substring(0, lastSegment.length() - 2) : lastSegment;
			baseVar = "$" + baseVarName;
			chainReversed.remove(chainReversed.size() - 1);
		} else {
			// No $ prefix found — not a valid Velocity reference
			return null;
		}

		// Reverse the chain to get it in correct order
		List<String> chain = new ArrayList<>();
		for (int i = chainReversed.size() - 1; i >= 0; i--) {
			chain.add(chainReversed.get(i));
		}

		// Calculate the replace offset (where the filter text starts)
		int replaceOffset = caretOffset - filter.length();

		return new DotExpression(baseVar, chain, filter, replaceOffset, filter.length());
	}

	private static boolean isIdentChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-';
	}

	/**
	 * Represents a parsed dot-expression.
	 *
	 * @param baseVar       The base variable (e.g., "$user")
	 * @param methodChain   The method/property chain between base and filter (e.g., ["getAddress()"])
	 * @param filter        The partial text being typed (e.g., "to" in "$user.to")
	 * @param replaceOffset The document offset where replacement should start
	 * @param replaceLength The length of text to replace
	 */
	public record DotExpression(
			String baseVar,
			List<String> methodChain,
			String filter,
			int replaceOffset,
			int replaceLength) {
	}
}
