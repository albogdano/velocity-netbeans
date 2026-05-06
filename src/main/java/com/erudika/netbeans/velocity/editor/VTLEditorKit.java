/*
 * $Id: VTLEditorKit.java 13 2010-01-01 05:01:13Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.editor;

import com.erudika.netbeans.velocity.parser.VTLParser;
import org.netbeans.modules.editor.NbEditorKit;

/**
 * The VTL editor kit class.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class VTLEditorKit extends NbEditorKit {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates new {@code VTLEditorKit}.
	 */
	public VTLEditorKit() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContentType() {
		return (VTLParser.VTL_MIME_TYPE);
	}
}
