/*
 * $Id: VTLFoldInfo.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor.fold;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

/**
 * Hold diverse information about a fold added to the editor.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
final class VTLFoldInfo implements Comparable<VTLFoldInfo>
{
   enum State
   {
      OLD,
      NEW,
      UNTOUCHED;
   }

   private final Position m_iStart;
   private final Position m_iEnd;
   private final boolean  m_fCollapseByDefault;
   private State           m_State;

   VTLFoldInfo(final Document document, final int iStart, final int iEnd, final boolean fCollapseByDefault) throws BadLocationException
   {
      m_iStart             = document.createPosition(iStart);
      m_iEnd               = document.createPosition(iEnd);
      m_fCollapseByDefault = fCollapseByDefault;
      m_State              = State.NEW;
   }

   void setState(final State state)
   {
      m_State = state;
   }

   State getState()
   {
      return(m_State);
   }

   @Override public int hashCode()
   {
      return(1);
   }

   @Override public boolean equals(final Object oObject)
   {
      if (oObject instanceof VTLFoldInfo)
         return compareTo((VTLFoldInfo)oObject) == 0;
      return false;
   }

   @Override public int compareTo(final VTLFoldInfo other)
   {
      if (m_iStart.getOffset() < other.m_iStart.getOffset())
         return -1;
      if (m_iStart.getOffset() > other.m_iStart.getOffset())
         return 1;
      if (m_iEnd.getOffset() < other.m_iEnd.getOffset())
         return -1;
      if (m_iEnd.getOffset() > other.m_iEnd.getOffset())
         return 1;
      return 0;
   }
}