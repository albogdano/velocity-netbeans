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

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class VTLCompletionEngineTest {

	@AfterEach
	void resetConfiguredSymbols() {
		VTLCompletionSettings.setConfiguredSymbols(List.of());
	}

	// ---- Directive completion ----

	@Test
	void completesDirectivesAfterHash() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("#", 1, null, true);
		assertContains(proposals, "#set");
		assertContains(proposals, "#if");
		assertContains(proposals, "#foreach");
		assertContains(proposals, "#macro");
		assertContains(proposals, "#else");
		assertContains(proposals, "#elseif");
		assertContains(proposals, "#end");
		assertContains(proposals, "#include");
		assertContains(proposals, "#parse");
		assertContains(proposals, "#evaluate");
		assertContains(proposals, "#define");
		assertContains(proposals, "#break");
		assertContains(proposals, "#stop");
	}

	@Test
	void filtersDirectivesByPrefix() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("#se", 3, null, true);
		assertContains(proposals, "#set");
		assertFalse(proposals.stream().anyMatch(p -> p.getName().equals("#if")));
		assertFalse(proposals.stream().anyMatch(p -> p.getName().equals("#foreach")));
	}

	@Test
	void filtersDirectivesByPartialPrefix() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("#fo", 3, null, true);
		assertContains(proposals, "#foreach");
		assertFalse(proposals.stream().anyMatch(p -> p.getName().equals("#set")));
	}

	@Test
	void completesDirectivesWithExclamationPrefix() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("#!i", 3, null, true);
		assertContains(proposals, "#if");
	}

	// ---- Reference completion ----

	@Test
	void completesReferencesAfterDollar() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$", 1, null, true);
		assertContains(proposals, "$foreach.count");
		assertContains(proposals, "$foreach.index");
		assertContains(proposals, "$foreach.hasNext");
	}

	@Test
	void filtersReferencesByPrefix() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$forea", 5, null, true);
		assertContains(proposals, "$foreach.count");
		assertContains(proposals, "$foreach.index");
		assertFalse(proposals.stream().anyMatch(p -> p.getName().equals("$title")));
	}

	@Test
	void completesDeclaredReferencesFromSet() {
		String template = "#set($title = \"Hello\")\n$ti";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "$title");
	}

	@Test
	void completesDeclaredReferencesFromForeach() {
		String template = "#foreach($item in $list)\n$it";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "$item");
	}

