/*
 * $Id: MacroParseException.java 16 2010-01-07 02:34:45Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.jcclexer;

/**
 *  Exception to indicate problem happened while constructing #macro()
 *
 *  For internal use in parser - not to be passed to application level
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class MacroParseException extends ParseException
{
   private final String m_strTemplateName;

   /**
    * Version Id for serializable
    */
   private static final long serialVersionUID = -4985224672336070689L;

   /**
    * Creates new {@code MacroParseException}.
    *
    * @param strMsg
    * @param strTemplateName
    * @param currentToken
    */
   public MacroParseException(final String strMsg, final String strTemplateName, final Token currentToken)
   {
      super(strMsg);

      this.currentToken = currentToken;
      m_strTemplateName = strTemplateName;
   }

   /**
    * Returns the Template name where this exception occurred.
    *
    * @return The Template name where this exception occurred.
    */
   public String getTemplateName()
   {
      return(m_strTemplateName);
   }

   /**
    * Returns the line number where this exception occurred.
    *
    * @return The line number where this exception occurred.
    */
   public int getLineNumber()
   {
      final int iLineNumber;

      if ((currentToken != null) && (currentToken.next != null))
         iLineNumber = currentToken.next.beginLine;
      else if (currentToken != null)
         iLineNumber = currentToken.beginLine;
      else
         iLineNumber = -1;

      return(iLineNumber);
   }

   /**
    * Returns the column number where this exception occurred.
    *
    * @return The column number where this exception occurred.
    */
   public int getColumnNumber()
   {
      final int iColumnNumber;

      if ((currentToken != null) && (currentToken.next != null))
         iColumnNumber =  currentToken.next.beginColumn;
      else if (currentToken != null)
         iColumnNumber = currentToken.beginColumn;
      else
         iColumnNumber = -1;

      return(iColumnNumber);
   }
}
