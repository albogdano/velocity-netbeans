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
package com.erudika.netbeans.velocity.parser;

import com.erudika.netbeans.velocity.jcclexer.Directive;
import com.erudika.netbeans.velocity.jcclexer.ParseException;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityParserTest {

	@Test
	void parsesSetExpressionsWithArithmetic() throws Exception {
		assertParsesWithoutErrors("#set($var = $other + 4)");
		assertParsesWithoutErrors("$prefix$!value.substring($value.length() - 6)");
	}

	@Test
	void parsesIndexedAssignmentsInsideSet() throws Exception {
		assertParsesWithoutErrors("#set($map[$key] = \"val\")");
		assertParsesWithoutErrors("#if($foo)\n#set($map[$key] = $other + 4)\n#end");
	}

	@Test
	void parsesMacroParametersWithDefaultValues() throws Exception {
		assertParsesWithoutErrors("#macro(test $x = 1)\n#end");
		assertParsesWithoutErrors("#macro(test $x $y = $fallback)\n#end");
	}

	private void assertParsesWithoutErrors(String template) throws ParseException {
		VelocityParser parser = new VelocityParser();
		parser.addDirective("parse", new Directive(Directive.LINE));
		parser.addDirective("evaluate", new Directive(Directive.LINE));
		parser.addDirective("define", new Directive(Directive.BLOCK));
		parser.parse(new StringReader(template), "test.vm");
		assertEquals(0, parser.getSyntaxErrors().size(), () -> "Unexpected parser errors for template:\n" + template);
	}
}
