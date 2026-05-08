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
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.JavaSource;
import org.openide.filesystems.FileObject;

public final class ContextPutAnalyzer {

	private static final Set<String> VELOCITY_CONTEXT_TYPES = Set.of(
			"org.apache.velocity.VelocityContext",
			"org.apache.velocity.context.Context",
			"org.apache.velocity.context.InternalContextAdapter",
			"org.apache.velocity.context.AbstractContext",
			"org.apache.velocity.context.AbstractContextAdapter"
	);

	private ContextPutAnalyzer() {
	}

	public static List<ContextVarEntry> analyze(FileObject javaFile) {
		if (javaFile == null || !javaFile.hasExt("java")) {
			return List.of();
		}

		JavaSource javaSource = JavaSource.forFileObject(javaFile);
		if (javaSource == null) {
			return List.of();
		}

		List<ContextVarEntry>[] result = new List[]{List.of()};
		try {
			javaSource.runUserActionTask(new Task<CompilationController>() {
				@Override
				public void run(CompilationController controller) throws Exception {
					controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
					if (controller.getFileObject() == null) {
						return;
					}
					ContextPutScanner scanner = new ContextPutScanner(controller);
					scanner.scan(controller.getCompilationUnit(), null);
					result[0] = scanner.entries;
				}
			}, true);
		} catch (Throwable ex) {
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
			if ("put".equals(methodName) && node.getArguments().size() >= 2) {
				if (isVelocityContextType(receiver)) {
					String varName = extractStringLiteral(node.getArguments().get(0));
					if (varName != null) {
						String typeName = resolveTypeName(node.getArguments().get(1));
						entries.add(new ContextVarEntry(varName, typeName != null ? typeName : "java.lang.Object"));
					}
				}
			}
			return super.visitMethodInvocation(node, p);
		}

		private boolean isVelocityContextType(ExpressionTree receiver) {
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
			if (qn != null && VELOCITY_CONTEXT_TYPES.contains(qn)) {
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

		private String getQualifiedName(Element el) {
			if (el instanceof TypeElement te) {
				return te.getQualifiedName().toString();
			}
			return null;
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