//	@Test
//	void completesObservedReferences() {
//		String template = "$user.name\n$u";
//		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
//		assertContains(proposals, "$user");
//	}

	@Test
	void completesConfiguredExternalSymbols() {
		VTLCompletionSettings.setConfiguredSymbols(List.of("request", "$session"));
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$se", 3, null, true);
		assertContains(proposals, "$session");
	}

	@Test
	void completesConfiguredSymbolsWithTypeAnnotation() {
		VTLCompletionSettings.setConfiguredSymbols(List.of("$user:com.example.User"));
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$us", 3, null, true);
		assertContains(proposals, "$user");
	}

	@Test
	void prioritizesDeclaredOverObservedReferences() {
		String template = "#set($title = \"Hello\")\n$title\n$ti";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal titleProposal = proposals.stream()
				.filter(p -> "$title".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(titleProposal);
		assertEquals(10, titleProposal.getSortPriority());
	}

	// ---- Macro completion ----

	@Test
	void completesLocalMacros() {
		String template = "#macro(renderCard $user)\n#end\n#ren";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "#renderCard");
	}

	@Test
	void completesMacroWithArgumentsInInsertText() {
		String template = "#macro(test $arg1 $arg2)\n#end\n#te";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal macroProposal = proposals.stream()
				.filter(p -> "#test".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(macroProposal);
		assertTrue(macroProposal.getInsertText().contains("${1 default=\"$arg1\"}"),
				() -> "Expected first arg placeholder in: " + macroProposal.getInsertText());
		assertTrue(macroProposal.getInsertText().contains("${2 default=\"$arg2\"}"),
				() -> "Expected second arg placeholder in: " + macroProposal.getInsertText());
	}

	@Test
	void completesMacroWithArgumentsInDescription() {
		String template = "#macro(myMacro $name $value)\n#end\n#my";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal macroProposal = proposals.stream()
				.filter(p -> "#myMacro".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(macroProposal);
		assertTrue(macroProposal.getDescription().contains("myMacro($name, $value)"),
				() -> "Expected signature in description, got: " + macroProposal.getDescription());
	}

	@Test
	void completesMacroWithSingleArgument() {
		String template = "#macro(greet $who)\n#end\n#gr";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal macroProposal = proposals.stream()
				.filter(p -> "#greet".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(macroProposal);
		assertTrue(macroProposal.getInsertText().contains("${1 default=\"$who\"}"),
				() -> "Expected arg placeholder in: " + macroProposal.getInsertText());
		assertTrue(macroProposal.getDescription().contains("greet($who)"),
				() -> "Expected signature in description, got: " + macroProposal.getDescription());
	}

	@Test
	void completesMacroWithNoArgumentsStillWorks() {
		String template = "#macro(noArgs)\n#end\n#no";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal macroProposal = proposals.stream()
				.filter(p -> "#noArgs".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(macroProposal);
		assertTrue(macroProposal.getInsertText().contains("#noArgs()"),
				() -> "Expected #noArgs() in: " + macroProposal.getInsertText());
		assertEquals("Velocimacro", macroProposal.getDescription());
	}
	void completesLocalMacrosAfterHash() {
		String template = "#macro(greet $name)\n#end\n#";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "#greet");
	}

	// ---- Block context ----

	@Test
	void prioritizesEndInsideIfBlock() {
		String template = "#if($foo)\n#";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal endProposal = proposals.stream()
				.filter(p -> "#end".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(endProposal);
		assertTrue(endProposal.getSortPriority() <= 5, "#end should be high priority inside #if");
	}

	@Test
	void prioritizesElseInsideIfBlock() {
		String template = "#if($foo)\n#";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "#else");
		assertContains(proposals, "#elseif");
		VTLCompletionProposal elseProposal = proposals.stream()
				.filter(p -> "#else".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(elseProposal);
		assertTrue(elseProposal.getSortPriority() <= 10, "#else should be high priority inside #if");
	}

	@Test
	void deprioritizesEndOutsideBlock() {
		String template = "Hello #";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal endProposal = proposals.stream()
				.filter(p -> "#end".equals(p.getName()))
				.findFirst()
				.orElse(null);
		if (endProposal != null) {
			assertTrue(endProposal.getSortPriority() >= 20, "#end should be low priority outside block context");
		}
	}

	// ---- Foreach in keyword ----

	@Test
	void suggestsInKeywordInsideForeach() {
		String template = "#foreach($item ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "in");
	}

	@Test
	void suggestsInKeywordAfterForeachVariable() {
		String template = "#foreach($item i";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "in");
	}

	// ---- Expression context ----

	@Test
	void suggestsKeywordsInIfExpression() {
		String template = "#if($foo ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "and");
		assertContains(proposals, "or");
		assertContains(proposals, "not");
	}

	@Test
	void suggestsOperatorsInSetExpression() {
		String template = "#set($x = $y ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "==");
		assertContains(proposals, "!=");
	}

	@Test
	void suggestsBooleansInExpression() {
		String template = "#if($foo == ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "true");
		assertContains(proposals, "false");
	}

	@Test
	void suggestsComparisonOperators() {
		String template = "#if($foo ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "eq");
		assertContains(proposals, "ne");
		assertContains(proposals, "lt");
		assertContains(proposals, "le");
		assertContains(proposals, "gt");
		assertContains(proposals, "ge");
	}

	// ---- General completion mode ----

	@Test
	void suggestsDirectivesAndReferencesInGeneralMode() {
		String template = "Hello ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "#set");
		assertContains(proposals, "#if");
	}

	@Test
	void emptyPrefixInGeneralModeReturnsAllDirectivesAndReferences() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("", 0, null, true);
		assertFalse(proposals.isEmpty());
		assertContains(proposals, "#set");
		assertContains(proposals, "$foreach.count");
	}

	@Test
	void returnsEmptyWhenNotAllowedAndNoVelocityPrefix() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("Hello ", 6, null, false);
		assertTrue(proposals.isEmpty());
	}

	@Test
	void allowsCompletionWhenVelocityPrefixPresent() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("Hello #", 7, null, false);
		assertFalse(proposals.isEmpty());
	}

	// ---- ExtractPrefix ----

	@Test
	void extractsDirectivePrefix() {
		assertEquals("#set", VTLCompletionEngine.extractPrefix("Hello #set", 10));
	}

	@Test
	void extractsReferencePrefix() {
		assertEquals("$user", VTLCompletionEngine.extractPrefix("Hello $user", 11));
	}

	@Test
	void extractsPartialPrefix() {
		assertEquals("#fo", VTLCompletionEngine.extractPrefix("#fo", 3));
	}

	@Test
	void extractsEmptyPrefixAtWhitespace() {
		assertEquals("", VTLCompletionEngine.extractPrefix("Hello ", 6));
	}

	@Test
	void extractsExclamationReference() {
		assertEquals("$!user", VTLCompletionEngine.extractPrefix("$!user", 6));
	}

	// ---- Built-in foreach references ----

	@Test
	void completesForeachBuiltInReferencesAfterDollar() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$foreach", 8, null, true);
		assertContains(proposals, "$foreach.count");
		assertContains(proposals, "$foreach.index");
		assertContains(proposals, "$foreach.first");
		assertContains(proposals, "$foreach.last");
		assertContains(proposals, "$foreach.parent");
		assertContains(proposals, "$foreach.topmost");
		assertContains(proposals, "$foreach.hasNext");
	}

	// ---- Directive insert text has code template parameters ----

	@Test
	void directiveSetHasParametrizedInsertText() {
		String template = "#se";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal setProposal = proposals.stream()
				.filter(p -> "#set".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(setProposal);
		assertTrue(setProposal.getInsertText().contains("${VAR"));
		assertTrue(setProposal.getInsertText().contains("${cursor}"));
	}

	@Test
	void directiveIfHasParametrizedInsertText() {
		String template = "#i";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal ifProposal = proposals.stream()
				.filter(p -> "#if".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(ifProposal);
		assertTrue(ifProposal.getInsertText().contains("${COND"));
		assertTrue(ifProposal.getInsertText().contains("${cursor}"));
	}

	@Test
	void directiveForeachHasCorrectInsertText() {
		String template = "#forea";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal foreachProposal = proposals.stream()
				.filter(p -> "#foreach".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(foreachProposal);
		assertTrue(foreachProposal.getInsertText().startsWith("#foreach("));
		assertTrue(foreachProposal.getInsertText().contains("${cursor}"));
	}

	// ---- Proposal type and priority ----

	@Test
	void directivesAreTypeDIRECTIVE() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("#", 1, null, true);
		VTLCompletionProposal setProposal = proposals.stream()
				.filter(p -> "#set".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(setProposal);
		assertEquals(VTLCompletionItem.ItemType.DIRECTIVE, setProposal.getType());
	}

	@Test
	void referencesAreTypeREFERENCE() {
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$foreach", 8, null, true);
		VTLCompletionProposal countProposal = proposals.stream()
				.filter(p -> "$foreach.count".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(countProposal);
		assertEquals(VTLCompletionItem.ItemType.REFERENCE, countProposal.getType());
	}

	@Test
	void inKeywordIsTypeKEYWORD() {
		String template = "#foreach($item ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal inProposal = proposals.stream()
				.filter(p -> "in".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(inProposal);
		assertEquals(VTLCompletionItem.ItemType.KEYWORD, inProposal.getType());
	}

	// ---- Type inference ----

	@Test
	void infersStringTypeFromStringLiteral() {
		String template = "#set($name = \"hello\")\n$na";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal nameProposal = proposals.stream()
				.filter(p -> "$name".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(nameProposal);
		assertTrue(nameProposal.getDescription().toLowerCase().contains("string"),
				() -> "Expected 'String' in description but got: " + nameProposal.getDescription());
	}

	@Test
	void infersIntegerTypeFromIntegerLiteral() {
		String template = "#set($count = 42)\n$co";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal countProposal = proposals.stream()
				.filter(p -> "$count".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(countProposal);
		assertTrue(countProposal.getDescription().toLowerCase().contains("integer"),
				() -> "Expected 'Integer' in description but got: " + countProposal.getDescription());
	}

	@Test
	void infersBooleanTypeFromTrueLiteral() {
		String template = "#set($active = true)\n$ac";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal activeProposal = proposals.stream()
				.filter(p -> "$active".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(activeProposal);
		assertTrue(activeProposal.getDescription().toLowerCase().contains("boolean"),
				() -> "Expected 'Boolean' in description but got: " + activeProposal.getDescription());
	}

	@Test
	void infersDoubleTypeFromFloatLiteral() {
		String template = "#set($price = 3.14)\n$pr";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal priceProposal = proposals.stream()
				.filter(p -> "$price".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(priceProposal);
		assertTrue(priceProposal.getDescription().toLowerCase().contains("double"),
				() -> "Expected 'Double' in description but got: " + priceProposal.getDescription());
	}

	@Test
	void infersBooleanFromComparisonExpression() {
		String template = "#set($flag = $a == $b)\n$fl";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal flagProposal = proposals.stream()
				.filter(p -> "$flag".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(flagProposal);
		assertTrue(flagProposal.getDescription().toLowerCase().contains("boolean"),
				() -> "Expected 'Boolean' in description but got: " + flagProposal.getDescription());
	}

	@Test
	void infersListTypeFromObjectArray() {
		String template = "#set($items = [1, 2, 3])\n$it";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal itemsProposal = proposals.stream()
				.filter(p -> "$items".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(itemsProposal);
		assertTrue(itemsProposal.getDescription().toLowerCase().contains("list"),
				() -> "Expected 'List' in description but got: " + itemsProposal.getDescription());
	}

	@Test
	void infersMapTypeFromMapLiteral() {
		String template = "#set($data = {\"key\": \"val\"})\n$da";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal dataProposal = proposals.stream()
				.filter(p -> "$data".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(dataProposal);
		assertTrue(dataProposal.getDescription().toLowerCase().contains("map"),
				() -> "Expected 'Map' in description but got: " + dataProposal.getDescription());
	}

	@Test
	void infersStringFromAddWithString() {
		String template = "#set($msg = $a + \"!\")\n$ms";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal msgProposal = proposals.stream()
				.filter(p -> "$msg".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(msgProposal);
		assertTrue(msgProposal.getDescription().toLowerCase().contains("string"),
				() -> "Expected 'String' in description but got: " + msgProposal.getDescription());
	}

	@Test
	void fallsBackToLocalVariableDescriptionWhenTypeUnknown() {
		String template = "#set($x = $unknown)\n$x";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		VTLCompletionProposal xProposal = proposals.stream()
				.filter(p -> "$x".equals(p.getName()))
				.findFirst()
				.orElse(null);
		assertNotNull(xProposal);
		assertTrue(xProposal.getDescription().toLowerCase().contains("local variable"),
				() -> "Expected 'Local variable' in description but got: " + xProposal.getDescription());
	}

	// ---- Helpers ----

	private void assertContains(List<VTLCompletionProposal> proposals, String name) {
		assertTrue(proposals.stream().anyMatch(proposal -> proposal.getName().equals(name)),
				() -> "Expected proposal '" + name + "' but got: " + proposals.stream().map(VTLCompletionProposal::getName).toList());
	}
}