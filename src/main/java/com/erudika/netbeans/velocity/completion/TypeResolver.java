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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 * Resolves Java types and enumerates their accessible members for Velocity
 * template completion. Supports property shortcuts (getName() → name) and
 * method chain resolution (getAddress() → Address → getCity() → String).
 */
public final class TypeResolver {

	private static final Logger LOG = Logger.getLogger(TypeResolver.class.getName());

	private static final Set<String> EXCLUDED_METHODS = Set.of(
			"wait", "notify", "notifyAll", "getClass", "finalize"
	);

	private static final Set<String> LOW_PRIORITY_METHODS = Set.of(
			"equals", "hashCode", "toString", "clone"
	);

	private static final Map<String, List<ResolvedMember>> typeCache = new ConcurrentHashMap<>();

	private TypeResolver() {
	}

	/**
	 * Resolves the type of a base variable ($varName) by checking:
	 * 1. Per-project scanned Java context (ContextVarStore)
	 * 2. Global configured type mappings (Options panel)
	 *
	 * @return fully-qualified type name, or null if unresolvable
	 */
	public static String resolveVariableType(FileObject contextFile, String varName) {
		if (contextFile == null || varName == null) {
			return null;
		}
		String normalized = varName.startsWith("$") ? varName : "$" + varName;

		// 1. Check per-project scanned entries
		Project project = FileOwnerQuery.getOwner(contextFile);
		if (project != null && project.getProjectDirectory() != null) {
			for (ContextPutAnalyzer.ContextVarEntry entry : ContextVarStore.load(project.getProjectDirectory())) {
				if (entry.varName().equals(normalized)) {
					String type = entry.typeName();
					if (type != null && !"java.lang.Object".equals(type)) {
						return type;
					}
				}
			}
		}

		// 2. Check global configured type mappings
		Map<String, String> globalMappings = VTLCompletionSettings.getConfiguredTypeMappings();
		String globalType = globalMappings.get(normalized);
		if (globalType != null && !globalType.isEmpty()) {
			return globalType;
		}

		return null;
	}

	/**
	 * Resolves the final type after following a method chain.
	 * E.g., starting from "com.example.User", chain ["getAddress()"] → resolves
	 * the return type of User.getAddress().
	 *
	 * @return fully-qualified type name of the final resolved type, or null
	 */
	public static String resolveChain(FileObject contextFile, String baseTypeName, List<String> methodChain) {
		if (baseTypeName == null || methodChain == null || methodChain.isEmpty()) {
			return baseTypeName;
		}

		String currentType = stripGenerics(baseTypeName);
		for (String methodCall : methodChain) {
			String methodName = extractMethodName(methodCall);
			if (methodName == null) {
				return null;
			}
			String returnType = resolveMethodReturnType(contextFile, currentType, methodName);
			if (returnType == null) {
				return null;
			}
			currentType = stripGenerics(returnType);
		}
		return currentType;
	}

	/**
	 * Returns all accessible members (methods + property shortcuts) for a given type.
	 * Results are cached per type name.
	 */
	public static List<ResolvedMember> getMembers(FileObject contextFile, String typeName) {
		if (typeName == null || contextFile == null) {
			LOG.log(Level.FINE, "getMembers: null input (contextFile={0}, typeName={1})", new Object[]{contextFile, typeName});
			return List.of();
		}
		String rawType = stripGenerics(typeName);

		// Check cache
		List<ResolvedMember> cached = typeCache.get(rawType);
		if (cached != null) {
			return cached;
		}

		// Resolve via Java Source API
		FileObject javaFile = findJavaSourceFile(contextFile);
		if (javaFile == null) {
			LOG.log(Level.INFO, "Velocity type resolution: No Java source file found in project for context: {0}. "
					+ "Method completion requires at least one .java file in the project.", contextFile.getPath());
			return List.of();
		}

		JavaSource javaSource = JavaSource.forFileObject(javaFile);
		if (javaSource == null) {
			LOG.log(Level.INFO, "Velocity type resolution: Java source infrastructure not available for: {0}. "
					+ "Try again after the IDE finishes indexing.", javaFile.getPath());
			return List.of();
		}

		Map<String, List<ResolvedMember>> result = new HashMap<>();
		try {
			javaSource.runUserActionTask((CompilationController controller) -> {
				controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
				TypeElement typeElement = controller.getElements().getTypeElement(rawType);
				if (typeElement == null) {
					LOG.log(Level.WARNING, "TypeResolver: getTypeElement returned null for: {0}", rawType);
					result.put("result", List.of());
					return;
				}
				result.put("result", collectMembers(controller, typeElement));
			}, true);
		} catch (Throwable ex) {
			LOG.log(Level.WARNING, "TypeResolver: Failed to resolve members for type: " + rawType, ex);
			return List.of();
		}

		List<ResolvedMember> members = result.getOrDefault("result", List.of());
		if (!members.isEmpty()) {
			typeCache.put(rawType, members);
		}
		return members;
	}

