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
package com.erudika.netbeans.velocity.completion;

import com.erudika.netbeans.velocity.indexing.ContextPutAnalyzer;
import com.erudika.netbeans.velocity.indexing.ContextVarStore;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

public final class MethodCompletionHelper {

	private static final Set<ElementKind> METHOD_KINDS = EnumSet.of(
			ElementKind.METHOD, ElementKind.FIELD
	);

	private MethodCompletionHelper() {
	}

	public static List<MemberInfo> getMembers(FileObject contextFile, String varName) {
		if (contextFile == null || varName == null) {
			return List.of();
		}
		Project project = FileOwnerQuery.getOwner(contextFile);
		if (project == null) {
			return List.of();
		}
		FileObject projectDir = project.getProjectDirectory();
		if (projectDir == null) {
			return List.of();
		}

		String normalized = varName.startsWith("$") ? varName : "$" + varName;
		final String typeName;
		String resolvedType = null;
		for (ContextPutAnalyzer.ContextVarEntry entry : ContextVarStore.load(projectDir)) {
			if (entry.varName().equals(normalized)) {
				resolvedType = entry.typeName();
				break;
			}
		}
		typeName = resolvedType;
		if (typeName == null || "java.lang.Object".equals(typeName)) {
			return List.of();
		}

		// Find a Java file in the project to use as a resolution context
		FileObject javaFile = findAnyJavaFile(projectDir);
		if (javaFile == null) {
			return List.of();
		}

		JavaSource javaSource = JavaSource.forFileObject(javaFile);
		if (javaSource == null) {
			return List.of();
		}

		Map<String, List<MemberInfo>> result = new HashMap<>();
		try {
			javaSource.runUserActionTask((CompilationController controller) -> {
				controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
				TypeElement type = controller.getElements().getTypeElement(typeName);
				if (type == null) {
					return;
				}
				List<MemberInfo> members = new ArrayList<>();
				for (Element member : controller.getElements().getAllMembers(type)) {
					if (!METHOD_KINDS.contains(member.getKind())) {
						continue;
					}
					Set<Modifier> mods = member.getModifiers();
					if (mods.contains(Modifier.PRIVATE) || mods.contains(Modifier.STATIC)) {
						continue;
					}
					String name = member.getSimpleName().toString();
					if (name.startsWith("<") || name.startsWith("_")) {
						continue;
					}
					if (member.getKind() == ElementKind.METHOD) {
						ExecutableElement method = (ExecutableElement) member;
						List<String> paramTypes = new ArrayList<>();
						for (var param : method.getParameters()) {
							paramTypes.add(typeDisplayName(param.asType()));
						}
						String returnType = typeDisplayName(method.getReturnType());
						String signature = name + "(" + String.join(", ", paramTypes) + ")";
						members.add(new MemberInfo(name, signature, returnType, true));
					} else if (member.getKind() == ElementKind.FIELD) {
						String fieldType = typeDisplayName(member.asType());
						members.add(new MemberInfo(name, name, fieldType, false));
					}
				}
				result.put("result", members);
			}, true);
		} catch (Exception ex) {
			return List.of();
		}
		return result.get("result");
	}

	private static String typeDisplayName(TypeMirror type) {
		if (type == null || type.getKind() == TypeKind.ERROR || type.getKind() == TypeKind.NONE) {
			return "Object";
		}
		String raw = type.toString();
		int lastDot = raw.lastIndexOf('.');
		return lastDot >= 0 ? raw.substring(lastDot + 1) : raw;
	}

	private static FileObject findAnyJavaFile(FileObject dir) {
		if (dir == null || !dir.isFolder()) {
			return null;
		}
		for (FileObject child : dir.getChildren()) {
			if (child.isFolder()) {
				String name = child.getNameExt();
				if (!name.startsWith(".") && !"nbproject".equals(name) && !"build".equals(name)
						&& !"dist".equals(name) && !"target".equals(name)) {
					FileObject found = findAnyJavaFile(child);
					if (found != null) {
						return found;
					}
				}
			} else if (child.hasExt("java")) {
				return child;
			}
		}
		return null;
	}

	public record MemberInfo(String name, String displaySignature, String returnType, boolean isMethod) {
	}
}
