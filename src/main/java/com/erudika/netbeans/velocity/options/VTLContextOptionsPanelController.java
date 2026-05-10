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
package com.erudika.netbeans.velocity.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import com.erudika.netbeans.velocity.VelocityRefresher;

//@OptionsPanelController.SubRegistration(
//		id="velocity",
//		location = "Editor/CodeCompletion",
//		displayName = "#AdvancedOption_DisplayName_VelocityContext",
//		keywords = "#AdvancedOption_Keywords_VelocityContext",
//		keywordsCategory = "Editor/CodeCompletion/Velocity"
//)
//@NbBundle.Messages({
//	"AdvancedOption_DisplayName_VelocityContext=Velocity Context",
//	"AdvancedOption_Keywords_VelocityContext=velocity,context,variables,macros,completion"
//})
public final class VTLContextOptionsPanelController extends OptionsPanelController {

	private VTLContextOptionsPanel panel;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private boolean changed;

	public static VTLContextOptionsPanelController create() {
		return new VTLContextOptionsPanelController();
	}

	@Override
	public void update() {
		getPanel().load();
		changed = false;
	}

	@Override
	public void applyChanges() {
		getPanel().store();
		changed = false;
		VelocityRefresher.refreshAllVTLEditors();
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isValid() {
		return getPanel().valid();
	}

	@Override
	public boolean isChanged() {
		return changed;
	}

	@Override
	public JComponent getComponent(Lookup masterLookup) {
		return getPanel();
	}

	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	private VTLContextOptionsPanel getPanel() {
		if (panel == null) {
			panel = new VTLContextOptionsPanel(this);
		}
		return panel;
	}

	void changed() {
		if (!changed) {
			changed = true;
			pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
		}
	}
}
