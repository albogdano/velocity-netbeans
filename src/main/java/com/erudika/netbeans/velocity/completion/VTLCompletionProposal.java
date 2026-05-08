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

import java.util.Objects;

final class VTLCompletionProposal {

	private final String name;
	private final String insertText;
	private final String description;
	private final VTLCompletionItem.ItemType type;
	private final int sortPriority;
	private final int replaceOffset;
	private final int replaceLength;

	VTLCompletionProposal(String name, String insertText, String description, VTLCompletionItem.ItemType type,
			int sortPriority, int replaceOffset, int replaceLength) {
		this.name = name;
		this.insertText = insertText;
		this.description = description;
		this.type = type;
		this.sortPriority = sortPriority;
		this.replaceOffset = replaceOffset;
		this.replaceLength = replaceLength;
	}

	String getName() {
		return name;
	}

	String getInsertText() {
		return insertText;
	}

	String getDescription() {
		return description;
	}

	VTLCompletionItem.ItemType getType() {
		return type;
	}

	int getSortPriority() {
		return sortPriority;
	}

	int getReplaceOffset() {
		return replaceOffset;
	}

	int getReplaceLength() {
		return replaceLength;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 17 * hash + Objects.hashCode(this.name);
		hash = 17 * hash + Objects.hashCode(this.insertText);
		hash = 17 * hash + Objects.hashCode(this.type);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final VTLCompletionProposal other = (VTLCompletionProposal) obj;
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (!Objects.equals(this.insertText, other.insertText)) {
			return false;
		}
		return this.type == other.type;
	}


}
