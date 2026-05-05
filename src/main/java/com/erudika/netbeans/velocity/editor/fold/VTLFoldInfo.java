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
final class VTLFoldInfo implements Comparable
{
   /**
    * State of this fold.
    */
   enum State
   {
      /** Already added in previous parses. */
      OLD,
      /** Added in the actual parsing. */
      NEW,
      /** Not changed by the actual parsing */
      UNTOUCHED;
   }

   private Position m_iStart;
   private Position m_iEnd;
   private boolean  m_fCollapseByDefault;
   private State    m_State;

   /**
    * Creates new {@code VTLFoldInfo}.
    *
    * @param document the document this fold belongs to.
    *        Must not be {@code null}.
    * @param iStart the start offset of the fold in the document.
    * @param iEnd the end offset of the fold in the document.
    * @param fCollapseByDefault {@code true} if and only if the fold is
    *        collapsed when created,
    *
    * @throws BadLocationException if the given start or end offset does not
    *         represent a valid location in the associated document.
    */
   VTLFoldInfo (final Document document, final int iStart, final int iEnd, final boolean fCollapseByDefault) throws BadLocationException
   {
      m_iStart             = document.createPosition(iStart);
      m_iEnd               = document.createPosition(iEnd);
      m_fCollapseByDefault = fCollapseByDefault;
      m_State              = State.NEW;
   }

   /**
    * Set the current state.
    *
    * @param state the state to set.
    */
   void setState(final State state)
   {
      m_State = state;
   }

   /**
    * Retrieves the current state.
    *
    * @return the current state.
    */
   State getState()
   {
      return(m_State);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int hashCode()
   {
      return(1);
   }

   /**
    * {@inheritDoc}
    */
   @Override public boolean equals(final Object oObject)
   {
      final boolean fRet;

      if (oObject instanceof VTLFoldInfo)
         fRet = (compareTo(oObject) == 0);
      else
         fRet = false;

      return(fRet);
   }

   /**
    * {@inheritDoc}
    */
   @Override public int compareTo(final Object object)
   {
      final VTLFoldInfo localFoldInfo = (VTLFoldInfo)object;
      final int         iRet;

      if (m_iStart.getOffset() < localFoldInfo.m_iStart.getOffset())
         iRet = -1;
      else if (m_iStart.getOffset() > localFoldInfo.m_iStart.getOffset())
         iRet = 1;
      else if (m_iEnd.getOffset() < localFoldInfo.m_iEnd.getOffset())
         iRet = -1;
      else if (m_iEnd.getOffset() > localFoldInfo.m_iEnd.getOffset())
         iRet = 1;
      else
         iRet = 0;

      return(iRet);
   }
}
