package com.erudika.netbeans.velocity.embedding;

public final class HTMLEmbeddingSupport
{
   private HTMLEmbeddingSupport()
   {
   }

   public static boolean containsHtmlContent(final CharSequence text)
   {
      if (text == null || text.length() < 3)
         return(false);

      for (int i = 0; i < text.length() - 2; i++)
      {
         if (text.charAt(i) != '<')
            continue;

         int j = i + 1;
         while (j < text.length() && Character.isWhitespace(text.charAt(j)))
            ++j;

         if (j < text.length() && (text.charAt(j) == '/' || text.charAt(j) == '!' || text.charAt(j) == '?'))
            ++j;

         if (j < text.length() && Character.isLetter(text.charAt(j)))
         {
            final int closingBracket = indexOf(text, '>', j + 1);
            if (closingBracket > j)
               return(true);
         }
      }

      return(false);
   }

   public static boolean mayContainHtmlMarkup(final CharSequence text)
   {
      if (text == null || text.length() == 0)
         return(false);

      if (looksLikeHtmlTagContinuation(text))
         return(true);

      for (int i = 0; i < text.length(); i++)
      {
         final char ch = text.charAt(i);

         if (ch == '<')
         {
            if (i + 1 < text.length())
            {
               final char next = text.charAt(i + 1);
               if (Character.isLetter(next) || next == '/' || next == '!' || next == '?')
                  return(true);
            }
         }
         else if (ch == '&')
            return(true);
         else if (ch == '>')
            return(true);
      }

      return(false);
   }

   private static boolean looksLikeHtmlTagContinuation(final CharSequence text)
   {
      int index = skipWhitespace(text, 0);
      if (index >= text.length())
         return(false);

      final char first = text.charAt(index);
      if (first == '"' || first == '\'')
      {
         index = skipWhitespace(text, index + 1);
         if (index >= text.length())
            return(false);
      }

      if (matches(text, index, "/>") || text.charAt(index) == '>')
         return(true);

      if (!Character.isLetter(text.charAt(index)))
         return(false);

      int end = index + 1;
      while (end < text.length() && isHtmlNameChar(text.charAt(end)))
         ++end;

      final int next = skipWhitespace(text, end);
      if (next >= text.length())
         return(false);

      final char ch = text.charAt(next);
      return(ch == '=' || ch == '>' || ch == '/');
   }

   private static boolean isHtmlNameChar(final char ch)
   {
      return(Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':' || ch == '.');
   }

   private static boolean matches(final CharSequence text, final int index, final String value)
   {
      if (index + value.length() > text.length())
         return(false);

      for (int i = 0; i < value.length(); i++)
      {
         if (text.charAt(index + i) != value.charAt(i))
            return(false);
      }

      return(true);
   }

   private static int skipWhitespace(final CharSequence text, final int index)
   {
      int i = index;
      while (i < text.length() && Character.isWhitespace(text.charAt(i)))
         ++i;

      return(i);
   }

   private static int indexOf(final CharSequence text, final char target, final int start)
   {
      for (int i = start; i < text.length(); i++)
      {
         if (text.charAt(i) == target)
            return(i);
      }

      return(-1);
   }
}
