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

import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.HtmlFormatter;
import org.netbeans.modules.csl.spi.DefaultCompletionProposal;

public class VTLCompletionProposal extends DefaultCompletionProposal {

	private final String name;
	private final String insertPrefix;
	private final String description;
	private final int sortPriority;
	private final ElementKind kind;

	public VTLCompletionProposal(String name, String insertPrefix, String description, ElementKind kind, int sortPriority, int anchorOffset) {
		this.name = name;
		this.insertPrefix = insertPrefix != null ? insertPrefix : name;
		this.description = description;
		this.kind = kind;
		this.sortPriority = sortPriority;
		setAnchorOffset(anchorOffset);
	}

	public VTLCompletionProposal(String name, String insertPrefix, String description, ElementKind kind, int sortPriority) {
		this(name, insertPrefix, description, kind, sortPriority, 0);
	}

	public VTLCompletionProposal(String name, String description, ElementKind kind, int sortPriority, int anchorOffset) {
		this(name, name, description, kind, sortPriority, anchorOffset);
	}

	public VTLCompletionProposal(String name, String description, ElementKind kind, int sortPriority) {
		this(name, name, description, kind, sortPriority, 0);
	}

	public VTLCompletionProposal(String name, String description, ElementKind kind) {
		this(name, name, description, kind, 100, 0);
	}

	public String getDescription() {
		return description;
	}

	public int getSortPriority() {
		return sortPriority;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ElementHandle getElement() {
		return null;
	}

	@Override
	public String getInsertPrefix() {
		return insertPrefix;
	}

	@Override
	public String getSortText() {
		return name;
	}

	@Override
	public ElementKind getKind() {
		return kind;
	}

	@Override
	public String getLhsHtml(HtmlFormatter formatter) {
		formatter.reset();
		formatter.appendText(name);
		return formatter.getText();
	}

	@Override
	public String getRhsHtml(HtmlFormatter formatter) {
		formatter.reset();
		if (description != null && !description.isEmpty()) {
			formatter.appendText(description);
		}
		return formatter.getText();
	}

	@Override
	public int getSortPrioOverride() {
		return sortPriority;
	}

	@Override
	public String getCustomInsertTemplate() {
		return null;
	}
}
