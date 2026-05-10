/*
 * $Id: VTLDataObject.java 13 2010-01-01 05:01:13Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity;

import com.erudika.netbeans.velocity.completion.MacroLibraryScanner;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.text.DataEditorSupport;
import org.openide.util.Lookup;

/**
 * Provides support for handling of data objects with multiple files.
 *
 * <p>On construction, pre-registers any library macro names from the
 * configured macro library so that the lexer can recognize them as
 * {@code MACROCALL_DIRECTIVE} tokens from the very first tokenization
 * pass. Without this, library macros would only be registered during
 * parsing, which happens after lexing, causing macros to appear as
 * plain {@code WORD} tokens until a re-lex is triggered.</p>
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jäger</a>
 */
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_VTL_loader_name", extension = {"vm", "vsl"}, mimeType = "text/x-velocity", position = 1309)
@DataObject.Registration(mimeType = "text/x-velocity", iconBase = "com/erudika/netbeans/velocity/VelocityFiles16.png", displayName = "#LBL_VTL_loader_name")
public class VTLDataObject extends MultiDataObject {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates new {@code VTLDataObject}.
	 *
	 * @param fo the primary file object.
	 * @param loader loader of this data object.
	 *
	 * @throws DataObjectExistsException if there is already a data object for this primary file
	 * @throws IOException in case of an IO error.
	 */
	public VTLDataObject(final FileObject fo, final MultiFileLoader loader) throws DataObjectExistsException, IOException {
		super(fo, loader);

		final CookieSet cookies = getCookieSet();
		cookies.add((Node.Cookie) DataEditorSupport.create(this, getPrimaryEntry(), cookies));

		preloadLibraryMacros(fo);
	}

	/**
	 * Pre-registers library macro names from the configured macro library
	 * for this file's project context. Called during construction so that
	 * macros are available to the lexer before the first tokenization pass.
	 */
	private static void preloadLibraryMacros(FileObject fo) {
		if (fo == null) return;

		try {
			for (MacroLibraryScanner.MacroInfo macro : MacroLibraryScanner.getMacros(fo)) {
				VelocityParser.addLibraryMacroName(macro.name());
			}
		} catch (Throwable ex) {
			// Don't let macro scanning failures break data object creation
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Node createNodeDelegate() {
		return (new DataNode(this, Children.LEAF, getLookup()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Lookup getLookup() {
		return (getCookieSet().getLookup());
	}
}