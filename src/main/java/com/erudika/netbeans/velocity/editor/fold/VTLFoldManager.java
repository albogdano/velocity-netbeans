/*
 * $Id: VTLFoldManager.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor.fold;

import com.erudika.netbeans.velocity.parser.VTLParser;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchyEvent;
import org.netbeans.api.editor.fold.FoldHierarchyListener;
import org.netbeans.api.editor.fold.FoldStateChange;
import org.netbeans.spi.editor.fold.FoldHierarchyTransaction;
import org.netbeans.spi.editor.fold.FoldManager;
import org.netbeans.spi.editor.fold.FoldOperation;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 * Maintains folds in the hierarchy for which it is constructed.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLFoldManager implements FoldManager, FoldHierarchyListener
{
   private FoldOperation          m_Operation;
   private VTLFoldAnalyser        m_Analyser;
   private Map<VTLFoldInfo, Fold> m_CurrentFolds;
   private FileObject             m_File;

   /**
    * Creates new {@code VTLFoldManager}.
    */
   public VTLFoldManager()
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void init(final FoldOperation operation)
   {
      m_Operation = operation;
      m_Operation.getHierarchy().addFoldHierarchyListener(this);
   }

   /**
    * {@inheritDoc}
    */
   @Override public void initFolds(final FoldHierarchyTransaction transaction)
   {

      final DataObject localDataObject = (DataObject)m_Operation.getHierarchy().getComponent().getDocument().getProperty(Document.StreamDescriptionProperty);

      if (localDataObject != null)
      {
         m_CurrentFolds = new HashMap<VTLFoldInfo, Fold>();
         m_Analyser     = new VTLFoldAnalyser(m_Operation, m_CurrentFolds);
         m_File         = localDataObject.getPrimaryFile();

         VTLParser.registerAnalyser(m_File, m_Analyser);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override public void insertUpdate(final DocumentEvent evt, final FoldHierarchyTransaction transaction)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void removeUpdate(final DocumentEvent evt, final FoldHierarchyTransaction transaction)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void changedUpdate(final DocumentEvent evt, final FoldHierarchyTransaction transaction)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void removeEmptyNotify(final Fold epmtyFold)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void removeDamagedNotify(final Fold damagedFold)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void expandNotify(final Fold expandedFold)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void release()
   {
      VTLParser.deregisterAnalyser(m_File, m_Analyser);
      m_CurrentFolds = null;
      m_Analyser     = null;
      m_File         = null;
   }

   /**
    * {@inheritDoc}
    */
   @Override public void foldHierarchyChanged(final FoldHierarchyEvent evt)
   {
      final int            iChangeCount = evt.getFoldStateChangeCount();
      final StyledDocument document     = (StyledDocument)m_Operation.getHierarchy().getComponent().getDocument();

      for (int i = 0; i < iChangeCount; i++)
      {
         final FoldStateChange change = evt.getFoldStateChange(i);

         if (change.isStartOffsetChanged() || change.isEndOffsetChanged())
         {
            try
            {
               final Fold        newFold   = change.getFold();
               final int         iOldStart = change.isStartOffsetChanged() ? change.getOriginalStartOffset() : newFold.getStartOffset();
               final int         iOldEnd   = change.isEndOffsetChanged() ? change.getOriginalEndOffset() : newFold.getEndOffset();
               final VTLFoldInfo oldInfo   = new VTLFoldInfo(document, iOldStart, iOldEnd, false);
               final Fold        oldFold   = m_CurrentFolds.get(oldInfo);
               if (oldFold != null)
               {
                  m_CurrentFolds.remove(oldInfo);
                  final VTLFoldInfo newInfo = new VTLFoldInfo(document, newFold.getStartOffset(), newFold.getEndOffset(), false);
                  m_CurrentFolds.put(newInfo, newFold);
               }
            }
            catch (BadLocationException ble)
            {
               Logger.getLogger(VTLFoldManager.class.getName()).log(Level.WARNING, null, ble);
            }
         }
      }
   }
}
