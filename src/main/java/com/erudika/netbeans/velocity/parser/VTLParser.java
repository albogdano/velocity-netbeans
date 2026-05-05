/*
 * $Id: VTLParser.java 16 2010-01-07 02:34:45Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.parser;

import com.erudika.netbeans.velocity.jcclexer.Directive;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.jcclexer.node.SimpleNode;
import com.erudika.netbeans.velocity.jcclexer.node.VelocityAnalyser;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileObject;

/**
 * Implementation of parser for VTL language.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLParser extends Parser
{
   private static final Map<FileObject, Set<VelocityAnalyser>> ANALYSERS = new HashMap<FileObject, Set<VelocityAnalyser>>();

   private final VelocityParser m_Parser;

   private Snapshot m_Snapshot;

   /**
    * Creates new {@code VTLParser}.
    */
   public VTLParser()
   {
      m_Parser = new VelocityParser();
   }

   /**
    * {@inheritDoc}
    */
   @Override public void parse(final Snapshot snapshot, final Task task, final SourceModificationEvent sme) throws ParseException
   {
      m_Snapshot = snapshot;

      try
      {
         m_Parser.addDirective("parse", new Directive(Directive.LINE));
         m_Parser.addDirective("evaluate", new Directive(Directive.LINE));
         m_Parser.addDirective("define", new Directive(Directive.BLOCK));
         final SimpleNode sn = m_Parser.parse(new StringReader(snapshot.getText().toString()), snapshot.getSource().getFileObject().getNameExt());

         if (sn != null)
         {
            final Set<VelocityAnalyser> analysers = ANALYSERS.get(sme.getModifiedSource().getFileObject());

            if (analysers != null)
            {
               for (final VelocityAnalyser analyser : analysers)
               {
                  analyser.openTransaction();
                  analyser.visit(sn, null);
                  analyser.commitTransaction();
               }
            }
         }
      }
      catch (com.erudika.netbeans.velocity.jcclexer.ParseException pe)
      {
         Logger.getLogger(VTLParser.class.getName()).log(Level.WARNING, null, pe);
      }
   }

   /**
    * Register an analyzer that {@code visit} method gets called each
    * time the {@link #parse} method is done with creating an AST.
    *
    * @param fo the file object for which to register the given analyzer.
    *           Must not be {@code null}.
    * @param analyser a reference to an analyzer that's visit method shall be
    *        called. If the passed analyzer is already registered this method
    *        returns silently. Must not be {@code null}.
    *
    * @throws ParseException
    */
   public static void registerAnalyser(final FileObject fo, final VelocityAnalyser analyser)
   {
      final Set<VelocityAnalyser> analysers = ANALYSERS.get(fo);

      if (analysers == null)
      {
         final Set<VelocityAnalyser> analyserSet = new HashSet<VelocityAnalyser>();
         analyserSet.add(analyser);
         ANALYSERS.put(fo, analyserSet);
      }
      else
         analysers.add(analyser);
   }

   /**
    * De register an registered analyzer.
    *
    * @param fo the file object for which to remove the given analyzer.
    *           Must not be {@code null}.
    * @param analyser a reference to an analyzer to be removed. If the passed
    *        analyzer id not registered this method returns silently.
    *        Must not be {@code null}.
    *
    * @throws ParseException
    */
   public static void deregisterAnalyser(final FileObject fo, final VelocityAnalyser analyser)
   {
      final Set<VelocityAnalyser> analysers = ANALYSERS.get(fo);

      if (analysers != null)
         analysers.remove(analyser);
   }

   /**
    * {@inheritDoc}
    */
   @Override public Result getResult(final Task task) throws ParseException
   {
      return(new VTLParserResult(m_Snapshot, m_Parser));
   }

   /**
    * {@inheritDoc}
    */
   @Override public void cancel()
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void addChangeListener(final ChangeListener cl)
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public void removeChangeListener(final ChangeListener cl)
   {
   }
}
