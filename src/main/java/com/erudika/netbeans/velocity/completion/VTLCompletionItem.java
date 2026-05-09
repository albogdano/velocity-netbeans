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
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.ImageUtilities;

public class VTLCompletionItem implements org.netbeans.spi.editor.completion.CompletionItem {

	ImageIcon hashBlackIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/hash-black.png"));
	ImageIcon hashWhiteIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/hash-white.png"));
	ImageIcon hashBlueIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/hash-blue.png"));
	ImageIcon hashDBlueIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/hash-dblue.png"));
	ImageIcon hashOrangeIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/hash-orange.png"));

	ImageIcon macroWhiteIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/macro-white.png"));
	ImageIcon macroBlueIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/macro-blue.png"));
	ImageIcon macroOrangeIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/macro-orange.png"));

	ImageIcon varWhiteIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/var-white.png"));
	ImageIcon varBlueIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/var-blue.png"));
	ImageIcon varOrangeIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/var-orange.png"));

	ImageIcon operatorIco = new ImageIcon(ImageUtilities.loadImage("com/erudika/netbeans/velocity/operator.png"));

	Color blue = Color.decode("#007dda");
	Color orange = Color.decode("#e89500");
	Color darkorange = Color.decode("#e3662a");

	public enum ItemType {
		DIRECTIVE("d"),
		REFERENCE("r"),
		KEYWORD("k"),
		OPERATOR("o"),
		PROPERTY("p"),
		METHOD("m");

		private final String label;

		ItemType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final VTLCompletionProposal proposal;

	VTLCompletionItem(VTLCompletionProposal proposal) {
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

			String insertText = proposal.getInsertText();
			if (insertText.endsWith("()")) {
				// No-param method or empty parens directive: place caret after ()
				caret.setDot(startOffset + insertText.length());
			} else if (insertText.contains("(")) {
				// Method with params or directive with body: place caret inside parens
				int parenPos = startOffset + insertText.indexOf('(') + 1;
				caret.setDot(parenPos);
			} else if (insertText.contains("#*")) {
				caret.setDot(startOffset + 3);
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
		CompletionUtilities.renderHtml(getIcon(selected), getLeftLabelHtml(defaultForeground), getRightLabelHtml(defaultForeground),
				g, defaultFont, getColor(defaultForeground, selected), width, height, selected);
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
		return g.getFontMetrics(font).stringWidth(proposal.getName() + "  " + proposal.getDescription() + "      ");
	}

	@Override
	public boolean instantSubstitution(JTextComponent component) {
		defaultAction(component);
		return true;
	}

	@Override
	public void processKeyEvent(KeyEvent evt) {
	}

	private String getLeftLabelHtml(Color defaultForeground) {
		StringBuilder sb = new StringBuilder();
//		sb.append("<html>");
//		sb.append("<font color='#0066cc'>");
//		sb.append(proposal.getType().getLabel());
//		sb.append("</font> ");
//		sb.append("<b>");
		sb.append(proposal.getName());
//		sb.append("</b>");
//		sb.append("</html>");
		return sb.toString();
	}

	private String getRightLabelHtml(Color defaultForeground) {
		StringBuilder sb = new StringBuilder();
//		sb.append("<html>");
		if (proposal.getDescription() != null && !proposal.getDescription().isEmpty()) {
			sb.append(" <font color='#").append(Integer.toHexString(defaultForeground.getRGB()).substring(2)).append("'>");
			sb.append(proposal.getDescription().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
			sb.append("</font>");
		}
//		sb.append("</html>");
		return sb.toString();
	}

	private ImageIcon getIcon(boolean selected) {
		String desc = proposal.getDescription().toLowerCase();
		switch (proposal.getType()) {
			case DIRECTIVE:
				return desc.contains("velocimacro") ?
						(selected ? macroWhiteIco : macroBlueIco) :
						(selected ? hashWhiteIco : hashBlueIco);
			case KEYWORD:
				return selected ? hashWhiteIco : hashDBlueIco;
			case OPERATOR:
				return operatorIco;
			case REFERENCE:
				return selected ? varWhiteIco : proposal.getInsertText().startsWith("$foreach.") ? varOrangeIco : varBlueIco;
			case PROPERTY:
				return selected ? varWhiteIco : varBlueIco;
			case METHOD:
				return selected ? macroWhiteIco : macroBlueIco;
			default:
				return macroBlueIco;
		}
	}

	private Color getColor(Color defaultForeground, boolean selected) {
		if (selected) {
			return defaultForeground;
		} else {
			String desc = proposal.getDescription().toLowerCase();
			switch (proposal.getType()) {
				case DIRECTIVE:
					return desc.contains("velocimacro") ? orange : blue;
				case KEYWORD:
					return darkorange;
				case OPERATOR:
					return darkorange;
				case REFERENCE:
					return proposal.getInsertText().startsWith("$foreach.") ? orange :
							(desc.contains("context variable") ? blue : defaultForeground);
				case PROPERTY:
					return blue;
				case METHOD:
					return defaultForeground;
				default:
					return defaultForeground;
			}
		}
	}
}
