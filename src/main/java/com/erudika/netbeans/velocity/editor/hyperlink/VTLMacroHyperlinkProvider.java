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
package com.erudika.netbeans.velocity.editor.hyperlink;

import com.erudika.netbeans.velocity.jcclexer.ParseException;
import com.erudika.netbeans.velocity.jcclexer.SimpleCharStream;
import com.erudika.netbeans.velocity.jcclexer.Token;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserTokenManager;
import com.erudika.netbeans.velocity.jcclexer.node.ASTIdentifier;
import com.erudika.netbeans.velocity.jcclexer.node.ASTMacroStatement;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import com.erudika.netbeans.velocity.lexer.VTLTokenId;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;

public final class VTLMacroHyperlinkProvider implements HyperlinkProviderExt {

	private static final Logger LOG = Logger.getLogger(VTLMacroHyperlinkProvider.class.getName());

	private static final java.util.Set<HyperlinkType> SUPPORTED_TYPES = java.util.Set.of(HyperlinkType.GO_TO_DECLARATION);

	@Override
	public java.util.Set<HyperlinkType> getSupportedHyperlinkTypes() {
		return SUPPORTED_TYPES;
	}

	@Override
	public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
		if (type != HyperlinkType.GO_TO_DECLARATION) {
			return false;
		}
		return findMacroName(doc, offset) != null;
	}

	@Override
	public String getTooltipText(Document doc, int offset, HyperlinkType type) {
		MacroNameInfo info = findMacroName(doc, offset);
		if (info != null) {
			return "Go to definition of #" + info.name;
		}
		return null;
	}

	@Override
	public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
		MacroNameInfo info = findMacroName(doc, offset);
		if (info != null) {
			return new int[]{info.startOffset, info.endOffset};
		}
		return null;
	}

	@Override
	public void performClickAction(Document doc, int offset, HyperlinkType type) {
		MacroNameInfo info = findMacroName(doc, offset);
		if (info == null) {
			return;
		}
		MacroDefinition def = findDefinition(info.name, doc);
		if (def == null) {
			return;
		}
		if (def.fileObject != null) {
			navigateTo(def.fileObject, def.lineNumber);
		}
	}

	private MacroNameInfo findMacroName(Document doc, int offset) {
		if (!(doc instanceof AbstractDocument)) {
			return null;
		}
		AbstractDocument adoc = (AbstractDocument) doc;
		adoc.readLock();
		try {
			TokenHierarchy<?> hierarchy = TokenHierarchy.get(doc);
			if (hierarchy == null) {
				return null;
			}
			TokenSequence<VTLTokenId> ts = hierarchy.tokenSequence(VTLTokenId.getLanguage());
			if (ts == null) {
				return null;
			}
			ts.move(offset);
			if (!ts.moveNext() && !ts.movePrevious()) {
				return null;
			}
			org.netbeans.api.lexer.Token<VTLTokenId> token = ts.token();
			if (token == null) {
				return null;
			}
			int tokenId = token.id().ordinal();

			// Case 1: MACROCALL_DIRECTIVE token — the whole token is "#macroName"
			if (tokenId == VelocityParserConstants.MACROCALL_DIRECTIVE) {
				String text = token.text().toString();
				String macroName = text.startsWith("#") ? text.substring(1) : text;
				return new MacroNameInfo(macroName, ts.offset(), ts.offset() + token.length());
			}

			// Case 2: WORD token that is a macro name after HASH — check if preceded by HASH
			if (tokenId == VelocityParserConstants.WORD) {
				if (ts.movePrevious()) {
					org.netbeans.api.lexer.Token<VTLTokenId> prevToken = ts.token();
					if (prevToken.id().ordinal() == VelocityParserConstants.HASH) {
						String macroName = token.text().toString();
						// Check if this word is a known macro
						if (isKnownMacro(macroName, doc)) {
							int macroStart = ts.offset();
							ts.moveNext(); // move back to WORD
							int macroEnd = ts.offset() + ts.token().length();
							return new MacroNameInfo(macroName, macroStart, macroEnd);
						}
					}
					ts.moveNext(); // restore position
				}
			}
			return null;
		} finally {
			adoc.readUnlock();
		}
	}

	private boolean isKnownMacro(String name, Document doc) {
		try {
			String text = doc.getText(0, doc.getLength());
			if (findMacroInText(name, text) != null) {
				return true;
			}
		} catch (javax.swing.text.BadLocationException ex) {
			// fall through
		}
		FileObject fo = extractFileObject(doc);
		if (fo != null) {
			for (com.erudika.netbeans.velocity.completion.MacroLibraryScanner.MacroInfo macro
					: com.erudika.netbeans.velocity.completion.MacroLibraryScanner.getMacros(fo)) {
				if (name.equals(macro.name())) {
					return true;
				}
			}
		}
		return VelocityParser.isMacro(name);
	}

	MacroDefinition findDefinition(String macroName, Document doc) {
		try {
			String text = doc.getText(0, doc.getLength());
			DefinitionInfo localDef = findMacroInText(macroName, text);
			if (localDef != null) {
				FileObject fo = extractFileObject(doc);
				return new MacroDefinition(fo, localDef.lineNumber);
			}
		} catch (javax.swing.text.BadLocationException ex) {
			// fall through to library search
		}

		FileObject fo = extractFileObject(doc);
		if (fo != null) {
			for (com.erudika.netbeans.velocity.completion.MacroLibraryScanner.MacroInfo macro
					: com.erudika.netbeans.velocity.completion.MacroLibraryScanner.getMacros(fo)) {
				if (macroName.equals(macro.name())) {
					// Find the line number in the library file
					FileObject libFile = findLibraryFile(fo, macro.sourceFile());
					if (libFile != null) {
						int line = findMacroLineInFile(macroName, libFile);
						return new MacroDefinition(libFile, line);
					}
				}
			}
		}
		return null;
	}

	private FileObject extractFileObject(Document doc) {
		Object streamDescription = doc.getProperty(Document.StreamDescriptionProperty);
		if (streamDescription instanceof DataObject dataObject) {
			return dataObject.getPrimaryFile();
		}
		if (streamDescription instanceof FileObject fileObject) {
			return fileObject;
		}
		Source source = Source.create(doc);
		return source != null ? source.getFileObject() : null;
	}

	private FileObject findLibraryFile(FileObject contextFile, String sourceFile) {
		if (sourceFile != null) {
			FileObject fo = contextFile.getParent().getFileObject(sourceFile);
			if (fo != null) return fo;
		}
		// Search parent directories
		FileObject folder = contextFile.getParent();
		while (folder != null) {
			String libName = com.erudika.netbeans.velocity.completion.VTLCompletionSettings.getConfiguredMacroLibrary();
			if (libName == null || libName.isBlank()) {
				libName = com.erudika.netbeans.velocity.completion.MacroLibraryScanner.DEFAULT_LIBRARY;
			}
			for (String part : libName.split(",")) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					FileObject candidate = folder.getFileObject(trimmed);
					if (candidate != null) return candidate;
				}
			}
			folder = folder.getParent();
		}
		return null;
	}

	private int findMacroLineInFile(String macroName, FileObject fo) {
		try {
			String content = new String(fo.asBytes(), StandardCharsets.UTF_8);
			DefinitionInfo info = findMacroInText(macroName, content);
			return info != null ? info.lineNumber : 1;
		} catch (IOException ex) {
			return 1;
		}
	}

	static DefinitionInfo findMacroInText(String macroName, String text) {
		if (text == null || macroName == null) {
			return null;
		}
		try {
			VelocityParser parser = new VelocityParser();
			com.erudika.netbeans.velocity.jcclexer.Directive lineDirective =
					new com.erudika.netbeans.velocity.jcclexer.Directive(
							com.erudika.netbeans.velocity.jcclexer.Directive.LINE);
			parser.addDirective("parse", lineDirective);
			parser.addDirective("evaluate", lineDirective);
			parser.addDirective("define",
					new com.erudika.netbeans.velocity.jcclexer.Directive(
							com.erudika.netbeans.velocity.jcclexer.Directive.BLOCK));

			SimpleNode root = parser.parse(new StringReader(text), "hyperlink.vm");
			if (root == null) return null;

			MacroDefinitionFinder finder = new MacroDefinitionFinder(macroName);
			finder.openTransaction();
			finder.visit(root, null);
			finder.commitTransaction();
			return finder.found;
		} catch (ParseException ex) {
			// Fall back to lexical search
			return lexSearchMacroDefinition(macroName, text);
		}
	}

	private static DefinitionInfo lexSearchMacroDefinition(String macroName, String text) {
		try {
			VelocityParserTokenManager tm = new VelocityParserTokenManager(
					new SimpleCharStream(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), 1, 1));
			Token t;
			boolean afterMacroDirective = false;
			int line = 1;
			do {
				t = tm.getNextToken();
				if (t.kind == VelocityParserConstants.NEWLINE) line++;
				if (t.kind == VelocityParserConstants.MACRO_DIRECTIVE) {
					afterMacroDirective = true;
				} else if (afterMacroDirective && t.kind == VelocityParserConstants.WORD) {
					if (macroName.equals(t.image)) {
						return new DefinitionInfo(line);
					}
					afterMacroDirective = false;
				} else if (t.kind != VelocityParserConstants.WHITESPACE) {
					afterMacroDirective = false;
				}
			} while (t.kind != VelocityParserConstants.EOF);
		} catch (com.erudika.netbeans.velocity.jcclexer.TokenMgrError ex) {
			// ignore
		}
		return null;
	}

	private void navigateTo(FileObject fo, int lineNumber) {
		try {
			DataObject dob = DataObject.find(fo);
			org.openide.cookies.LineCookie lc = dob.getLookup().lookup(org.openide.cookies.LineCookie.class);
			if (lc == null) return;
			Line.Set lineSet = lc.getLineSet();
			Line line = lineSet.getCurrent(lineNumber - 1);
			line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
		} catch (DataObjectNotFoundException ex) {
			LOG.log(Level.FINE, "Cannot open file for navigation: {0}", fo);
		}
	}

	private record MacroNameInfo(String name, int startOffset, int endOffset) {
	}

	static final class DefinitionInfo {
		final int lineNumber;
		DefinitionInfo(int lineNumber) {
			this.lineNumber = lineNumber;
		}
	}

	private static final class MacroDefinition {
		final FileObject fileObject;
		final int lineNumber;
		MacroDefinition(FileObject fileObject, int lineNumber) {
			this.fileObject = fileObject;
			this.lineNumber = lineNumber;
		}
	}

	private static final class MacroDefinitionFinder extends VelocityAnalyser {
		private final String targetName;
		DefinitionInfo found;

		MacroDefinitionFinder(String targetName) {
			this.targetName = targetName;
		}

		@Override
		public Object visit(ASTMacroStatement node, Object data) {
			Token macroName = nthToken(node.getFirstToken(), 2);
			if (macroName != null && targetName.equals(macroName.image)) {
				found = new DefinitionInfo(macroName.beginLine);
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