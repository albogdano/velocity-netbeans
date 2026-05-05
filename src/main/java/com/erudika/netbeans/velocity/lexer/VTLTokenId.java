/*
 * $Id: VTLTokenId.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.lexer;

import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;

/**
 * Implementation of a VTL token-type.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLTokenId implements TokenId
{
   private static final Language<VTLTokenId> LANGUAGE = new VTLLanguageHierarchy().language();

   private final String m_strName;
   private final String m_strPrimaryCategory;
   private final int    m_iId;

   /**
    * Creates new {@code VTLTokenId}.
    *
    * @param strName the name of this token type.
    * @param strPrimaryCategory the name of primary token category into which
    *        this token type belongs.
    * @param iId the integer identification of this token type as defined in
    *        {@link com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants
    *        VelocityParserConstants}.
    */
   public VTLTokenId(final String strName, final String strPrimaryCategory, final int iId)
   {
      m_strName            = strName;
      m_strPrimaryCategory = strPrimaryCategory;
      m_iId                = iId;
   }

   /**
    * Retrieves the language for this token type.
    *
    * @return a reference to the language of this token type.
    *         Never {@code null}.
    */
   public static Language<VTLTokenId> getLanguage()
   {
      return(LANGUAGE);
   }

   /**
    * {@inheritDoc}
    */
   @Override public String name()
   {
      return(m_strName);
   }

   /**
    * {@inheritDoc}
    */
   @Override public String primaryCategory()
   {
      return(m_strPrimaryCategory);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int ordinal()
   {
      return(m_iId);
   }
}
