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
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
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
	private static final Set<FileObject> LISTENER_REGISTERED = ConcurrentHashMap.newKeySet();

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
		// Allow rescan if previous scan stored empty results
		if (!SCAN_TRIGGERED.add(projectDir)) {
			// Already triggered - check if we should allow retry (empty results)
			List<ContextPutAnalyzer.ContextVarEntry> existing = ContextVarStore.load(projectDir);
			if (!existing.isEmpty()) {
				return; // Already have results, don't rescan
			}
			// Empty results - clear and retry
			SCAN_TRIGGERED.remove(projectDir);
			SCAN_TRIGGERED.add(projectDir);
		}
		RP.post(new ScanTask(project));

		// Register file listener only once per project
		if (LISTENER_REGISTERED.add(projectDir)) {
			java.io.File projectFile = FileUtil.toFile(projectDir);
			if (projectFile != null) {
				FileUtil.addFileChangeListener(new ProjectFileListener(projectDir), projectFile);
			}
		}
	}

	private static final class ScanTask implements Runnable {

		private final Project project;

		ScanTask(Project project) {
			this.project = project;
		}

		@Override
		public void run() {
			try {
				List<ContextPutAnalyzer.ContextVarEntry> allEntries = new ArrayList<>();
				int[] counts = {0, 0}; // [0]=total java files, [1]=successfully analyzed

				// First, try to scan project source roots (handles Maven/Gradle projects)
				Sources sources = ProjectUtils.getSources(project);
				if (sources != null) {
					SourceGroup[] javaGroups = sources.getSourceGroups("java");
					if (javaGroups != null) {
						for (SourceGroup group : javaGroups) {
							FileObject root = group.getRootFolder();
							if (root != null && root.isValid()) {
								collectJavaFiles(root, allEntries, counts);
							}
						}
					}
				}

				// Always also scan the project directory for any .java files not in source roots
				FileObject projectDir = project.getProjectDirectory();
				if (projectDir != null) {
					collectJavaFiles(projectDir, allEntries, counts);
				}

				ContextVarStore.store(projectDir, allEntries);
				LOG.log(Level.INFO, "Velocity context scan complete: found {0} variables from {1}/{2} Java files in project {3}",
						new Object[]{allEntries.size(), counts[1], counts[0], projectDir.getName()});
			} catch (Throwable ex) {
				LOG.log(Level.WARNING, "Error scanning velocity context variables in " + project.getProjectDirectory().getName(), ex);
				SCAN_TRIGGERED.remove(project.getProjectDirectory());
			}
		}

		private void collectJavaFiles(FileObject dir, List<ContextPutAnalyzer.ContextVarEntry> entries, int[] counts) {
			if (dir == null || !dir.isFolder()) {
				return;
			}
			for (FileObject child : dir.getChildren()) {
				if (child.isFolder()) {
					String name = child.getNameExt();
					if (!name.startsWith(".") && !SKIP_DIRS.contains(name)) {
						collectJavaFiles(child, entries, counts);
					}
				} else if (child.hasExt("java")) {
					counts[0]++;
					List<ContextPutAnalyzer.ContextVarEntry> fileEntries = ContextPutAnalyzer.analyze(child);
					if (!fileEntries.isEmpty()) {
						counts[1]++;
						entries.addAll(fileEntries);
					}
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
			RP.post(new ScanTask(FileOwnerQuery.getOwner(projectDir)));
		}
	}
}