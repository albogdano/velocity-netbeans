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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class ContextVarStore {

	private static final Logger LOG = Logger.getLogger(ContextVarStore.class.getName());
	private static final String STORE_PATH = "nbproject/private/velocity-context.properties";

	private static final Map<FileObject, List<ContextPutAnalyzer.ContextVarEntry>> cache =
			Collections.synchronizedMap(new WeakHashMap<>());

	private ContextVarStore() {
	}

	public static void store(FileObject projectDir, List<ContextPutAnalyzer.ContextVarEntry> entries) {
		if (projectDir == null) {
			return;
		}
		Properties props = new Properties();
		for (ContextPutAnalyzer.ContextVarEntry entry : entries) {
			props.setProperty(entry.varName(), entry.typeName());
		}
		try {
			FileObject storeFile = FileUtil.createData(projectDir, STORE_PATH);
			try (OutputStream out = storeFile.getOutputStream()) {
				props.store(out, "Velocity Context Variables (auto-generated)");
			}
			cache.put(projectDir, new ArrayList<>(entries));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Failed to store velocity context variables", ex);
		}
	}

	public static List<ContextPutAnalyzer.ContextVarEntry> load(FileObject projectDir) {
		if (projectDir == null) {
			return List.of();
		}
		List<ContextPutAnalyzer.ContextVarEntry> cached = cache.get(projectDir);
		if (cached != null) {
			return cached;
		}
		FileObject storeFile = projectDir.getFileObject(STORE_PATH);
		if (storeFile == null || !storeFile.isValid()) {
			return List.of();
		}
		Properties props = new Properties();
		try (InputStream in = storeFile.getInputStream()) {
			props.load(in);
		} catch (IOException ex) {
			return List.of();
		}
		List<ContextPutAnalyzer.ContextVarEntry> entries = new ArrayList<>();
		for (String key : props.stringPropertyNames()) {
			entries.add(new ContextPutAnalyzer.ContextVarEntry(key, props.getProperty(key, "java.lang.Object")));
		}
		cache.put(projectDir, entries);
		return entries;
	}

	public static void clearCache(FileObject projectDir) {
		if (projectDir != null) {
			cache.remove(projectDir);
		}
	}
}
