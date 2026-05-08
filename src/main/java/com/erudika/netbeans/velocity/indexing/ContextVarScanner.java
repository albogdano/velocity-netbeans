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
package com.erudika.netbeans.velocity.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

public final class ContextVarScanner {

	private static final Logger LOG = Logger.getLogger(ContextVarScanner.class.getName());
	private static final RequestProcessor RP = new RequestProcessor("Velocity-ContextScan", 1, false);
	private static final Set<String> SKIP_DIRS = Set.of(
			"target", "build", "dist", "node_modules", "bin", "obj", ".gradle"
	);
	private static final Set<FileObject> SCAN_TRIGGERED = ConcurrentHashMap.newKeySet();

	private ContextVarScanner() {
	}

	public static void scanProject(Project project) {
		if (project == null) {
			return;
		}
		FileObject projectDir = project.getProjectDirectory();
		if (projectDir == null) {
			return;
		}
		if (!SCAN_TRIGGERED.add(projectDir)) {
			return;
		}
		RP.post(new ScanTask(projectDir));

		FileUtil.addFileChangeListener(new ProjectFileListener(projectDir), FileUtil.toFile(projectDir));
	}

	private static final class ScanTask implements Runnable {

		private final FileObject projectDir;

		ScanTask(FileObject projectDir) {
			this.projectDir = projectDir;
		}

		@Override
		public void run() {
			try {
				List<ContextPutAnalyzer.ContextVarEntry> allEntries = new ArrayList<>();
				collectJavaFiles(projectDir, allEntries);
				ContextVarStore.store(projectDir, allEntries);
				LOG.log(Level.FINE, "Scanned {0} context variables from project {1}",
						new Object[]{allEntries.size(), projectDir.getName()});
			} catch (Exception ex) {
				LOG.log(Level.WARNING, "Error scanning velocity context variables", ex);
				SCAN_TRIGGERED.remove(projectDir);
			}
		}

		private void collectJavaFiles(FileObject dir, List<ContextPutAnalyzer.ContextVarEntry> entries) {
			if (dir == null || !dir.isFolder()) {
				return;
			}
			for (FileObject child : dir.getChildren()) {
				if (child.isFolder()) {
					String name = child.getNameExt();
					if (!name.startsWith(".") && !SKIP_DIRS.contains(name)) {
						collectJavaFiles(child, entries);
					}
				} else if (child.hasExt("java")) {
					List<ContextPutAnalyzer.ContextVarEntry> fileEntries = ContextPutAnalyzer.analyze(child);
					entries.addAll(fileEntries);
				}
			}
		}
	}

	private static final class ProjectFileListener implements FileChangeListener {

		private final FileObject projectDir;

		ProjectFileListener(FileObject projectDir) {
			this.projectDir = projectDir;
		}

		@Override
		public void fileChanged(FileEvent fe) {
			reScanIfJava(fe.getFile());
		}

		@Override
		public void fileDataCreated(FileEvent fe) {
			reScanIfJava(fe.getFile());
		}

		@Override
		public void fileDeleted(FileEvent fe) {
			reScan();
		}

		@Override
		public void fileRenamed(FileRenameEvent fe) {
			reScan();
		}

		@Override
		public void fileFolderCreated(FileEvent fe) {
		}

		@Override
		public void fileAttributeChanged(FileAttributeEvent fe) {
		}

		private void reScanIfJava(FileObject fo) {
			if (fo != null && fo.hasExt("java")) {
				reScan();
			}
		}

		private void reScan() {
			ContextVarStore.clearCache(projectDir);
			SCAN_TRIGGERED.remove(projectDir);
			RP.post(new ScanTask(projectDir));
		}
	}
}
