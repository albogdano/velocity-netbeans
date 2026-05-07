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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class VTLCompletionEngineTest {

	@AfterEach
	void resetConfiguredSymbols() {
		VTLCompletionSettings.setConfiguredSymbols(List.of());
	}

	@Test
	void completesLocalReferencesAndMacros() {
		String template = "#macro(renderCard $user)\n#set($title = $user.name)\n#end\n#ren";
		List<VTLCompletionProposal> directiveProposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(directiveProposals, "#renderCard");

		String referenceTemplate = "#foreach($item in $items)\n#set($title = $item.name)\n$";
		List<VTLCompletionProposal> referenceProposals = VTLCompletionEngine.complete(referenceTemplate, referenceTemplate.length(), null, true);
		assertContains(referenceProposals, "$title");
		assertContains(referenceProposals, "$item");
	}

	@Test
	void completesConfiguredExternalSymbols() {
		VTLCompletionSettings.setConfiguredSymbols(List.of("request", "$session"));
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete("$se", 3, null, true);
		assertContains(proposals, "$session");
	}

	@Test
	void prioritizesBlockClosersInsideIfBlocks() {
		String template = "#if($foo)\n#";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "#end");
		assertContains(proposals, "#else");
		assertContains(proposals, "#elseif");
	}

	@Test
	void suggestsForeachInKeyword() {
		String template = "#foreach($item ";
		List<VTLCompletionProposal> proposals = VTLCompletionEngine.complete(template, template.length(), null, true);
		assertContains(proposals, "in");
	}

	private void assertContains(List<VTLCompletionProposal> proposals, String name) {
		assertTrue(proposals.stream().anyMatch(proposal -> proposal.getName().equals(name)),
				() -> "Expected proposal " + name + " but got " + proposals.stream().map(VTLCompletionProposal::getName).toList());
	}
}
