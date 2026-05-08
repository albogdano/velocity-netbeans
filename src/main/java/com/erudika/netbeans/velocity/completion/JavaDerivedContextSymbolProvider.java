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

import com.erudika.netbeans.velocity.indexing.ContextPutAnalyzer;
import com.erudika.netbeans.velocity.indexing.ContextVarScanner;
import com.erudika.netbeans.velocity.indexing.ContextVarStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

public class JavaDerivedContextSymbolProvider implements VelocityContextSymbolProvider {

	public static JavaDerivedContextSymbolProvider create() {
		return new JavaDerivedContextSymbolProvider();
	}

	@Override
	public Collection<VelocityContextSymbol> getSymbols(FileObject fileObject) {
		if (fileObject == null) {
			return List.of();
		}
		Project project = FileOwnerQuery.getOwner(fileObject);
		if (project == null) {
			return List.of();
		}
		FileObject projectDir = project.getProjectDirectory();
		if (projectDir == null) {
			return List.of();
		}

		List<ContextPutAnalyzer.ContextVarEntry> entries = ContextVarStore.load(projectDir);
		if (entries.isEmpty()) {
			ContextVarScanner.scanProject(project);
			return List.of();
		}

		List<VelocityContextSymbol> symbols = new ArrayList<>();
		for (ContextPutAnalyzer.ContextVarEntry entry : entries) {
			String description = "Java Context variable \u2192 " + entry.typeName();
			symbols.add(new VelocityContextSymbol(entry.varName(), description));
		}
		return symbols;
	}
}
