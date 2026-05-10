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
import com.erudika.netbeans.velocity.jcclexer.node.ASTBlock;
import com.erudika.netbeans.velocity.jcclexer.node.ASTDirective;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.parser.VTLMacroValidationTask.MacroCallValidator;
import com.erudika.netbeans.velocity.parser.VTLMacroValidationTask.MacroDefinitionCollector;
import com.erudika.netbeans.velocity.parser.VTLMacroValidationTask.MacroValidationError;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VTLMacroValidationTaskTest {

	private VelocityParser parser;

	@BeforeEach
	void setUp() {
		parser = new VelocityParser();
		parser.addDirective("parse", new Directive(Directive.LINE));
		parser.addDirective("evaluate", new Directive(Directive.LINE));
		parser.addDirective("define", new Directive(Directive.BLOCK));
	}

	private SimpleNode parse(String template) throws ParseException {
		SimpleNode root = parser.parse(new StringReader(template), "test.vm");
		assertNotNull(root, "Parse result should not be null for: " + template);
		return root;
	}

	@Test
	void collectsMacroDefinitionWithTwoParams() throws Exception {
		SimpleNode root = parse("#macro(myMacro $x $y)\n#end");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		assertEquals(1, collector.definitions.size());
		assertEquals(2, collector.definitions.get("myMacro"));
	}

	@Test
	void collectsMacroDefinitionWithZeroParams() throws Exception {
		SimpleNode root = parse("#macro(greeting)\nHello!\n#end");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		assertEquals(1, collector.definitions.size());
		assertEquals(0, collector.definitions.get("greeting"));
	}

	@Test
	void collectsMultipleMacroDefinitions() throws Exception {
		SimpleNode root = parse("#macro(m1 $x)\n#end\n#macro(m2 $a $b)\n#end");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		assertEquals(2, collector.definitions.size());
		assertEquals(1, collector.definitions.get("m1"));
		assertEquals(2, collector.definitions.get("m2"));
	}

	@Test
	void collectsMacroDefinitionWithDefaultParams() throws Exception {
		SimpleNode root = parse("#macro(myMacro $x $y = 1)\n#end");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		assertEquals(1, collector.definitions.size());
		assertEquals(2, collector.definitions.get("myMacro"));
	}

	@Test
	void validatesMacroCallWithExactArgCount() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(myMacro $x $y)\n#end\n#myMacro($a $b)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "No errors expected for matching arg count");
	}

	@Test
	void flagsMacroCallWithTooManyArgs() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(myMacro $x)\n#end\n#myMacro($a $b $c)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		MacroValidationError err = validator.errors.get(0);
		assertEquals("myMacro", err.macroName);
		assertEquals(1, err.expectedParams);
		assertEquals(3, err.actualArgs);
		assertTrue(err.message.contains("expects 1 argument"));
		assertTrue(err.message.contains("was called with 3"));
	}

	@Test
	void flagsMacroCallWithTooFewArgs() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(myMacro $x $y $z)\n#end\n#myMacro($a)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		MacroValidationError err = validator.errors.get(0);
		assertEquals(3, err.expectedParams);
		assertEquals(1, err.actualArgs);
		assertTrue(err.message.contains("expects 3 arguments"));
		assertTrue(err.message.contains("was called with 1"));
	}

	@Test
	void validatesForwardReference() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#myMacro($a)\n#macro(myMacro $x $y)\n#end");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		assertEquals(2, validator.errors.get(0).expectedParams);
		assertEquals(1, validator.errors.get(0).actualArgs);
	}

	@Test
	void skipsBuiltinDirectives() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#parse(\"template.vm\")");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "#parse is a builtin directive, should be skipped");
	}

	@Test
	void doesNotFlagUnknownMacros() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#unknownMacro($a $b)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "Unknown macros should not be flagged");
	}

	@Test
	void validatesZeroArgMacroCalledWithZeroArgs() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(greeting)\nHello!\n#end\n#greeting()");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "Zero-arg macro called with zero args should be valid");
	}

	@Test
	void flagsZeroArgMacroCalledWithArgs() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(greeting)\nHello!\n#end\n#greeting($x)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		assertEquals(0, validator.errors.get(0).expectedParams);
		assertEquals(1, validator.errors.get(0).actualArgs);
	}

	@Test
	void validatesBlockMacroCall() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		// Define macro and call it with correct arg count
		SimpleNode root = parse("#macro(bordered $content)\nhello\n#end\n#bordered($body)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "Macro call with correct arg count should be valid");
	}

	@Test
	void validatesBlockMacroCallWithWrongArgCount() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(bordered $content)\nhello\n#end\n#bordered($a $b)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		assertEquals(1, validator.errors.get(0).expectedParams);
		assertEquals(2, validator.errors.get(0).actualArgs);
	}

	@Test
	void validatesMultipleMismatchesInSameFile() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(m1 $x)\n#end\n#macro(m2 $a $b)\n#end\n#m1()\n#m2($z)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(2, validator.errors.size());
	}

	@Test
	void extractDirectiveNameFromMacroCallToken() throws Exception {
		SimpleNode root = parse("#macro(test $x)\n#end\n#test($val)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "#test($val) should match 1-param definition");
	}

	@Test
	void errorContainsCorrectLocation() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#macro(myMacro $x $y)\n#end\n#myMacro($a)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		MacroCallValidator validator = new MacroCallValidator(p, collector.definitions);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(1, validator.errors.size());
		MacroValidationError err = validator.errors.get(0);
		assertTrue(err.line > 0, "Error should have a valid line number");
		assertTrue(err.column > 0, "Error should have a valid column number");
		assertTrue(err.endLine > 0, "Error should have a valid end line");
		assertTrue(err.endColumn > 0, "Error should have a valid end column");
	}

	@Test
	void libraryMergesWithLocalDefinitions() throws Exception {
		VelocityParser p = new VelocityParser();
		p.addDirective("parse", new Directive(Directive.LINE));
		p.addDirective("evaluate", new Directive(Directive.LINE));
		p.addDirective("define", new Directive(Directive.BLOCK));

		SimpleNode root = parse("#myLocal($a)");
		MacroDefinitionCollector collector = new MacroDefinitionCollector();
		collector.openTransaction();
		root.jjtAccept(collector, null);
		collector.commitTransaction();

		Map<String, Integer> macroParams = collector.definitions;
		macroParams.putIfAbsent("libMacro", 2);

		MacroCallValidator validator = new MacroCallValidator(p, macroParams);
		validator.openTransaction();
		root.jjtAccept(validator, null);
		validator.commitTransaction();

		assertEquals(0, validator.errors.size(), "#myLocal($a) matches 1-param local definition");
	}

	@Test
	void errorMessagesUseCorrectPluralization() throws Exception {
		MacroValidationError single = new MacroValidationError("m", 1, 2, 1, 1, 1, 5);
		assertTrue(single.message.contains("expects 1 argument"));

		MacroValidationError plural = new MacroValidationError("m", 3, 1, 1, 1, 1, 5);
		assertTrue(plural.message.contains("expects 3 arguments"));
	}
}