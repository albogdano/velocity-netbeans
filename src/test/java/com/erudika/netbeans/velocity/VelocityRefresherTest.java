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
package com.erudika.netbeans.velocity;

import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VelocityRefresherTest {

	@Test
	void clearLibraryMacroNamesRemovesAllEntries() {
		VelocityParser.addLibraryMacroName("testMacro1");
		VelocityParser.addLibraryMacroName("testMacro2");
		assertTrue(VelocityParser.isMacro("testMacro1"));
		assertTrue(VelocityParser.isMacro("testMacro2"));

		VelocityParser.clearLibraryMacroNames();

		assertFalse(VelocityParser.isMacro("testMacro1"));
		assertFalse(VelocityParser.isMacro("testMacro2"));
	}

	@Test
	void clearThenReRegisterLibraryMacros() {
		VelocityParser.addLibraryMacroName("oldMacro");
		assertTrue(VelocityParser.isMacro("oldMacro"));

		VelocityParser.clearLibraryMacroNames();
		VelocityParser.addLibraryMacroName("newMacro");

		assertFalse(VelocityParser.isMacro("oldMacro"));
		assertTrue(VelocityParser.isMacro("newMacro"));

		VelocityParser.clearLibraryMacroNames();
	}

	@Test
	void clearLibraryMacroNamesDoesNotAffectFileMacros() {
		VelocityParser.clearLibraryMacroNames();
		VelocityParser.addMacroName("fileMacro");
		assertTrue(VelocityParser.isMacro("fileMacro"));

		VelocityParser.clearLibraryMacroNames();
		assertTrue(VelocityParser.isMacro("fileMacro"));

		VelocityParser.clearLibraryMacroNames();
	}

	@Test
	void libraryMacroNamesSizeTracksAdditions() {
		VelocityParser.clearLibraryMacroNames();
		assertEquals(0, VelocityParser.libraryMacroNamesSize());

		VelocityParser.addLibraryMacroName("macro1");
		assertEquals(1, VelocityParser.libraryMacroNamesSize());

		VelocityParser.addLibraryMacroName("macro2");
		VelocityParser.addLibraryMacroName("macro3");
		assertEquals(3, VelocityParser.libraryMacroNamesSize());

		VelocityParser.clearLibraryMacroNames();
		assertEquals(0, VelocityParser.libraryMacroNamesSize());
	}

	@Test
	void libraryMacroNamesSizeDetectsGrowth() {
		VelocityParser.clearLibraryMacroNames();
		int before = VelocityParser.libraryMacroNamesSize();

		VelocityParser.addLibraryMacroName("newMacro");
		int after = VelocityParser.libraryMacroNamesSize();

		assertTrue(after > before, "Size should grow after adding a macro");

		VelocityParser.clearLibraryMacroNames();
	}
}