	/**
	 * Clears the type resolution cache. Should be called when project classpath changes.
	 */
	public static void clearCache() {
		typeCache.clear();
	}

	private static String resolveMethodReturnType(FileObject contextFile, String typeName, String methodName) {
		List<ResolvedMember> members = getMembers(contextFile, typeName);
		for (ResolvedMember member : members) {
			if (member.isMethod() && member.name().equals(methodName) && member.returnTypeFqn() != null) {
				return member.returnTypeFqn();
			}
			// Also check property → getter mapping
			if (member.isProperty() && member.name().equals(methodName) && member.returnTypeFqn() != null) {
				return member.returnTypeFqn();
			}
		}
		// Try getter patterns: property "name" → "getName" or "isName"
		String getterName = "get" + capitalize(methodName);
		String boolGetterName = "is" + capitalize(methodName);
		for (ResolvedMember member : members) {
			if (member.isMethod()) {
				if (member.name().equals(getterName) || member.name().equals(boolGetterName)) {
					return member.returnTypeFqn();
				}
			}
		}
		return null;
	}

	private static List<ResolvedMember> collectMembers(CompilationController controller, TypeElement typeElement) {
		List<ResolvedMember> members = new ArrayList<>();
		Map<String, ResolvedMember> propertyMap = new HashMap<>();

		for (Element member : controller.getElements().getAllMembers(typeElement)) {
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
				if (EXCLUDED_METHODS.contains(name)) {
					continue;
				}

				String returnTypeDisplay = typeDisplayName(method.getReturnType());
				String returnTypeFqn = typeFqn(method.getReturnType());
				List<String> paramTypes = new ArrayList<>();
				for (var param : method.getParameters()) {
					paramTypes.add(typeDisplayName(param.asType()));
				}
				String signature = name + "(" + String.join(", ", paramTypes) + ")";
				boolean lowPriority = LOW_PRIORITY_METHODS.contains(name);

				members.add(new ResolvedMember(
						name, signature, returnTypeDisplay, returnTypeFqn,
						paramTypes.size(), false, true, lowPriority, null));

				// Derive property shortcut from getter pattern
				if (paramTypes.isEmpty() && !returnTypeDisplay.equals("void")) {
					String propertyName = derivePropertyName(name, returnTypeDisplay);
					if (propertyName != null && !propertyMap.containsKey(propertyName)) {
						propertyMap.put(propertyName, new ResolvedMember(
								propertyName, propertyName, returnTypeDisplay, returnTypeFqn,
								0, true, false, false, name + "()"));
					}
				}
			} else if (member.getKind() == ElementKind.FIELD) {
				if (!mods.contains(Modifier.PRIVATE)) {
					String fieldType = typeDisplayName(member.asType());
					String fieldTypeFqn = typeFqn(member.asType());
					if (!propertyMap.containsKey(name)) {
						propertyMap.put(name, new ResolvedMember(
								name, name, fieldType, fieldTypeFqn,
								0, true, false, false, null));
					}
				}
			}
		}

