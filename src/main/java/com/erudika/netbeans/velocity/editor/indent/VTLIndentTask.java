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
package com.erudika.netbeans.velocity.editor.indent;

import javax.swing.text.BadLocationException;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.IndentTask;

class VTLIndentTask implements IndentTask {

	private final Context context;

	VTLIndentTask(Context context) {
		this.context = context;
	}

	@Override
	public void reindent() throws BadLocationException {
		int caretOffset = context.caretOffset();
		int lineStart = context.lineStartOffset(caretOffset);

		if (lineStart == 0) {
			context.modifyIndent(lineStart, 0);
			return;
		}

		int prevLineEnd = lineStart - 1;
		int prevLineStart = context.lineStartOffset(prevLineEnd);
		int prevIndent = context.lineIndent(prevLineStart);

		context.modifyIndent(lineStart, prevIndent);
	}

	@Override
	public ExtraLock indentLock() {
		return null;
	}
}