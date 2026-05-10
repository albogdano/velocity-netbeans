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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VTLMacroHyperlinkProviderTest {

	private static final String SIMPLE_MACRO = "#macro(greeting $name)\nHello $name\n#end\n#greeting($user)";

	private static final String MULTIPLE_MACROS = "#macro(header)\n<h1>Title</h1>\n#end\n#macro(footer)\n<footer/>\n#end\n#header()";

	private static final String NESTED_MACROS = "#macro(outer)\n#macro(inner)\n  nested\n#end\n#end\n#outer()";

	private static final String NO_MACROS = "<html><body>Hello</body></html>";

	@Test
	void findMacroInText_simpleMacro() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("greeting", SIMPLE_MACRO);
		assertNotNull(info);
		assertEquals(1, info.lineNumber);
	}

	@Test
	void findMacroInText_notFound() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("nonexistent", SIMPLE_MACRO);
		assertNull(info);
	}

	@Test
	void findMacroInText_multipleMacros_firstMacro() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("header", MULTIPLE_MACROS);
		assertNotNull(info);
		assertEquals(1, info.lineNumber);
	}

	@Test
	void findMacroInText_multipleMacros_secondMacro() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("footer", MULTIPLE_MACROS);
		assertNotNull(info);
		assertEquals(4, info.lineNumber);
	}

	@Test
	void findMacroInText_noMacros() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("anything", NO_MACROS);
		assertNull(info);
	}

	@Test
	void findMacroInText_nullInput() {
		assertNull(VTLMacroHyperlinkProvider.findMacroInText(null, SIMPLE_MACRO));
		assertNull(VTLMacroHyperlinkProvider.findMacroInText("greeting", null));
	}

	@Test
	void findMacroInText_nestedMacros_outer() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("outer", NESTED_MACROS);
		assertNotNull(info);
		assertEquals(1, info.lineNumber);
	}

	@Test
	void findMacroInText_nestedMacros_inner() {
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("inner", NESTED_MACROS);
		assertNotNull(info);
		assertEquals(2, info.lineNumber);
	}

	@Test
	void findMacroInText_macroWithConditionals() {
		String text = "#set($x = 1)\n#macro(check $val)\n#if($val)\n  yes\n#end\n#end\n#check(true)";
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("check", text);
		assertNotNull(info);
		assertEquals(2, info.lineNumber);
	}

	@Test
	void findMacroInText_lexicalFallback() {
		String brokenText = "#macro(myMacro $arg\nmissing paren\n#end\n#myMacro()";
		VTLMacroHyperlinkProvider.DefinitionInfo info = VTLMacroHyperlinkProvider.findMacroInText("myMacro", brokenText);
		assertNotNull(info);
		assertEquals(1, info.lineNumber);
	}
}