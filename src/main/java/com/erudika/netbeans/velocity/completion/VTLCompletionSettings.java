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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public final class VTLCompletionSettings {

	static final String CONTEXT_SYMBOLS_KEY = "completion.external.symbols";
	private static volatile Set<String> overrideSymbols;

	private VTLCompletionSettings() {
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
	}

	private static Preferences preferences() {
		return NbPreferences.forModule(VTLCompletionSettings.class);
	}
}
