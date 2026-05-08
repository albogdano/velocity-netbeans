/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import javax.swing.text.JTextComponent;
import org.netbeans.spi.editor.completion.CompletionTask;

/**
 * Registers Velocity completion on the embedded HTML mime path as well.
 * This keeps Velocity completions available inside HTML-embedded regions
 * of a Velocity document where the caret mime path is text/html.
 */
public class EmbeddedVTLCompletionProvider extends VTLCompletionProvider {

	@Override
	public CompletionTask createTask(int queryType, JTextComponent component) {
		return super.createTask(queryType, component);
	}

	@Override
	public int getAutoQueryTypes(JTextComponent component, String typedText) {
		return super.getAutoQueryTypes(component, typedText);
	}
}
