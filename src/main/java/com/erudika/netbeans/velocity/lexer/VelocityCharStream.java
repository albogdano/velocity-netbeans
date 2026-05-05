/*
 * $Id: VelocityCharStream.java 13 2010-01-01 05:01:13Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.lexer;

import com.erudika.netbeans.velocity.jcclexer.CharStream;
import java.io.IOException;
import org.netbeans.spi.lexer.LexerInput;

/**
 * Reads input chars from LexerInput, in place of standard InputStream.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VelocityCharStream implements CharStream
{
   private final LexerInput m_Input;

   /**
    * Creates new {@code VelocityCharStream}.
    *
    * @param input the input to read from. Must not be {@code null}.
    */
   public VelocityCharStream(final LexerInput input)
   {
      m_Input = input;
   }


   /**
    * {@inheritDoc}
    */
   @Override public char BeginToken() throws IOException
   {
      final int i = m_Input.read();

      if (i == LexerInput.EOF)
         throw new IOException("EOF encountered");

      return((char)i);
   }

   /**
    * {@inheritDoc}
    */
   @Override public String GetImage()
   {
      return(m_Input.readText().toString());
   }

   /**
    * {@inheritDoc}
    */
   @Override public char[] GetSuffix(final int iLen)
   {
      if (iLen > m_Input.readLength())
         throw new IllegalArgumentException();

      return(m_Input.readText(m_Input.readLength() - iLen, m_Input.readLength()).toString().toCharArray());
   }

   /**
    * {@inheritDoc}
    */
   @Override public void backup(final int iAmmont)
   {
      m_Input.backup(iAmmont);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getBeginColumn()
   {
      return(0);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getBeginLine()
   {
      return(0);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getEndColumn()
   {
      return(0);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getEndLine()
   {
      return(0);
   }

   /**
    * {@inheritDoc}
    */
   @Override public char readChar() throws IOException
   {
      final int i = m_Input.read();

      if (i == LexerInput.EOF)
         throw new IOException("EOF encountered");

      return((char)i);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getColumn()
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   /**
    * {@inheritDoc}
    */
   @Override public int getLine()
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   /**
    * {@inheritDoc}
    */
   @Override public void Done()
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }
}
