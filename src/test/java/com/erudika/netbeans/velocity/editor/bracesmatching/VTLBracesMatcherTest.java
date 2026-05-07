package com.erudika.netbeans.velocity.editor.bracesmatching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VTLBracesMatcherTest
{
   @Test
   void trimsTrailingWhitespaceFromHighlightedRange()
   {
      assertEquals(4, VTLBracesMatcher.getHighlightedLength("#end   "));
      assertEquals(5, VTLBracesMatcher.getHighlightedLength("#if()   "));
      assertEquals(1, VTLBracesMatcher.getHighlightedLength(" "));
   }
}
