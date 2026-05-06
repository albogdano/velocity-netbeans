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
 *
 * For issues and patches go to: https://github.com/erudika
 */
@TemplateRegistrations({
	@TemplateRegistration(
			scriptEngine = "velocity",
			displayName = "#Templates/Other/VTLTemplate.vsl",
			folder = "Other",
			content = "VTLTemplate.vsl"),
	@TemplateRegistration(
			scriptEngine = "velocity",
			displayName = "#Templates/Other/VelocityMacroTemplate.vm",
			folder = "Other",
			content = "VelocityMacroTemplate.vm")
})
@NbBundle.Messages({
	"Templates/Other/VTLTemplate.vsl=Empty Velocity Library file",
	"Templates/Other/VelocityMacroTemplate.vm=Empty Velocity Macro file"
})
package com.erudika.netbeans.velocity;

import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.api.templates.TemplateRegistrations;
import org.openide.util.NbBundle;

