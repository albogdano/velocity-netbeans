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

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.openide.filesystems.FileObject;

public final class ContextPutAnalyzer {

	private static final Logger LOG = Logger.getLogger(ContextPutAnalyzer.class.getName());

	/** Velocity context types — direct put() calls. */
	private static final Set<String> VELOCITY_CONTEXT_TYPES = Set.of(
			"org.apache.velocity.VelocityContext",
			"org.apache.velocity.context.Context",
			"org.apache.velocity.context.InternalContextAdapter",
			"org.apache.velocity.context.AbstractContext",
			"org.apache.velocity.context.AbstractContextAdapter"
	);

	/** Spring Model types — addAttribute()/addObject() calls that feed into VelocityContext. */
	private static final Set<String> SPRING_MODEL_TYPES = Set.of(
			"org.springframework.ui.Model",
			"org.springframework.ui.ModelMap",
			"org.springframework.ui.ExtendedModelMap",
			"org.springframework.ui.ConcurrentModel",
			"org.springframework.web.servlet.ModelAndView"
	);

	/** Simple class names for fallback matching when fully-qualified type resolution fails. */
	private static final Set<String> CONTEXT_TYPE_SIMPLE_NAMES = Set.of(
			"VelocityContext", "Context", "InternalContextAdapter", "AbstractContext", "AbstractContextAdapter",
			"Model", "ModelMap", "ExtendedModelMap", "ConcurrentModel", "ModelAndView"
	);

	/** Method names that put a keyed value into a Velocity-bound context. */
	private static final Set<String> CONTEXT_PUT_METHODS = Set.of(
			"put", "addAttribute", "addObject"
	);

	/**
	 * Regex pattern for text-based fallback scanning.
	 * Matches: receiver.put("key", ...), receiver.addAttribute("key", ...), receiver.addObject("key", ...)
	 * Also matches standalone calls: put("key", ...), addAttribute("key", ...), addObject("key", ...)
	 */
	private static final Pattern CONTEXT_PUT_PATTERN = Pattern.compile(
			"(?:\\b\\w+\\s*\\.\\s*)?(?:put|addAttribute|addObject)\\s*\\(\\s*\"([^\"]+)\"\\s*,"
	);

	private ContextPutAnalyzer() {
	}

	public static List<ContextVarEntry> analyze(FileObject javaFile) {
		if (javaFile == null || !javaFile.hasExt("java")) {
			return List.of();
		}

		// 1. AST-based analysis (precise type resolution)
		List<ContextVarEntry> astResult = analyzeWithAST(javaFile);

		// 2. Text-based regex fallback (catches calls the AST misses)
		List<ContextVarEntry> textResult = analyzeWithTextScan(javaFile);

		// Merge: AST results have priority (better types), text results fill gaps
		if (astResult.isEmpty()) {
			return textResult;
		}
		if (textResult.isEmpty()) {
			return astResult;
		}

		// Combine: AST entries take priority, add text entries that aren't already present
		Set<String> astVarNames = new HashSet<>();
		for (ContextPutAnalyzer.ContextVarEntry e : astResult) {
			astVarNames.add(e.varName());
		}
		List<ContextVarEntry> merged = new ArrayList<>(astResult);
		for (ContextPutAnalyzer.ContextVarEntry e : textResult) {
			if (!astVarNames.contains(e.varName())) {
				merged.add(e);
			}
		}
		return merged;
	}

	private static List<ContextVarEntry> analyzeWithAST(FileObject javaFile) {
		JavaSource javaSource = JavaSource.forFileObject(javaFile);
		if (javaSource == null) {
			LOG.log(Level.FINE, "ContextPutAnalyzer: JavaSource unavailable for {0}", javaFile.getPath());
			return List.of();
		}

		List<ContextVarEntry>[] result = new List[]{List.of()};
		try {
			javaSource.runUserActionTask(new Task<CompilationController>() {
				@Override
				public void run(CompilationController controller) throws Exception {
					controller.toPhase(JavaSource.Phase.RESOLVED);
					if (controller.getFileObject() == null) {
						return;
					}
					ContextPutScanner scanner = new ContextPutScanner(controller);
					scanner.scan(controller.getCompilationUnit(), null);
					result[0] = scanner.entries;
				}
			}, true);
		} catch (Throwable ex) {
			LOG.log(Level.WARNING, "ContextPutAnalyzer: Error analyzing " + javaFile.getNameExt(), ex);
			return List.of();
		}
		return result[0];
	}

