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
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;

/**
 * Utility that clears stale caches, re-registers library macros, and forces
 * re-lex/re-parse of all open VTL editor documents. Called when the macro
 * library configuration changes in the Options panel.
 *
 * <p>Re-lex is triggered by closing and reopening each editor tab, which is
 * the only reliable way to force full re-tokenization without conflicting
 * with the fold hierarchy's transaction model. Direct document modification
 * (insert+remove) was previously used but caused
 * {@code IllegalStateException: Active transaction already exists} in the
 * fold hierarchy because {@code NbDocument.runAtomicAsUser()} acquires an
 * atomic lock that conflicts with ongoing fold transactions.</p>
 */
public final class VelocityRefresher {

	private static final Logger LOG = Logger.getLogger(VelocityRefresher.class.getName());
	private static final String VTL_MIME = VTLParser.VTL_MIME_TYPE;

	private VelocityRefresher() {
	}

	/**
	 * Clears macro-related caches, re-registers library macros from the
	 * configured library files, and forces re-lex/re-parse of all open VTL
	 * editors by closing and reopening each editor tab.
	 *
	 * <p>Call this after the macro library path is changed in the Options
	 * panel.</p>
	 */
	public static void refreshAllVTLEditors() {
		VelocityParser.clearLibraryMacroNames();
		MacroLibraryScanner.clearCache();

		List<DataObject> vtlFiles = new ArrayList<>();
		List<EditorCookie> cookies = new ArrayList<>();

		for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
			EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);
			if (ec == null) continue;
			StyledDocument doc = ec.getDocument();
			if (doc == null) continue;
			if (!isVTLDocument(doc)) continue;

			Object streamDesc = doc.getProperty(Document.StreamDescriptionProperty);
			if (!(streamDesc instanceof DataObject dob)) continue;

			for (MacroLibraryScanner.MacroInfo macro : MacroLibraryScanner.getMacros(dob.getPrimaryFile())) {
				VelocityParser.addLibraryMacroName(macro.name());
			}

			vtlFiles.add(dob);
			cookies.add(ec);
		}

		// Close and reopen each editor on the EDT with a delay between
		// operations to let the fold hierarchy settle. This forces a full
		// re-lex with the freshly registered macro names.
		SwingUtilities.invokeLater(() -> reopenEditors(cookies, 0));
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

	/**
	 * Closes and reopens each editor cookie sequentially with delays to
	 * avoid conflicting with the fold hierarchy. Each close+open cycle
	 * forces a complete re-lex of the document with the newly registered
	 * library macros.
	 */
	private static void reopenEditors(List<EditorCookie> cookies, int index) {
		if (index >= cookies.size()) return;

		EditorCookie ec = cookies.get(index);
		ec.close();
		// Delay before reopening to let fold hierarchy clean up
		javax.swing.Timer timer = new javax.swing.Timer(300, e -> {
			ec.open();
			reopenEditors(cookies, index + 1);
		});
		timer.setRepeats(false);
		timer.start();
	}
}