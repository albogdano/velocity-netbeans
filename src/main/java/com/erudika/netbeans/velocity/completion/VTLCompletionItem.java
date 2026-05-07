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

	private final VTLCompletionProposal proposal;

	public VTLCompletionItem(VTLCompletionProposal proposal) {
		this.proposal = proposal;
	}

	@Override
	public void defaultAction(JTextComponent component) {
		try {
			Document doc = component.getDocument();
			Caret caret = component.getCaret();
			int startOffset = proposal.getReplaceOffset();
			doc.remove(startOffset, proposal.getReplaceLength());
			doc.insertString(startOffset, proposal.getInsertText(), null);

			if (proposal.getInsertText().contains("(")) {
				int parenPos = startOffset + proposal.getInsertText().indexOf('(') + 1;
				caret.setDot(parenPos);
			} else {
				caret.setDot(startOffset + proposal.getInsertText().length());
			}

			Completion.get().hideAll();
		} catch (BadLocationException ex) {
			Completion.get().hideAll();
		}
	}

	@Override
	public int getSortPriority() {
		return proposal.getSortPriority();
	}

	@Override
	public CharSequence getSortText() {
		return proposal.getName();
	}

	@Override
	public CharSequence getInsertPrefix() {
		return proposal.getName();
	}

	@Override
	public void render(Graphics g, Font defaultFont, Color defaultForeground, Color defaultBackground, int width, int height, boolean selected) {
		g.setColor(selected ? new Color(220, 230, 255) : defaultBackground);
		g.fillRect(0, 0, width, height);
		g.setColor(defaultForeground);
		g.setFont(defaultFont);
		String text = proposal.getName() + " - " + (proposal.getDescription() != null ? proposal.getDescription() : "");
		g.drawString(text, 5, height - 5);
//		CompletionUtilities.renderHtml(null, text, null, g, defaultFont, (selected ? Color.white : Color.ORANGE), width, height, selected);
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
		return g.getFontMetrics(font).stringWidth(proposal.getName() + "  " + proposal.getDescription());
	}

	@Override
	public boolean instantSubstitution(JTextComponent component) {
		defaultAction(component);
		return true;
	}

	@Override
	public void processKeyEvent(KeyEvent evt) {
	}

	private String getLabelHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<font color='#0066cc'>");
		sb.append(proposal.getType().getLabel());
		sb.append("</font> ");
		sb.append("<b>");
		sb.append(proposal.getName());
		sb.append("</b>");
		if (proposal.getDescription() != null && !proposal.getDescription().isEmpty()) {
			sb.append(" <font color='#666666'>- ");
			sb.append(proposal.getDescription());
			sb.append("</font>");
		}
		sb.append("</html>");
		return sb.toString();
	}
}
