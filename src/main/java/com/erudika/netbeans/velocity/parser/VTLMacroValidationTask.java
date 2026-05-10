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
package com.erudika.netbeans.velocity.parser;

import com.erudika.netbeans.velocity.completion.MacroLibraryScanner;
import com.erudika.netbeans.velocity.jcclexer.Token;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.jcclexer.node.ASTBlock;
import com.erudika.netbeans.velocity.jcclexer.node.ASTDirective;
import com.erudika.netbeans.velocity.jcclexer.node.ASTIdentifier;
import com.erudika.netbeans.velocity.jcclexer.node.ASTMacroStatement;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 * Scheduler task that validates macro call argument counts against their
 * definitions. Reports WARNING annotations when a macro is called with
 * a different number of arguments than declared.
 */
public class VTLMacroValidationTask extends ParserResultTask<VTLParserResult> {

	private static final String ERROR_LAYER = "velocity-macro";

	public VTLMacroValidationTask() {
	}

	@Override
	public void run(VTLParserResult vtlResult, SchedulerEvent event) {
		try {
			VelocityParser parser = vtlResult.getParser();
			SimpleNode astRoot = vtlResult.getAST();
			Document document = vtlResult.getSnapshot().getSource().getDocument(false);

			if (astRoot == null || document == null) {
				HintsController.setErrors(document == null ? vtlResult.getSnapshot().getSource().getDocument(true) : document, ERROR_LAYER, List.of());
				return;
			}

			VTLUpToDateStatusProvider sp = VTLUpToDateStatusProvider.forDocument(document);
			if (sp != null) {
				sp.setProcessingStatus();
			}

			Map<String, Integer> macroParamCounts = collectMacroDefinitions(astRoot);

			FileObject fo = vtlResult.getSnapshot().getSource().getFileObject();
			mergeLibraryMacros(fo, macroParamCounts);

			List<MacroValidationError> errors = validateMacroCalls(astRoot, parser, macroParamCounts, document);

			List<ErrorDescription> errorDescs = new ArrayList<>();
			for (MacroValidationError err : errors) {
				int start = NbDocument.findLineOffset((StyledDocument) document, Math.max(err.line - 1, 0))
						+ Math.max(err.column - 1, 0);
				int end = NbDocument.findLineOffset((StyledDocument) document, Math.max(err.endLine - 1, 0))
						+ err.endColumn;

				ErrorDescription desc = ErrorDescriptionFactory.createErrorDescription(
						Severity.WARNING,
						err.message,
						document,
						document.createPosition(start),
						document.createPosition(end)
				);
				errorDescs.add(desc);
			}

			HintsController.setErrors(document, ERROR_LAYER, errorDescs);

			if (sp != null) {
				sp.setOkStatus();
			}
		} catch (BadLocationException | ParseException ex) {
			Exceptions.printStackTrace(ex);
		}
	}

	private Map<String, Integer> collectMacroDefinitions(SimpleNode root) {
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();
		return collector.definitions;
	}

	private void mergeLibraryMacros(FileObject fo, Map<String, Integer> macroParamCounts) {
		if (fo == null) return;
		try {
			for (MacroLibraryScanner.MacroInfo macro : MacroLibraryScanner.getMacros(fo)) {
				macroParamCounts.putIfAbsent(macro.name(), macro.params().size());
			}
		} catch (Exception ex) {
			// Library scanning failures should not block validation
		}
	}

	private List<MacroValidationError> validateMacroCalls(SimpleNode root, VelocityParser parser,
			Map<String, Integer> macroParamCounts, Document document) {
		MacroCallValidator validator = new MacroCallValidator(parser, macroParamCounts);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();
		return validator.errors;
	}

	@Override
	public int getPriority() {
		return 110;
	}

	@Override
	public Class<? extends Scheduler> getSchedulerClass() {
		return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
	}

	@Override
	public void cancel() {
	}