		// Add properties before methods (they appear first)
		List<ResolvedMember> result = new ArrayList<>(propertyMap.values());
		result.addAll(members);
		return Collections.unmodifiableList(result);
	}

	/**
	 * Derives a Velocity property name from a Java getter method name.
	 * "getName" → "name", "isActive" → "active", "hasItems" → null (not a standard getter)
	 */
	private static String derivePropertyName(String methodName, String returnType) {
		if (methodName.startsWith("get") && methodName.length() > 3) {
			return decapitalize(methodName.substring(3));
		}
		if (methodName.startsWith("is") && methodName.length() > 2
				&& ("boolean".equals(returnType) || "Boolean".equals(returnType))) {
			return decapitalize(methodName.substring(2));
		}
		return null;
	}

	private static String decapitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		if (str.length() > 1 && Character.isUpperCase(str.charAt(1))) {
			// e.g., "URL" stays "URL", not "uRL"
			return str;
		}
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	private static String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	public static String typeDisplayName(String type) {
		if (type == null) {
			return "Object";
		}
		// Show simple name for readability
		int lastDot = type.lastIndexOf('.');
		if (lastDot >= 0) {
			// Handle generics: java.util.List<java.lang.String> → List<String>
			// Simple approach: just take after last package dot before any < or end
			int angleIdx = type.indexOf('<');
			if (angleIdx < 0) {
				return type.substring(lastDot + 1);
			}
			String basePart = type.substring(0, angleIdx + 1);
			int baseLastDot = basePart.lastIndexOf('.');
			return (baseLastDot >= 0 ? basePart.substring(baseLastDot + 1) : basePart)
					+ simplifyGenerics(type.substring(angleIdx));
		}
		return type;
	}

	private static String typeDisplayName(TypeMirror type) {
		if (type == null || type.getKind() == TypeKind.ERROR || type.getKind() == TypeKind.NONE) {
			return "Object";
		}
		return typeDisplayName(type.toString());
	}

	private static String simplifyGenerics(String generics) {
		// Simplify "<java.lang.String, java.util.List<java.lang.Integer>>" → "<String, List<Integer>>"
		StringBuilder result = new StringBuilder();
		String[] parts = generics.split("(?=[<>,])");
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.equals("<") || trimmed.equals(">") || trimmed.equals(",")) {
				result.append(trimmed);
			} else {
				int dot = trimmed.lastIndexOf('.');
				result.append(dot >= 0 ? trimmed.substring(dot + 1) : trimmed);
			}
		}
		return result.toString();
	}

	private static String typeFqn(TypeMirror type) {
		if (type == null || type.getKind() == TypeKind.ERROR || type.getKind() == TypeKind.NONE) {
			return null;
		}
		if (type.getKind() == TypeKind.DECLARED) {
			DeclaredType dt = (DeclaredType) type;
			Element el = dt.asElement();
			if (el instanceof TypeElement te) {
				return te.getQualifiedName().toString();
			}
		}
		if (type.getKind().isPrimitive()) {
			return type.toString();
		}
		return type.toString();
	}

	/**
	 * Strips generic type parameters from a type name.
	 * "java.util.List&lt;java.lang.String&gt;" → "java.util.List"
	 */
	static String stripGenerics(String typeName) {
		if (typeName == null) {
			return null;
		}
		int angleIdx = typeName.indexOf('<');
		return angleIdx > 0 ? typeName.substring(0, angleIdx).trim() : typeName.trim();
	}

	/**
	 * Extracts the method name from a method call string.
	 * "getName()" → "getName", "get" → "get"
	 */
	private static String extractMethodName(String methodCall) {
		if (methodCall == null) {
			return null;
		}
		int parenIdx = methodCall.indexOf('(');
		return parenIdx > 0 ? methodCall.substring(0, parenIdx).trim() : methodCall.trim();
	}

	private static FileObject findJavaSourceFile(FileObject contextFile) {
		Project project = FileOwnerQuery.getOwner(contextFile);
		if (project == null) {
			return null;
		}
		return findAnyJavaFile(project.getProjectDirectory());
	}

	private static FileObject findAnyJavaFile(FileObject dir) {
		if (dir == null || !dir.isFolder()) {
			return null;
		}
		for (FileObject child : dir.getChildren()) {
			if (child.isFolder()) {
				String name = child.getNameExt();
				if (!name.startsWith(".") && !"nbproject".equals(name) && !"build".equals(name)
						&& !"dist".equals(name) && !"target".equals(name) && !"node_modules".equals(name)) {
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
}
