package org.vaadin.addons.comboboxmultiselect.renderers;

import com.vaadin.server.Resource;

public interface ComboBoxMultiselectItemIconGenerator<BEANTYPE> {

	Resource getIcon(final BEANTYPE item);
}
