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

/**
 * Represents a resolved member (method or property) of a Java type,
 * ready for use in VTL completion proposals.
 *
 * @param name           The member name ("name", "getName", "size")
 * @param displaySignature  Display text ("getName()", "size()", "name")
 * @param returnTypeDisplay Short return type display ("String", "int", "List&lt;User&gt;")
 * @param returnTypeFqn  Fully-qualified return type for chain resolution ("java.lang.String")
 * @param paramCount     Number of parameters (0 for properties/fields)
 * @param isProperty     True if this is a Velocity property shortcut (derived from getter)
 * @param isMethod       True if this is a raw Java method
 * @param isLowPriority  True for inherited Object methods (equals, hashCode, toString)
 * @param derivedFrom    For properties: the method it derives from ("getName()"), null otherwise
 */
public record ResolvedMember(
		String name,
		String displaySignature,
		String returnTypeDisplay,
		String returnTypeFqn,
		int paramCount,
		boolean isProperty,
		boolean isMethod,
		boolean isLowPriority,
		String derivedFrom) {
}
