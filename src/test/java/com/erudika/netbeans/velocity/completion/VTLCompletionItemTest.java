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

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VTLCompletionItemTest {

	@Test
	void applyIndentationSingleLine() throws Exception {
		String result = invokeApplyIndentation("#set($x = 1)", "    ");
		assertEquals("#set($x = 1)", result);
	}

	@Test
	void applyIndentationMultiLine() throws Exception {
		String result = invokeApplyIndentation("#if($cond)\n\n#end", "    ");
		assertEquals("#if($cond)\n    \n    #end", result);
	}

	@Test
	void applyIndentationEmptyIndent() throws Exception {
		String result = invokeApplyIndentation("#if($cond)\n\n#end", "");
		assertEquals("#if($cond)\n\n#end", result);
	}

	@Test
	void applyIndentationTabIndent() throws Exception {
		String result = invokeApplyIndentation("#if($cond)\nbody\n#end", "\t");
		assertEquals("#if($cond)\n\tbody\n\t#end", result);
	}

	@Test
	void applyIndentationThreeLines() throws Exception {
		String result = invokeApplyIndentation("line1\nline2\nline3", "  ");
		assertEquals("line1\n  line2\n  line3", result);
	}

	@Test
	void applyIndentationPreservesFirstLine() throws Exception {
		String result = invokeApplyIndentation("first\nsecond", "----");
		assertEquals("first\n----second", result);
	}

	private static String invokeApplyIndentation(String text, String lineIndent) throws Exception {
		Method method = VTLCompletionItem.class.getDeclaredMethod("applyIndentation", String.class, String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, text, lineIndent);
	}
}