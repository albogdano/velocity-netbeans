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
package com.erudika.netbeans.velocity.options;

import com.erudika.netbeans.velocity.completion.VTLCompletionSettings;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class VTLContextOptionsPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	private final VTLContextOptionsPanelController controller;

	private final JLabel macroLabel;
	private final JTextField macroField;
	private final JLabel symbolsLabel;
	private final JLabel symbolsHint;
	private final JTextArea symbolsArea;
	private final JScrollPane symbolsScroll;

	VTLContextOptionsPanel(VTLContextOptionsPanelController controller) {
		this.controller = controller;
		this.macroLabel = new JLabel("Macro Library Files (comma-separated):");
		this.macroField = new JTextField();
		this.symbolsLabel = new JLabel("Context Variables (one per line):");
		this.symbolsHint = new JLabel("<html><small>Format: <b>$varName</b> or <b>$varName:com.example.Type</b> (enables method completion)</small></html>");
		this.symbolsArea = new JTextArea(8, 40);
		this.symbolsScroll = new JScrollPane(symbolsArea);

		macroField.getDocument().addDocumentListener(new ChangeListener());
		symbolsArea.getDocument().addDocumentListener(new ChangeListener());

		macroLabel.setLabelFor(macroField);
		symbolsLabel.setLabelFor(symbolsArea);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(macroLabel)
				.addComponent(macroField)
				.addGap(12)
				.addComponent(symbolsLabel)
				.addComponent(symbolsHint)
				.addComponent(symbolsScroll));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(macroLabel)
				.addComponent(macroField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
				.addComponent(symbolsLabel)
				.addComponent(symbolsHint)
				.addComponent(symbolsScroll));
	}

	void load() {
		macroField.setText(VTLCompletionSettings.getConfiguredMacroLibrary());
		symbolsArea.setText(String.join(System.lineSeparator(), VTLCompletionSettings.getConfiguredSymbols()));
	}

	void store() {
		VTLCompletionSettings.setConfiguredMacroLibrary(macroField.getText().trim());
		VTLCompletionSettings.setConfiguredSymbols(
				java.util.List.of(symbolsArea.getText().split("[\\r\\n]+")));
	}

	boolean valid() {
		return true;
	}

	private class ChangeListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) { controller.changed(); }
		@Override
		public void removeUpdate(DocumentEvent e) { controller.changed(); }
		@Override
		public void changedUpdate(DocumentEvent e) { controller.changed(); }
	}
}