	static final class MacroDefinitionCollector extends VelocityAnalyser {
		final Map<String, Integer> definitions = new HashMap<>();

		@Override
		public Object visit(ASTMacroStatement node, Object data) {
			Token macroName = nthToken(node.getFirstToken(), 2);
			if (macroName != null && macroName.image != null && !macroName.image.isBlank()) {
				String name = macroName.image.trim();
				int paramCount = countParams(node);
				definitions.put(name, paramCount);
			}
			return node.childrenAccept(this, data);
		}

		private int countParams(ASTMacroStatement node) {
			int count = 0;
			for (int i = 0; i < node.jjtGetNumChildren(); i++) {
				if (node.jjtGetChild(i) instanceof ASTIdentifier) {
					count++;
				}
			}
			return count;
		}

		private Token nthToken(Token token, int offset) {
			Token current = token;
			for (int i = 0; i < offset && current != null; i++) {
				current = current.next;
			}
			return current;
		}

		@Override
		public void openTransaction() {
		}

		@Override
		public void commitTransaction() {
		}
	}

	static final class MacroCallValidator extends VelocityAnalyser {
		private final VelocityParser parser;
		private final Map<String, Integer> macroParamCounts;
		final List<MacroValidationError> errors = new ArrayList<>();

		MacroCallValidator(VelocityParser parser, Map<String, Integer> macroParamCounts) {
			this.parser = parser;
			this.macroParamCounts = macroParamCounts;
		}

		@Override
		public Object visit(ASTDirective node, Object data) {
			String name = extractDirectiveName(node);
			if (name == null) {
				return node.childrenAccept(this, data);
			}

			if (parser.isDirective(name)) {
				return node.childrenAccept(this, data);
			}

			Integer expectedParams = macroParamCounts.get(name);
			if (expectedParams == null) {
				return node.childrenAccept(this, data);
			}

			int callArgs = countCallArgs(node);
			if (callArgs != expectedParams) {
				Token token = node.getFirstToken();
				errors.add(new MacroValidationError(
						name, expectedParams, callArgs,
						token.beginLine, token.beginColumn,
						token.endLine, token.endColumn
				));
			}

			return node.childrenAccept(this, data);
		}

		private String extractDirectiveName(ASTDirective node) {
			Token first = node.getFirstToken();
			if (first == null) return null;

			switch (first.kind) {
				case VelocityParserConstants.MACROCALL_DIRECTIVE:
					String image = first.image;
					return image.startsWith("#") ? image.substring(1) : image;
				case VelocityParserConstants.BRACKETED_WORD:
					return first.image.substring(2, first.image.length() - 1);
				case VelocityParserConstants.WORD:
					return first.image.startsWith("#") ? first.image.substring(1) : first.image;
				default:
					return null;
			}
		}

		private int countCallArgs(ASTDirective node) {
			int numChildren = node.jjtGetNumChildren();
			if (numChildren == 0) return 0;

			int args = numChildren;
			if (node.jjtGetChild(numChildren - 1) instanceof ASTBlock) {
				args--;
			}
			return args;
		}

		@Override
		public void openTransaction() {
		}

		@Override
		public void commitTransaction() {
		}
	}

	static final class MacroValidationError {
		final String macroName;
		final int expectedParams;
		final int actualArgs;
		final int line;
		final int column;
		final int endLine;
		final int endColumn;
		final String message;

		MacroValidationError(String macroName, int expectedParams, int actualArgs,
				int line, int column, int endLine, int endColumn) {
			this.macroName = macroName;
			this.expectedParams = expectedParams;
			this.actualArgs = actualArgs;
			this.line = line;
			this.column = column;
			this.endLine = endLine;
			this.endColumn = endColumn;
			this.message = "Macro #" + macroName + " expects " + expectedParams
					+ " argument" + (expectedParams != 1 ? "s" : "")
					+ " but was called with " + actualArgs;
		}
	}
}