/*
 * $Id: VTLBracesMatcher.java 26 2015-08-17 07:25:55Z wejaeger $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor.bracesmatching;

import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.lexer.VTLTokenId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

/**
 * Implements a VTL braces matcher.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLBracesMatcher implements BracesMatcher, BracesMatcherFactory
{
   private final MatcherContext m_Context;

   private int     m_iOriginOffset;
   private boolean m_fBackward;

   /**
    * Creates new {@code VTLBracesMatcher}.
    */
   public VTLBracesMatcher()
   {
      this(null);
   }

   /**
    * Creates new {@code VTLBracesMatcher}.
    */
   private VTLBracesMatcher(final MatcherContext mc)
   {
      m_Context = mc;
   }

   /**
    * {@inheritDoc}
    */
   @Override public int[] findOrigin() throws InterruptedException, BadLocationException
   {
      final int[] aiOrigins;
      final AbstractDocument doc = (AbstractDocument)m_Context.getDocument();

      doc.readLock();

      try
      {
         final TokenHierarchy<?>                      localTokenHierarchy = TokenHierarchy.get(m_Context.getDocument());
         final List<TokenSequence<? extends TokenId>> localList           = getTokenSequences(localTokenHierarchy, m_Context.getSearchOffset(), VTLTokenId.getLanguage());

         if (!localList.isEmpty())
         {
            final TokenSequence<? extends TokenId> ts    = localList.get(localList.size() - 1);
            final Token<? extends TokenId>         token = ts.offsetToken();

            switch (token.id().ordinal())
            {
               case VelocityParserConstants.ELSEIF_DIRECTIVE:
               case VelocityParserConstants.ELSE_DIRECTIVE:
               case VelocityParserConstants.FOREACH_DIRECTIVE:
               case VelocityParserConstants.IF_DIRECTIVE:
               case VelocityParserConstants.MACRO_DIRECTIVE:
                  aiOrigins       = createMatchRange(ts.offset(), token);
                  m_iOriginOffset = aiOrigins[0];
                  m_fBackward     = false;
                  break;

               case VelocityParserConstants.END:
                  aiOrigins       = createMatchRange(ts.offset(), token);
                  m_iOriginOffset = aiOrigins[0];
                  m_fBackward     = true;
                  break;

               default:
                  aiOrigins = null;
            }
         }
         else
            aiOrigins = null;
      }
      finally
      {
         doc.readUnlock();
      }

      return(aiOrigins);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int[] findMatches() throws InterruptedException, BadLocationException
   {
      final AbstractDocument doc = (AbstractDocument)m_Context.getDocument();
      int[] aiMatches = null;

      doc.readLock();

      try
      {
         final TokenHierarchy<?>                      localTokenHierarchy = TokenHierarchy.get(m_Context.getDocument());
         final List<TokenSequence<? extends TokenId>> localList           = getTokenSequences(localTokenHierarchy, m_iOriginOffset, VTLTokenId.getLanguage());

         if (!localList.isEmpty())
         {
            final TokenSequence<? extends TokenId> ts = localList.get(localList.size() - 1);

            ts.move(m_iOriginOffset);

            final boolean fHasNext;
            if (m_fBackward)
               fHasNext = true;
            else
               fHasNext = ts.moveNext();

            if (fHasNext)
            {
               int iLevel = 0;
               while ((iLevel >= 0) && (m_fBackward ? ts.movePrevious() : ts.moveNext()))
               {
                  final Token<? extends TokenId> token = ts.offsetToken();

                  switch (token.id().ordinal())
                  {
                     case VelocityParserConstants.ELSEIF_DIRECTIVE:
                     case VelocityParserConstants.ELSE_DIRECTIVE:
                        break;

                     case VelocityParserConstants.FOREACH_DIRECTIVE:
                     case VelocityParserConstants.IF_DIRECTIVE:
                     case VelocityParserConstants.MACRO_DIRECTIVE:
                        if (m_fBackward)
                        {
                           if (iLevel == 0)
                              aiMatches = createMatchRange(ts.offset(), token);

                           iLevel--;
                        }
                        else
                           iLevel++;

                        break;

                     case VelocityParserConstants.END:
                        if (!m_fBackward)
                        {
                           if (iLevel == 0)
                              aiMatches = createMatchRange(ts.offset(), token);

                           iLevel--;
                        }
                        else
                           iLevel++;

                        break;

                     default:
                  }
               }
            }
         }
      }
      finally
      {
         doc.readUnlock();
      }

      return(aiMatches);
   }

   /**
    * Creates a {@link VTLBracesMatcher} for searching a VTL document for
    * matching areas.
    *
    * @param mc The context to use for searching. It contains the position of
    *        a caret in a document and allows to report results.
    *
    * @return a reference to a newly created {@link VTLBracesMatcher} object.
    */
   @Override public BracesMatcher createMatcher(final MatcherContext mc)
   {
      return(new VTLBracesMatcher(mc));
   }

   private static List<TokenSequence<? extends TokenId>> getTokenSequences(final TokenHierarchy<?> tokenHierarchy, final int iOriginOffset, final Language<? extends TokenId> language)
   {
      final List<TokenSequence<?>> localList = tokenHierarchy.embeddedTokenSequences(iOriginOffset, false);

      for (int i = localList.size() - 1; i >= 0; --i)
      {
         final TokenSequence<?> localTokenSequence = localList.get(i);
         if (localTokenSequence.language() == language)
            break;

         localList.remove(i);
      }

      return(new ArrayList<TokenSequence<? extends TokenId>>(localList));
   }

   static int[] createMatchRange(final int iOffset, final Token<? extends TokenId> token)
   {
      final int iLength = getHighlightedLength(token.text());
      return(new int[] {iOffset, iOffset + iLength});
   }

   static int getHighlightedLength(final CharSequence text)
   {
      if (text == null || text.length() == 0)
         return(0);

      int iEnd = text.length();
      while (iEnd > 0 && Character.isWhitespace(text.charAt(iEnd - 1)))
         --iEnd;

      return(Math.max(1, iEnd));
   }
}
