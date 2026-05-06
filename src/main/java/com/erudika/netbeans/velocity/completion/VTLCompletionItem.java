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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionTask;

public class VTLCompletionItem implements org.netbeans.spi.editor.completion.CompletionItem {

	public enum ItemType {
		DIRECTIVE("d"),
		REFERENCE("r"),
		KEYWORD("k"),
		OPERATOR("o");

		private final String label;

		ItemType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final String name;
	private final String insertText;
	private final String description;
	private final ItemType type;
	private final int sortPriority;

	public VTLCompletionItem(String name, String insertText, String description, ItemType type, int sortPriority) {
		this.name = name;
		this.insertText = insertText;
		this.description = description;
		this.type = type;
		this.sortPriority = sortPriority;
	}

	public VTLCompletionItem(String name, String description, ItemType type, int sortPriority) {
		this(name, name, description, type, sortPriority);
	}

	@Override
	public void defaultAction(JTextComponent component) {
		try {
			Document doc = component.getDocument();
			Caret caret = component.getCaret();
			int dotPos = caret.getDot();
			String text = component.getText(0, dotPos);
			String prefix = extractPrefix(text);
			int startOffset = dotPos - prefix.length();

			doc.remove(startOffset, prefix.length());
			doc.insertString(startOffset, insertText, null);

			if (insertText.contains("(")) {
				int parenPos = startOffset + insertText.indexOf('(') + 1;
				caret.setDot(parenPos);
			} else {
				caret.setDot(startOffset + insertText.length());
			}

			Completion.get().hideAll();
		} catch (BadLocationException ex) {
			Completion.get().hideAll();
		}
	}

	@Override
	public int getSortPriority() {
		return sortPriority;
	}

	@Override
	public CharSequence getSortText() {
		return name;
	}

	@Override
	public CharSequence getInsertPrefix() {
		return insertText;
	}

	@Override
	public void render(Graphics g, Font defaultFont, Color defaultForeground, Color defaultBackground, int width, int height, boolean selected) {
		g.setColor(selected ? new Color(220, 230, 255) : defaultBackground);
		g.fillRect(0, 0, width, height);
		g.setColor(defaultForeground);
		g.setFont(defaultFont);
		g.drawString(name + " - " + (description != null ? description : ""), 5, height - 5);
	}

	@Override
	public CompletionTask createDocumentationTask() {
		return null;
	}

	@Override
	public CompletionTask createToolTipTask() {
		return null;
	}

	@Override
	public int getPreferredWidth(Graphics g, Font font) {
		return g.getFontMetrics(font).stringWidth(name + "  " + description);
	}

	@Override
	public boolean instantSubstitution(JTextComponent component) {
		defaultAction(component);
		return true;
	}

	@Override
	public void processKeyEvent(KeyEvent evt) {
	}

	private String extractPrefix(String textBefore) {
		StringBuilder sb = new StringBuilder();
		int pos = textBefore.length() - 1;
		while (pos >= 0) {
			char ch = textBefore.charAt(pos);
			if (Character.isJavaIdentifierPart(ch) || ch == '#' || ch == '$') {
				sb.insert(0, ch);
				pos--;
			} else {
				break;
			}
		}
		return sb.toString();
	}

	private String getLabelHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<font color='#0066cc'>");
		sb.append(type.getLabel());
		sb.append("</font> ");
		sb.append("<b>");
		sb.append(name);
		sb.append("</b>");
		if (description != null && !description.isEmpty()) {
			sb.append(" <font color='#666666'>- ");
			sb.append(description);
			sb.append("</font>");
		}
		sb.append("</html>");
		return sb.toString();
	}
}
