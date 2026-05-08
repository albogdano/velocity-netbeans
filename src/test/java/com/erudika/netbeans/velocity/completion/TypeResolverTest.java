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

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TypeResolverTest {

	@Test
	void stripGenericsSimple() {
		assertEquals("java.util.List", TypeResolver.stripGenerics("java.util.List<java.lang.String>"));
	}

	@Test
	void stripGenericsNested() {
		assertEquals("java.util.Map", TypeResolver.stripGenerics("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>"));
	}

	@Test
	void stripGenericsNoGenerics() {
		assertEquals("java.lang.String", TypeResolver.stripGenerics("java.lang.String"));
	}

	@Test
	void stripGenericsNull() {
		assertNull(TypeResolver.stripGenerics(null));
	}

	@Test
	void configuredTypeMappings() {
		// Set up test data
		VTLCompletionSettings.setConfiguredSymbols(Set.of(
				"$user:com.example.User",
				"$name",
				"$items:java.util.List<com.example.Item>"
		));

		Map<String, String> mappings = VTLCompletionSettings.getConfiguredTypeMappings();
		assertEquals("com.example.User", mappings.get("$user"));
		assertEquals("java.util.List<com.example.Item>", mappings.get("$items"));
		assertNull(mappings.get("$name")); // No type annotation
	}

	@Test
	void extractVarNameWithType() {
		assertEquals("$user", VTLCompletionSettings.extractVarName("$user:com.example.User"));
	}

	@Test
	void extractVarNameWithoutType() {
		assertEquals("$name", VTLCompletionSettings.extractVarName("$name"));
	}

	@Test
	void extractVarNameWithoutDollar() {
		assertEquals("$name", VTLCompletionSettings.extractVarName("name:java.lang.String"));
	}
}
