/*
 * $Id: VTLLexer.java 16 2010-01-07 02:34:45Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.lexer;

import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserTokenManager;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * A lexer for the VTL language.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
class VTLLexer implements Lexer<VTLTokenId>
{
   private final LexerRestartInfo<VTLTokenId> m_Info;
   private final VelocityParserTokenManager   m_VelocityParserTokenManager;

   private Integer m_CurrLexState;

   /**
    * Creates new {@code VTLLexer}.
    *
    * @param info a reference to the lexer info object.
    *        Must not be {@code null}.
    */
   VTLLexer(final LexerRestartInfo<VTLTokenId> info)
   {
      m_Info                       = info;
      m_VelocityParserTokenManager = new VelocityParserTokenManager(new VelocityCharStream(info.input()));
      m_CurrLexState               = null;

      // Register built-in directives as known library macros so the token manager
      // produces MACROCALL_DIRECTIVE (highlighted as directive) instead of WORD.
      // Using addLibraryMacroName() ensures they persist across parse() clear() calls.
      VelocityParser.addLibraryMacroName("parse");
      VelocityParser.addLibraryMacroName("define");
      VelocityParser.addLibraryMacroName("evaluate");
      VelocityParser.addLibraryMacroName("break");

      if (info.state() != null)
         m_VelocityParserTokenManager.SwitchTo((Integer)info.state());
   }

   /**
    * {@inheritDoc}
    */
   @Override public Token<VTLTokenId> nextToken()
   {
      final Token<VTLTokenId> returnToken;

      final com.erudika.netbeans.velocity.jcclexer.Token token = m_VelocityParserTokenManager.getNextToken();

      final int iCurLexState = m_VelocityParserTokenManager.getCurrLexState();
      if (iCurLexState == VelocityParserConstants.DEFAULT)
         m_CurrLexState = null;
      else
         m_CurrLexState = iCurLexState;

      if (m_Info.input().readLength() < 1)
         returnToken = null;
      else
      {
         VTLTokenId tokenId = VTLLanguageHierarchy.getToken(token.kind);
         if (tokenId == null)
         {
            // Fallback for unmapped token kinds — treat as plain text
            tokenId = VTLLanguageHierarchy.getToken(VelocityParserConstants.TEXT);
         }
         returnToken = m_Info.tokenFactory().createToken(tokenId);
      }

      return(returnToken);
   }

   /**
    * {@inheritDoc}
    */
   @Override public Object state()
   {
      return(m_CurrLexState);
   }

   /**
    * {@inheritDoc}
    */
   @Override public void release()
   {
   }
}
