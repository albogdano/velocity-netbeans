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
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	/** Spring Model types — addAttribute() calls that feed into VelocityContext via Spring's VelocityView. */
	private static final Set<String> SPRING_MODEL_TYPES = Set.of(
			"org.springframework.ui.Model",
			"org.springframework.ui.ModelMap",
			"org.springframework.ui.ExtendedModelMap",
			"org.springframework.ui.ConcurrentModel",
			"org.springframework.web.servlet.ModelAndView"
	);

	/** Method names that put a keyed value into a Velocity-bound context. */
	private static final Set<String> CONTEXT_PUT_METHODS = Set.of(
			"put", "addAttribute"
	);

	private ContextPutAnalyzer() {
	}

	public static List<ContextVarEntry> analyze(FileObject javaFile) {
		if (javaFile == null || !javaFile.hasExt("java")) {
			return List.of();
		}

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
			}
			if (methodName != null && CONTEXT_PUT_METHODS.contains(methodName) && node.getArguments().size() >= 2) {
				boolean isContextType = isTemplateContextType(receiver);
				if (!isContextType && LOG.isLoggable(Level.FINE)) {
					String receiverType = describeReceiverType(receiver);
					LOG.log(Level.FINE, "ContextPutAnalyzer: {0}() on type ''{1}'' - not a context type",
							new Object[]{methodName, receiverType});
				}
				if (isContextType) {
					String varName = extractStringLiteral(node.getArguments().get(0));
					if (varName != null) {
						String typeName = resolveTypeName(node.getArguments().get(1));
						entries.add(new ContextVarEntry(varName, typeName != null ? typeName : "Object"));
						LOG.log(Level.FINE, "ContextPutAnalyzer: Found variable {0} of type {1}", new Object[]{varName, typeName});
					}
				}
			}
			return super.visitMethodInvocation(node, p);
		}

		private String describeReceiverType(ExpressionTree receiver) {
			if (receiver == null) return "null";
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
				return false;
			}
			Trees trees = controller.getTrees();
			TypeMirror type;
			try {
				type = trees.getTypeMirror(trees.getPath(controller.getCompilationUnit(), receiver));
			} catch (Throwable ex) {
				return false;
			}
			if (type == null) {
				return false;
			}
			Element el = controller.getTypes().asElement(type);
			if (el == null) {
				return false;
			}
			if (el instanceof TypeElement te) {
				return isAssignableToContext(te);
			}
			return false;
		}

		private boolean isAssignableToContext(TypeElement te) {
			if (te == null) {
				return false;
			}
			String qn = te.getQualifiedName().toString();
			if (qn != null && (VELOCITY_CONTEXT_TYPES.contains(qn) || SPRING_MODEL_TYPES.contains(qn))) {
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
