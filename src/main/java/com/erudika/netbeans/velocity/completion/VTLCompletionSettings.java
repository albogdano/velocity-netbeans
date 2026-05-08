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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public final class VTLCompletionSettings {

	static final String CONTEXT_SYMBOLS_KEY = "completion.external.symbols";
	static final String MACRO_LIBRARY_KEY = "completion.macro.library";
	static final String defaultLibraryFile = "VM_global_library.vm";
	private static volatile Set<String> overrideSymbols;
	private static volatile String overrideMacroLibrary;

	private VTLCompletionSettings() {
	}

	public static String getConfiguredMacroLibrary() {
		if (overrideMacroLibrary != null) {
			return overrideMacroLibrary;
		}
		try {
			return preferences().get(MACRO_LIBRARY_KEY, defaultLibraryFile);
		} catch (Throwable ex) {
			return "";
		}
	}

	public static void setConfiguredMacroLibrary(String value) {
		overrideMacroLibrary = (value != null && !value.isEmpty()) ? value : defaultLibraryFile;
		try {
			preferences().put(MACRO_LIBRARY_KEY, value != null ? value : defaultLibraryFile);
		} catch (Throwable ex) {
			// Ignore
		}
	}

	public static Set<String> getConfiguredSymbols() {
		if (overrideSymbols != null) {
			return new LinkedHashSet<String>(overrideSymbols);
		}

		String raw;
		try {
			raw = preferences().get(CONTEXT_SYMBOLS_KEY, "");
		} catch (Throwable ex) {
			return new LinkedHashSet<String>();
		}
		LinkedHashSet<String> values = new LinkedHashSet<String>();
		for (String token : raw.split("[,\\n\\r]+")) {
			String trimmed = token.trim();
			if (!trimmed.isEmpty()) {
				values.add(trimmed);
			}
		}
		return values;
	}

	/**
	 * Returns configured type mappings extracted from the configured symbols.
	 * Symbols in the format "$varName:com.example.Type" produce a mapping
	 * from "$varName" to "com.example.Type". Symbols without a colon are ignored.
	 */
	public static Map<String, String> getConfiguredTypeMappings() {
		LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
		for (String symbol : getConfiguredSymbols()) {
			int colonIdx = symbol.indexOf(':');
			if (colonIdx > 0 && colonIdx < symbol.length() - 1) {
				String varName = symbol.substring(0, colonIdx).trim();
				String typeName = symbol.substring(colonIdx + 1).trim();
				if (!varName.isEmpty() && !typeName.isEmpty()) {
					if (!varName.startsWith("$")) {
						varName = "$" + varName;
					}
					mappings.put(varName, typeName);
				}
			}
		}
		return mappings;
	}

	/**
	 * Extracts just the variable name from a configured symbol, stripping any
	 * type annotation. "$user:com.example.User" returns "$user".
	 */
	public static String extractVarName(String configuredSymbol) {
		if (configuredSymbol == null) {
			return null;
		}
		int colonIdx = configuredSymbol.indexOf(':');
		String varPart = colonIdx > 0 ? configuredSymbol.substring(0, colonIdx).trim() : configuredSymbol.trim();
		if (varPart.isEmpty()) {
			return null;
		}
		return varPart.startsWith("$") ? varPart : "$" + varPart;
	}

	public static void setConfiguredSymbols(Collection<String> symbols) {
		LinkedHashSet<String> values = new LinkedHashSet<String>();
		for (String symbol : symbols) {
			if (symbol != null) {
				String trimmed = symbol.trim();
				if (!trimmed.isEmpty()) {
					values.add(trimmed);
				}
			}
		}
		overrideSymbols = new LinkedHashSet<String>(values);
		try {
			preferences().put(CONTEXT_SYMBOLS_KEY, String.join("\n", values));
		} catch (Throwable ex) {
			// Ignore
		}
	}

	private static Preferences preferences() {
		return NbPreferences.forModule(VTLCompletionSettings.class);
	}
}
