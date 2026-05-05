/*
 * $Id: VTLUpToDateStatusProviderFactory.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.parser;

import javax.swing.text.Document;
import org.netbeans.spi.editor.errorstripe.UpToDateStatusProvider;
import org.netbeans.spi.editor.errorstripe.UpToDateStatusProviderFactory;

/**
 * Factory to create a new {@link VTLUpToDateStatusProvider} instance.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLUpToDateStatusProviderFactory implements UpToDateStatusProviderFactory
{
   /**
    * Creates new {@code VTLUpToDateStatusProviderFactory}.
    */
   public VTLUpToDateStatusProviderFactory()
   {
   }

   /**
    * Factory method to create new providers for the given document.
    *
    * @param document the document to create a provider for.
    *        Must not be {@code null}.
    *
    * @return a reference to a provider either retrieved from a cache or newly
    *         created. Never {@code null}.
    */
   @Override public UpToDateStatusProvider createUpToDateStatusProvider(final Document document)
   {
      return(VTLUpToDateStatusProvider.forDocument(document));
   }
}
