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
import java.util.Collection;
import java.util.List;
import org.openide.filesystems.FileObject;

public class ConfiguredVelocityContextSymbolProvider implements VelocityContextSymbolProvider {

	public static ConfiguredVelocityContextSymbolProvider create() {
		return new ConfiguredVelocityContextSymbolProvider();
	}

	@Override
	public Collection<VelocityContextSymbol> getSymbols(FileObject fileObject) {
		List<VelocityContextSymbol> symbols = new ArrayList<VelocityContextSymbol>();
		for (String symbol : VTLCompletionSettings.getConfiguredSymbols()) {
			symbols.add(new VelocityContextSymbol(symbol, "Configured context variable"));
		}
		return symbols;
	}
}
