package com.erudika.netbeans.velocity.editor.bracesmatching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VTLBracesMatcherTest {

	@Test
	void trimsTrailingWhitespaceFromHighlightedRange() {
		assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end   "));
		assertEquals(5, VTLBracesMatcher.getHighlightedLength("#if()   "));
		assertEquals(1, VTLBracesMatcher.getHighlightedLength(" "));
	}

	@Test
	void returnsZeroForEmptyInput() {
		assertEquals(0, VTLBracesMatcher.getHighlightedLength(""));
		assertEquals(0, VTLBracesMatcher.getHighlightedLength(null));
	}

	@Test
	void returnsFullLengthForNoTrailingWhitespace() {
		assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end"));
		assertEquals(8, VTLBracesMatcher.getHighlightedLength("#foreach"));
		assertEquals(3, VTLBracesMatcher.getHighlightedLength("#if"));
		assertEquals(2, VTLBracesMatcher.getHighlightedLength("$x"));
	}

	@Test
	void handlesLeadingWhitespace() {
		assertEquals(5, VTLBracesMatcher.getHighlightedLength(" #end  "));
	}

	@Test
	void returnsMinOneForAllWhitespace() {
		assertEquals(1, VTLBracesMatcher.getHighlightedLength("   "));
	}

	@Test
	void handlesDirectiveWithParens() {
		assertEquals(6, VTLBracesMatcher.getHighlightedLength("#set()"));
		assertEquals(6, VTLBracesMatcher.getHighlightedLength("#set() "));
	}

	@Test
	void handlesEndKeyword() {
		assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end"));
		assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end\t"));
		assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end\n"));
	}

	@Test
	void handlesMultilineContent() {
		assertEquals(3, VTLBracesMatcher.getHighlightedLength("#if"));
		assertEquals(8, VTLBracesMatcher.getHighlightedLength("#foreach"));
		assertEquals(6, VTLBracesMatcher.getHighlightedLength("#macro"));
	}
}