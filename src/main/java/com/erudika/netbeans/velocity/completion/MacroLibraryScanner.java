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
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.node.ASTIdentifier;
import com.erudika.netbeans.velocity.jcclexer.node.ASTMacroStatement;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class MacroLibraryScanner {

	/**
	 * Default library file.
	 */
	public static final String DEFAULT_LIBRARY = "VM_global_library.vm";

	private static final Logger LOG = Logger.getLogger(MacroLibraryScanner.class.getName());
	private static final String LIBRARY_PROPERTY = "velocimacro.library";

	private static final Map<FileObject, CachedResult> cache = Collections.synchronizedMap(new WeakHashMap<>());

	private MacroLibraryScanner() {
	}

	public static List<MacroInfo> getMacros(FileObject contextFile) {
		List<String> libraryNames = resolveLibraryNames();
		if (libraryNames.isEmpty()) {
			return List.of();
		}

		List<MacroInfo> allMacros = new ArrayList<>();
		for (String libName : libraryNames) {
			FileObject libFile = findLibraryFile(contextFile, libName);
			if (libFile == null) {
				continue;
			}
			allMacros.addAll(parseAndCache(libFile));
		}

		// Register library macro names so the lexer highlights them as directives
		for (MacroInfo macro : allMacros) {
			com.erudika.netbeans.velocity.jcclexer.VelocityParser.addMacroName(macro.name());
		}

		return allMacros;
	}

	private static List<String> resolveLibraryNames() {
		String configured = VTLCompletionSettings.getConfiguredMacroLibrary();
		if (configured != null && !configured.isBlank()) {
			List<String> names = new ArrayList<>();
			for (String part : configured.split(",")) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					names.add(trimmed);
				}
			}
			return names;
		}
		String prop = System.getProperty(LIBRARY_PROPERTY);
		if (prop != null && !prop.isBlank()) {
			List<String> names = new ArrayList<>();
			for (String part : prop.split(",")) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					names.add(trimmed);
				}
			}
			return names;
		}
		return List.of(DEFAULT_LIBRARY);
	}

	private static FileObject findLibraryFile(FileObject contextFile, String libName) {
		FileObject folder = contextFile.getParent();
		while (folder != null) {
			FileObject candidate = folder.getFileObject(libName);
			if (candidate != null) {
				return candidate;
			}
			folder = folder.getParent();
		}
		FileObject fromRoot = FileUtil.getConfigFile(libName);
		if (fromRoot != null) {
			return fromRoot;
		}
		LOG.log(Level.FINE, "Macro library not found: {0}", libName);
		return null;
	}

	private static List<MacroInfo> parseAndCache(FileObject libFile) {
		CachedResult cached = cache.get(libFile);
		long modified = libFile.lastModified().getTime();
		if (cached != null && cached.timestamp == modified) {
			return cached.macros;
		}

		List<MacroInfo> macros = parseLibrary(libFile);
		cache.put(libFile, new CachedResult(macros, modified));
		return macros;
	}

	private static List<MacroInfo> parseLibrary(FileObject libFile) {
		try {
			String content = new String(libFile.asBytes(), java.nio.charset.StandardCharsets.UTF_8);
			VelocityParser parser = new VelocityParser();
			parser.addDirective("parse", new Directive(Directive.LINE));
			parser.addDirective("evaluate", new Directive(Directive.LINE));
			parser.addDirective("define", new Directive(Directive.BLOCK));

			SimpleNode root = parser.parse(new StringReader(content), libFile.getNameExt());
			if (root == null) {
				return List.of();
			}

			MacroCollector collector = new MacroCollector();
			collector.openTransaction();
			collector.visit(root, null);
			collector.commitTransaction();
			return collector.macros;
		} catch (ParseException | java.io.IOException ex) {
			LOG.log(Level.FINE, "Failed to parse macro library: " + libFile.getNameExt(), ex);
			return List.of();
		}
	}

	public record MacroInfo(String name, List<String> params, String sourceFile) {
	}

	private record CachedResult(List<MacroInfo> macros, long timestamp) {
	}

	private static final class MacroCollector extends VelocityAnalyser {

		private final List<MacroInfo> macros = new ArrayList<>();

		@Override
		public Object visit(ASTMacroStatement node, Object data) {
			com.erudika.netbeans.velocity.jcclexer.Token macroName = nthToken(node.getFirstToken(), 2);
			String name = (macroName != null && macroName.image != null) ? macroName.image.trim() : null;
			if (name != null && !name.isEmpty()) {
				List<String> params = new ArrayList<>();
				for (int i = 0; i < node.jjtGetNumChildren(); i++) {
					if (node.jjtGetChild(i) instanceof ASTIdentifier child) {
						if (child.getFirstToken() != null && child.getFirstToken().image != null) {
							params.add(child.getFirstToken().image);
						}
					}
				}
				macros.add(new MacroInfo(name, params, null));
			}
			return node.childrenAccept(this, data);
		}

		@Override
		public void openTransaction() {
		}

		@Override
		public void commitTransaction() {
		}

		private com.erudika.netbeans.velocity.jcclexer.Token nthToken(com.erudika.netbeans.velocity.jcclexer.Token token, int offset) {
			com.erudika.netbeans.velocity.jcclexer.Token current = token;
			for (int i = 0; i < offset && current != null; i++) {
				current = current.next;
			}
			return current;
		}
	}
}
