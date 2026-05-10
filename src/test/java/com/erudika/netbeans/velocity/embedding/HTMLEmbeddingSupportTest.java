package com.erudika.netbeans.velocity.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HTMLEmbeddingSupportTest
{
   @Test
   void detectsRealHtmlTags()
   {
      assertTrue(HTMLEmbeddingSupport.containsHtmlContent("<div class=\"hero\">Hello</div>"));
      assertTrue(HTMLEmbeddingSupport.containsHtmlContent("before <span>value</span> after"));
   }

   @Test
   void ignoresPlainVelocityExpressions()
   {
      assertFalse(HTMLEmbeddingSupport.containsHtmlContent("$prefix$!value.substring($value.length() - 6)"));
      assertFalse(HTMLEmbeddingSupport.containsHtmlContent("#set($foo = $bar + 1)"));
      assertFalse(HTMLEmbeddingSupport.containsHtmlContent("plain text only"));
   }

   @Test
   void detectsPartialHtmlMarkupForHighlighting()
   {
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("<div class=\"hero\""));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("</section>"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("&nbsp;"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("\" class=\"new-comment-form\">"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup(" id=\"id\">"));
      assertFalse(HTMLEmbeddingSupport.mayContainHtmlMarkup("$prefix$!value.substring($value.length() - 6)"));
   }

   @Test
   void detectsTagContinuationsAfterVtlHashSplit()
   {
      // When VTL lexer splits TEXT at '#' inside href="#anchor",
      // the second TEXT token starts with '#anchor">' — must be detected as HTML
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("#spaces-tab\">"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("#section\">some text</a>"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup("value\">"));
      assertTrue(HTMLEmbeddingSupport.mayContainHtmlMarkup(">text</div>"));
      // Pure VTL content without HTML should not be detected
      assertFalse(HTMLEmbeddingSupport.mayContainHtmlMarkup("#set($x = 1)"));
      assertFalse(HTMLEmbeddingSupport.mayContainHtmlMarkup("plain text"));
   }
}
