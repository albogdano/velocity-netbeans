/*
 * $Id: VTLFoldManagerFactory.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor.fold;

import org.netbeans.spi.editor.fold.FoldManager;
import org.netbeans.spi.editor.fold.FoldManagerFactory;

/**
 * Creates a new {@link VTLFoldManager} object.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLFoldManagerFactory implements FoldManagerFactory
{
   /**
    * Creates new {@code VTLFoldManagerFactory}.
    */
   public VTLFoldManagerFactory()
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override public FoldManager createFoldManager()
   {
      return(new VTLFoldManager());
   }
}