	/**
	 * Text-based fallback scanning using regex.
	 * Finds context.put("key", ...), model.addAttribute("key", ...), modelAndView.addObject("key", ...)
	 * without requiring the Java Source API or classpath resolution.
	 */
	private static List<ContextVarEntry> analyzeWithTextScan(FileObject javaFile) {
		String content;
		try {
			content = new String(javaFile.asBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return List.of();
		}

		List<ContextVarEntry> entries = new ArrayList<>();
		Matcher matcher = CONTEXT_PUT_PATTERN.matcher(content);
		while (matcher.find()) {
			String varName = "$" + matcher.group(1);
			entries.add(new ContextVarEntry(varName, "Object"));
		}
		return entries;
	}

	public record ContextVarEntry(String varName, String typeName) {
	}

	private static final class ContextPutScanner extends TreeScanner<Void, Void> {

		private final CompilationController controller;
		private final List<ContextVarEntry> entries = new ArrayList<>();

		ContextPutScanner(CompilationController controller) {
			this.controller = controller;
		}

		@Override
		public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
			ExpressionTree methodSelect = node.getMethodSelect();
			String methodName = null;
			ExpressionTree receiver = null;
			if (methodSelect instanceof com.sun.source.tree.MemberSelectTree mst) {
				methodName = mst.getIdentifier().toString();
				receiver = mst.getExpression();
			} else if (methodSelect instanceof IdentifierTree) {
				methodName = methodSelect.toString();
			}
			if (methodName != null && CONTEXT_PUT_METHODS.contains(methodName) && node.getArguments().size() >= 2) {
				boolean isContextType = isTemplateContextType(receiver);
				if (isContextType) {
					String varName = extractStringLiteral(node.getArguments().get(0));
					if (varName != null) {
						String typeName = resolveTypeName(node.getArguments().get(1));
						entries.add(new ContextVarEntry(varName, typeName != null ? typeName : "Object"));
						LOG.log(Level.FINE, "ContextPutAnalyzer: Found variable {0} of type {1}", new Object[]{varName, typeName});
					}
				} else {
					LOG.log(Level.FINE, "ContextPutAnalyzer: {0}() not on context type (receiver: {1})",
							new Object[]{methodName, describeReceiverType(receiver)});
				}
			}
			return super.visitMethodInvocation(node, p);
		}

		private String describeReceiverType(ExpressionTree receiver) {
			if (receiver == null) return "null (this)";
			try {
				Trees trees = controller.getTrees();
				TypeMirror type = trees.getTypeMirror(trees.getPath(controller.getCompilationUnit(), receiver));
				if (type == null) return "unresolvable (TypeMirror null)";
				Element el = controller.getTypes().asElement(type);
				if (el == null) return type.toString() + " (asElement null)";
				if (el instanceof TypeElement te) return te.getQualifiedName().toString();
				return type.toString();
			} catch (Throwable ex) {
				return "error: " + ex.getMessage();
			}
		}

		private boolean isTemplateContextType(ExpressionTree receiver) {
			if (receiver == null) {
				TypeElement enclosing = findEnclosingTypeElement();
				if (enclosing != null) {
					return isAssignableToContext(enclosing);
				}
				return false;
			}
			Trees trees = controller.getTrees();
			TypeMirror type;
			try {
				type = trees.getTypeMirror(trees.getPath(controller.getCompilationUnit(), receiver));
			} catch (Throwable ex) {
				type = null;
			}
			if (type != null) {
				Element el = controller.getTypes().asElement(type);
				if (el instanceof TypeElement te) {
					if (isAssignableToContext(te)) {
						return true;
					}
				}
				String typeStr = type.toString();
				if (matchesContextTypeSimpleName(typeStr)) {
					return true;
				}
			}
			String receiverName = receiver instanceof IdentifierTree id ? id.getName().toString() : null;
			if (receiverName != null && matchesContextVariableName(receiverName)) {
				return true;
			}
			return false;
		}

		private static boolean matchesContextTypeSimpleName(String typeName) {
			if (typeName == null || typeName.isEmpty()) {
				return false;
			}
			String simple = typeName;
			int dot = typeName.lastIndexOf('.');
			if (dot >= 0) {
				simple = typeName.substring(dot + 1);
			}
			int angle = simple.indexOf('<');
			if (angle >= 0) {
				simple = simple.substring(0, angle);
			}
			return CONTEXT_TYPE_SIMPLE_NAMES.contains(simple);
		}

		private static boolean matchesContextVariableName(String varName) {
			if (varName == null) return false;
			String lower = varName.toLowerCase();
			return lower.equals("model") || lower.equals("modelmap") || lower.equals("modelandview")
					|| lower.equals("ctx") || lower.equals("context") || lower.equals("velocitycontext")
					|| lower.equals("mav") || lower.equals("modelview");
		}

		private TypeElement findEnclosingTypeElement() {
			TreePath path = controller.getTrees().getPath(
					controller.getCompilationUnit(), controller.getCompilationUnit());
			if (path == null) {
				return null;
			}
			Element el = controller.getTrees().getElement(path);
			while (el != null) {
				if (el instanceof TypeElement te) {
					return te;
				}
				el = el.getEnclosingElement();
			}
			return null;
		}

		private boolean isAssignableToContext(TypeElement te) {
			if (te == null) {
				return false;
			}
			String qn = te.getQualifiedName().toString();
			if (qn != null && (VELOCITY_CONTEXT_TYPES.contains(qn) || SPRING_MODEL_TYPES.contains(qn))) {
				return true;
			}
			if (matchesContextTypeSimpleName(qn)) {
				return true;
			}
			TypeMirror superclass = te.getSuperclass();
			if (superclass != null && superclass.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
				TypeElement parent = (TypeElement) ((DeclaredType) superclass).asElement();
				if (isAssignableToContext(parent)) {
					return true;
				}
			}
			for (TypeMirror iface : te.getInterfaces()) {
				if (iface.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
					TypeElement ifaceEl = (TypeElement) ((DeclaredType) iface).asElement();
					if (isAssignableToContext(ifaceEl)) {
						return true;
					}
				}
			}
			return false;
		}

		private String extractStringLiteral(ExpressionTree arg) {
			if (arg instanceof LiteralTree lit && lit.getValue() instanceof String str) {
				return "$" + str;
			}
			return null;
		}

		private String resolveTypeName(ExpressionTree arg) {
			Trees trees = controller.getTrees();
			TypeMirror type;
			try {
				type = trees.getTypeMirror(trees.getPath(controller.getCompilationUnit(), arg));
			} catch (Throwable ex) {
				return null;
			}
			if (type == null) {
				return null;
			}
			String name = type.toString();
			if (name != null && !name.isEmpty()) {
				return name;
			}
			Element el = controller.getTypes().asElement(type);
			if (el instanceof TypeElement te) {
				return te.getQualifiedName().toString();
			}
			return null;
		}
	}
}