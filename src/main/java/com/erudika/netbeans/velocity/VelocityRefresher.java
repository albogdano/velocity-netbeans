/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.erudika.netbeans.velocity;

import com.erudika.netbeans.velocity.completion.MacroLibraryScanner;
import com.erudika.netbeans.velocity.jcclexer.VelocityParser;
import com.erudika.netbeans.velocity.parser.VTLParser;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.windows.TopComponent;

/**
 * Utility that clears stale caches and forces re-lex/re-parse of all open
 * VTL editor documents. Called when the macro library configuration changes
 * in the Options panel so that new macros are highlighted and clickable
 * immediately.
 */
public final class VelocityRefresher {

	private static final Logger LOG = Logger.getLogger(VelocityRefresher.class.getName());
	private static final String VTL_MIME = VTLParser.VTL_MIME_TYPE;
	private static final int RELEX_DELAY_MS = 500;
	private static final int MAX_RETRIES = 8;

	private VelocityRefresher() {
	}

	/**
	 * Clears macro-related caches, re-registers library macros from the
	 * configured library files, and forces re-lex/re-parse of all open VTL
	 * editors. Call this after the macro library path is changed in the
	 * Options panel.
	 */
	public static void refreshAllVTLEditors() {
		VelocityParser.clearLibraryMacroNames();
		MacroLibraryScanner.clearCache();

		List<StyledDocument> docs = findAllOpenVTLDocusments();
		for (StyledDocument doc : docs) {
			FileObject fo = getFileObject(doc);
			if (fo != null) {
				for (MacroLibraryScanner.MacroInfo macro : MacroLibraryScanner.getMacros(fo)) {
					VelocityParser.addLibraryMacroName(macro.name());
				}
			}
		}

		for (StyledDocument doc : docs) {
			scheduleRelex(doc);
		}
	}

	/**
	 * Schedules a re-lex for a document by performing a minimal edit
	 * (insert+remove at position 0) to invalidate the token cache. Uses
	 * retry logic with increasing delays to handle fold hierarchy conflicts.
	 *
	 * @param doc the document to re-lex, must not be null
	 */
	public static void scheduleRelex(Document doc) {
		if (!(doc instanceof StyledDocument sdoc)) return;

		SwingUtilities.invokeLater(() -> forceRelexWithRetry(sdoc, 0));
	}

	/**
	 * Checks whether the given document is a VTL (Velocity Template Language)
	 * document by examining its MIME type.
	 */
	public static boolean isVTLDocument(Document doc) {
		Object streamDesc = doc.getProperty(Document.StreamDescriptionProperty);
		if (streamDesc instanceof DataObject dob) {
			return VTL_MIME.equals(dob.getPrimaryFile().getMIMEType());
		}
		if (streamDesc instanceof FileObject fo) {
			return VTL_MIME.equals(fo.getMIMEType());
		}
		return false;
	}

	private static void forceRelexWithRetry(StyledDocument sdoc, int attempt) {
		try {
			NbDocument.runAtomicAsUser(sdoc, () -> {
				try {
					sdoc.insertString(0, " ", null);
					sdoc.remove(0, 1);
				} catch (BadLocationException ex) {
					LOG.log(Level.FINE, "Failed to re-lex VTL document", ex);
				}
			});
		} catch (BadLocationException ex) {
			LOG.log(Level.FINE, "Failed to re-lex VTL document", ex);
		} catch (IllegalStateException ex) {
			if (attempt < MAX_RETRIES) {
				int delay = RELEX_DELAY_MS * (attempt + 1);
				LOG.log(Level.FINE, "Fold hierarchy busy, retrying re-lex in {0}ms (attempt {1})", new Object[]{delay, attempt + 1});
				Timer timer = new Timer(delay, e -> forceRelexWithRetry(sdoc, attempt + 1));
				timer.setRepeats(false);
				timer.start();
			} else {
				LOG.log(Level.WARNING, "Could not re-lex VTL document after " + MAX_RETRIES + " attempts", ex);
			}
		}
	}

	private static List<StyledDocument> findAllOpenVTLDocusments() {
		List<StyledDocument> docs = new ArrayList<>();
		for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
			EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);
			if (ec == null) continue;
			StyledDocument doc = ec.getDocument();
			if (doc == null) continue;
			if (isVTLDocument(doc)) {
				docs.add(doc);
			}
		}
		return docs;
	}

	private static FileObject getFileObject(Document doc) {
		Object streamDesc = doc.getProperty(Document.StreamDescriptionProperty);
		if (streamDesc instanceof DataObject dob) {
			return dob.getPrimaryFile();
		}
		if (streamDesc instanceof FileObject fo) {
			return fo;
		}
		return null;
	}
}