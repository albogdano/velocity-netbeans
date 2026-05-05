/*
 * $Id: VTLUpToDateStatusProvider.java 16 2010-01-07 02:34:45Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.parser;

import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.text.Document;
import org.netbeans.spi.editor.errorstripe.UpToDateStatus;
import org.netbeans.spi.editor.errorstripe.UpToDateStatusProvider;

/**
 * Provides information whether the current annotations attached to the
 * documents are up-to-date.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
class VTLUpToDateStatusProvider extends UpToDateStatusProvider
{
   private static final Map<Document, VTLUpToDateStatusProvider> CACHE = new WeakHashMap<Document, VTLUpToDateStatusProvider>();

   private final Document m_Document;

   private UpToDateStatus m_Status;

   /**
    * Creates new {@code VTLUpToDateStatusProvider}.
    *
    * @param document a reference to the document thats status to report.
    *        Must not be {@code null}.
    */
   private VTLUpToDateStatusProvider(final Document document)
   {
      m_Document = document;
      m_Status   = UpToDateStatus.UP_TO_DATE_DIRTY;
      CACHE.put(document, this);
   }

   /**
    * Factory method to create new providers for a given document.
    *
    * @param document the document to create a provider for.
    *        Must not be {@code null}.
    *
    * @return a reference to a provider either retrieved from a cache or newly
    *         created. Never {@code null}.
    */
   static VTLUpToDateStatusProvider forDocument(final Document document)
   {
      final VTLUpToDateStatusProvider localUpToDateStatusProvider;
      final VTLUpToDateStatusProvider localUpToDateStatusProvider1 = CACHE.get(document);
      if (localUpToDateStatusProvider1 == null)
         localUpToDateStatusProvider = new VTLUpToDateStatusProvider(document);
      else
         localUpToDateStatusProvider = localUpToDateStatusProvider1;

      return(localUpToDateStatusProvider);
   }

   /**
    * {@inheritDoc}
    */
   @Override public UpToDateStatus getUpToDate()
   {
      return(m_Status);
   }

   /**
    * Switch current Up-to-date status to processing.
    *
    * <p>
    *   Up-to-date status saying that the list of marks is not up-to-date,
    *   but a up-to-date list of marks is currently being found.
    * </p>
    */
   void setProcessingStatus()
   {
      final UpToDateStatus oldStatus = m_Status;
      m_Status = UpToDateStatus.UP_TO_DATE_PROCESSING;
      firePropertyChange(PROP_UP_TO_DATE, oldStatus, m_Status);
   }

   /**
    * Switch current Up-to-date status to dirty.
    *
    * <p>
    *   Up-to-date status saying that the list of marks is not up-to-date,
    *   and nothing is currently done in order to get the up-to-date list.
    * </p>
    */
   void setDirtyStatus()
   {
      final UpToDateStatus oldStatus = m_Status;
      m_Status = UpToDateStatus.UP_TO_DATE_DIRTY;
      firePropertyChange(PROP_UP_TO_DATE, oldStatus, m_Status);
   }

   /**
    * Switch current Up-to-date status to okay.
    *
    * <p>
    *   Up-to-date status saying everything is up-to-date.
    * </p>
    */
   void setOkStatus()
   {
      final UpToDateStatus oldStatus = m_Status;
      m_Status = UpToDateStatus.UP_TO_DATE_OK;
      firePropertyChange(PROP_UP_TO_DATE, oldStatus, m_Status);
   }
}
