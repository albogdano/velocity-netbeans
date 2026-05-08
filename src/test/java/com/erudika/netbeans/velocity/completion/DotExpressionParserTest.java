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
import org.junit.jupiter.api.Test;

class DotExpressionParserTest {

	@Test
	void parsesSimpleDotExpression() {
		// "$user." → base=$user, chain=[], filter=""
		String text = "$user.";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertTrue(result.methodChain().isEmpty());
		assertEquals("", result.filter());
	}

	@Test
	void parsesPartialPropertyName() {
		// "$user.na" → base=$user, chain=[], filter="na"
		String text = "$user.na";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertTrue(result.methodChain().isEmpty());
		assertEquals("na", result.filter());
	}

	@Test
	void parsesMethodChain() {
		// "$user.getAddress()." → base=$user, chain=["getAddress()"], filter=""
		String text = "$user.getAddress().";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(1, result.methodChain().size());
		assertEquals("getAddress()", result.methodChain().get(0));
		assertEquals("", result.filter());
	}

	@Test
	void parsesMethodChainWithFilter() {
		// "$user.getName().to" → base=$user, chain=["getName()"], filter="to"
		String text = "$user.getName().to";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(1, result.methodChain().size());
		assertEquals("getName()", result.methodChain().get(0));
		assertEquals("to", result.filter());
	}

	@Test
	void parsesMultiLevelChain() {
		// "$user.getAddress().getCity()." → base=$user, chain=["getAddress()","getCity()"], filter=""
		String text = "$user.getAddress().getCity().";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(2, result.methodChain().size());
		assertEquals("getAddress()", result.methodChain().get(0));
		assertEquals("getCity()", result.methodChain().get(1));
		assertEquals("", result.filter());
	}

	@Test
	void handlesExclamationPrefix() {
		// "$!user." → base=$user, chain=[], filter=""
		String text = "$!user.";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertTrue(result.methodChain().isEmpty());
		assertEquals("", result.filter());
	}

	@Test
	void handlesBracePrefix() {
		// "${user." → base=$user, chain=[], filter=""
		String text = "${user.";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertTrue(result.methodChain().isEmpty());
		assertEquals("", result.filter());
	}

	@Test
	void handlesBangBracePrefix() {
		// "$!{user.getName()." → base=$user, chain=["getName()"], filter=""
		String text = "$!{user.getName().";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(1, result.methodChain().size());
		assertEquals("getName()", result.methodChain().get(0));
		assertEquals("", result.filter());
	}

	@Test
	void returnsNullForNonReference() {
		// "hello." → no $ prefix, returns null
		String text = "hello.";
		var result = DotExpressionParser.parse(text, text.length());
		assertNull(result);
	}

	@Test
	void returnsNullForJustDollar() {
		// "$" → no dot, returns null
		String text = "$";
		var result = DotExpressionParser.parse(text, text.length());
		assertNull(result);
	}

	@Test
	void worksWithSurroundingText() {
		// "Hello $user.getName()." within larger text
		String text = "Hello $user.getName().";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(1, result.methodChain().size());
		assertEquals("getName()", result.methodChain().get(0));
		assertEquals("", result.filter());
	}

	@Test
	void parsesPropertyChainWithoutParens() {
		// "$user.address." → base=$user, chain=["address"], filter=""
		String text = "$user.address.";
		var result = DotExpressionParser.parse(text, text.length());
		assertNotNull(result);
		assertEquals("$user", result.baseVar());
		assertEquals(1, result.methodChain().size());
		assertEquals("address", result.methodChain().get(0));
		assertEquals("", result.filter());
	}
}
