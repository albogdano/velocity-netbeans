/*
 * $Id: VTLFoldAnalyser.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor.fold;

import com.erudika.netbeans.velocity.jcclexer.Token;
import com.erudika.netbeans.velocity.jcclexer.VelocityParserConstants;
import com.erudika.netbeans.velocity.jcclexer.node.ASTElseIfStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTElseStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTForEachStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTIfStatement;
import com.erudika.netbeans.velocity.jcclexer.node.ASTMacroStatement;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldTemplate;
import org.netbeans.api.editor.fold.FoldType;
import org.netbeans.spi.editor.fold.FoldHierarchyTransaction;
import org.netbeans.spi.editor.fold.FoldOperation;
import org.openide.text.NbDocument;

/**
 * Analyzes an abstract syntax tree to find and add code folds to VTL editor.
 *
 * <p>Fold operations are required to run on the EDT. The {@link VTLParser}
 * schedules analyser invocations on the EDT via {@code SwingUtilities.invokeLater()}.
 * If an EDT invocation finds that the fold hierarchy is busy (e.g. an active
 * transaction from the framework), the transaction is marked invalid and all
 * fold additions are skipped for that parse cycle.</p>
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jäger</a>
 */
class VTLFoldAnalyser extends VelocityAnalyser
{
   private static final Logger LOG = Logger.getLogger(VTLFoldAnalyser.class.getName());

   private final FoldOperation          m_Operation;
   private final Map<VTLFoldInfo, Fold>  m_CurrentFolds;
   private final StyledDocument          m_Document;

   private FoldHierarchyTransaction     m_Transaction;
   private boolean                      m_TransactionValid;

   VTLFoldAnalyser(final FoldOperation operation, final Map<VTLFoldInfo, Fold> currentFolds)
   {
      m_Operation    = operation;
      m_CurrentFolds = currentFolds;
      m_Document     = (StyledDocument)m_Operation.getHierarchy().getComponent().getDocument();
   }

   @Override public Object visit(final ASTMacroStatement node, final Object oData)
   {
      return(visitImpl(node, oData));
   }

   @Override public Object visit(final ASTForEachStatement node, final Object oData)
   {
      return(visitImpl(node, oData));
   }

   @Override public Object visit(final ASTIfStatement node, final Object oData)
   {
      return(visitImpl(node, oData));
   }

   @Override public Object visit(final ASTElseIfStatement node, final Object oData)
   {
      return(visitImpl(node, oData));
   }

   @Override public Object visit(final ASTElseStatement node, final Object oData)
   {
      return(visitImpl(node, oData));
   }

   private Object visitImpl(final SimpleNode node, final Object oData)
   {
      if (!m_TransactionValid)
         return(node.childrenAccept(this, oData));

      final Token  firstToken = node.getFirstToken();
      final Token  lastToken  = node.getLastToken();

      if (lastToken.kind == VelocityParserConstants.END)
      {
         final int    iStart  = NbDocument.findLineOffset(m_Document, firstToken.beginLine - 1) + firstToken.beginColumn - 1;
         final int    iEnd    = NbDocument.findLineOffset(m_Document, lastToken.endLine - 1) + lastToken.endColumn - (lastToken.image.length() - lastToken.image.trim().length());
         final String strDesc = firstToken.image + firstToken.next.image + firstToken.next.next.image + " ...";

         try
         {
            final VTLFoldInfo info = new VTLFoldInfo(m_Document, iStart, iEnd, false);
            final Fold oldFold     = m_CurrentFolds.get(info);

            if (oldFold == null)
            {
               final Fold fold = m_Operation.addToHierarchy(FoldType.CODE_BLOCK, iStart, iEnd, false, FoldTemplate.DEFAULT_BLOCK, strDesc, info, m_Transaction);
               m_CurrentFolds.put(info, fold);
            }
            else
               ((VTLFoldInfo)m_Operation.getExtraInfo(oldFold)).setState(VTLFoldInfo.State.OLD);
         }
         catch (BadLocationException ble)
         {
            LOG.log(Level.WARNING, null, ble);
         }
         catch (IllegalStateException x)
         {
            LOG.log(Level.FINE, "Fold hierarchy transaction no longer valid, skipping remaining folds", x);
            m_TransactionValid = false;
         }
      }

      return(node.childrenAccept(this, oData));
   }

   @Override public void openTransaction()
   {
      m_TransactionValid = false;
      m_Transaction = null;

      try
      {
         m_Transaction = m_Operation.openTransaction();
      }
      catch (final IllegalStateException x)
      {
         LOG.log(Level.FINE, "Could not open fold hierarchy transaction", x);
         return;
      }

      m_TransactionValid = true;

      for (final Fold fold : m_CurrentFolds.values())
         ((VTLFoldInfo)m_Operation.getExtraInfo(fold)).setState(VTLFoldInfo.State.UNTOUCHED);
   }

   @Override public void commitTransaction()
   {
      if (!m_TransactionValid)
      {
         m_Transaction = null;
         m_TransactionValid = false;
         return;
      }

      final Set<VTLFoldInfo> untouched = new HashSet<>();

      for (final Fold fold : m_CurrentFolds.values())
      {
         final VTLFoldInfo info = ((VTLFoldInfo)m_Operation.getExtraInfo(fold));
         if (info.getState() == VTLFoldInfo.State.UNTOUCHED)
         {
            try
            {
               m_Operation.removeFromHierarchy(fold, m_Transaction);
            }
            catch (final IllegalStateException x)
            {
               LOG.log(Level.FINE, "Could not remove fold from hierarchy", x);
            }
            untouched.add(info);
         }
      }

      for (final VTLFoldInfo info : untouched)
         m_CurrentFolds.remove(info);

      if (m_Transaction != null)
      {
         try
         {
            m_Transaction.commit();
         }
         catch (final IllegalStateException x)
         {
            LOG.log(Level.FINE, "Fold hierarchy transaction already committed", x);
         }
      }

      m_Transaction = null;
      m_TransactionValid = false;
   }
}