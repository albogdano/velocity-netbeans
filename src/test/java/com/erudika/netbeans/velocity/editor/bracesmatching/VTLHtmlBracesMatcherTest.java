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
package com.erudika.netbeans.velocity.editor.bracesmatching;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VTLHtmlBracesMatcherTest {

	@Test
	void voidElementsAreRecognized() {
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("br"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("hr"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("img"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("input"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("meta"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("link"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("area"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("base"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("col"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("embed"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("source"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("track"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("wbr"));
	}

	@Test
	void nonVoidElementsAreNotRecognized() {
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("div"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("span"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("p"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("table"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("a"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("form"));
	}

	@Test
	void voidElementDetectionIsCaseInsensitive() {
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("BR"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("Img"));
		assertTrue(VTLHtmlBracesMatcher.isVoidElement("INPUT"));
	}

	@Test
	void nonVoidElementCaseInsensitive() {
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("DIV"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("Span"));
		assertFalse(VTLHtmlBracesMatcher.isVoidElement("P"));
	}
}