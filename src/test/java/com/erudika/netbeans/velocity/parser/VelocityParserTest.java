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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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

	@Test
	void parsesHashInHtmlAttributes() throws Exception {
		assertParsesWithoutErrors("<a href=\"#spaces-tab\">link</a>");
		assertParsesWithoutErrors("<li #if($key == \"Core\")class=\"active\"#end>");
		assertParsesWithoutErrors("<li class=\"tab\"><a href=\"#section\">$!{lang.get(\"title\")}</a></li>");
		assertParsesWithoutErrors("<a href='#anchor'>link</a>");
		assertParsesWithoutErrors("<a href=\"#\">top</a>");
	}

	@Test
	void parsesBasicDirectives() throws Exception {
		assertParsesWithoutErrors("#set($x = 1)");
		assertParsesWithoutErrors("#if($x)\n#end");
		assertParsesWithoutErrors("#if($x)\n#else\n#end");
		assertParsesWithoutErrors("#if($x)\n#elseif($y)\n#end");
		assertParsesWithoutErrors("#foreach($item in $list)\n#end");
		assertParsesWithoutErrors("#macro(myMacro $arg)\n#end");
	}

	@Test
	void parsesSetWithVariousExpressions() throws Exception {
		assertParsesWithoutErrors("#set($x = $y)");
		assertParsesWithoutErrors("#set($x = $y + 1)");
		assertParsesWithoutErrors("#set($x = $y - 1)");
		assertParsesWithoutErrors("#set($x = $y * 2)");
		assertParsesWithoutErrors("#set($x = $y / 2)");
		assertParsesWithoutErrors("#set($x = $y % 3)");
		assertParsesWithoutErrors("#set($x = !$y)");
		assertParsesWithoutErrors("#set($x = $y && $z)");
		assertParsesWithoutErrors("#set($x = $y || $z)");
		assertParsesWithoutErrors("#set($x = true)");
		assertParsesWithoutErrors("#set($x = false)");
		assertParsesWithoutErrors("#set($x = \"hello\")");
	}

	@Test
	void parsesIfElseChains() throws Exception {
		assertParsesWithoutErrors("#if($a)\nA\n#elseif($b)\nB\n#else\nC\n#end");
	}

	@Test
	void parsesNestedBlocks() throws Exception {
		assertParsesWithoutErrors("#if($a)\n#if($b)\n#end\n#end");
		assertParsesWithoutErrors("#foreach($x in $list)\n#if($x.active)\n#end\n#end");
		assertParsesWithoutErrors("#macro(myMacro)\n#if($cond)\n#end\n#end");
	}

	@Test
	void parsesMethodCalls() throws Exception {
		assertParsesWithoutErrors("$user.getName()");
		assertParsesWithoutErrors("$user.setName(\"test\")");
		assertParsesWithoutErrors("$!user.getName()");
		assertParsesWithoutErrors("${user.getName()}");
	}

	@Test
	void parsesReferenceForms() throws Exception {
		assertParsesWithoutErrors("$simple");
		assertParsesWithoutErrors("$!simple");
		assertParsesWithoutErrors("${formal}");
		assertParsesWithoutErrors("$!{formalBang}");
		assertParsesWithoutErrors("$object.property");
		assertParsesWithoutErrors("$object.property.method()");
	}

	@Test
	void parsesStringLiterals() throws Exception {
		assertParsesWithoutErrors("#set($x = \"hello world\")");
		assertParsesWithoutErrors("#set($x = 'single quoted')");
	}

	@Test
	void parsesForeachDirective() throws Exception {
		assertParsesWithoutErrors("#foreach($item in $list)\n$item\n#end");
	}

	@Test
	void parsesIncludeAndParse() throws Exception {
		assertParsesWithoutErrors("#include(\"template.vm\")");
	}

	@Test
	void parsesEvaluatedExpressions() throws Exception {
		assertParsesWithoutErrors("#set($result = $a + $b)");
		assertParsesWithoutErrors("#set($result = ($a + $b) * $c)");
	}

	@Test
	void parsesHtmlMixedWithVelocity() throws Exception {
		assertParsesWithoutErrors("<html><body>#if($user)<h1>Hello $user</h1>#end</body></html>");
		assertParsesWithoutErrors("<div class=\"${cssClass}\">content</div>");
		assertParsesWithoutErrors("<input type=\"text\" value=\"$!{value}\"/>");
	}

	@Test
	void parsesComments() throws Exception {
		assertParsesWithoutErrors("## This is a single-line comment\nHello");
		assertParsesWithoutErrors("#* This is a block comment *#");
		assertParsesWithoutErrors("#* Multi-line\ncomment *#");
	}

	@Test
	void parsesStopDirective() throws Exception {
		assertParsesWithoutErrors("#stop");
		assertParsesWithoutErrors("#if($x)\n#stop\n#end");
	}

	@Test
	void parseResultIsNotNullOnValidInput() throws Exception {
		VelocityParser parser = new VelocityParser();
		parser.addDirective("parse", new Directive(Directive.LINE));
		parser.addDirective("evaluate", new Directive(Directive.LINE));
		parser.addDirective("define", new Directive(Directive.BLOCK));
		com.erudika.netbeans.velocity.jcclexer.node.SimpleNode root = parser.parse(new StringReader("#set($x = 1)"), "test.vm");
		assertNotNull(root);
	}

	@Test
	void collectsSyntaxErrorsOnInvalidInput() throws Exception {
		VelocityParser parser = new VelocityParser();
		parser.addDirective("parse", new Directive(Directive.LINE));
		parser.addDirective("evaluate", new Directive(Directive.LINE));
		parser.addDirective("define", new Directive(Directive.BLOCK));
		parser.parse(new StringReader("#if("), "test.vm");
		assertTrue(parser.getSyntaxErrors().size() > 0, "Expected syntax errors for incomplete #if(");
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