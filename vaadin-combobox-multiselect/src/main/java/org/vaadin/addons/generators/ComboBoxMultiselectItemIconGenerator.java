package org.vaadin.addons.generators;

import com.vaadin.server.Resource;

public interface ComboBoxMultiselectItemIconGenerator<BEANTYPE> {

	Resource getIcon(final BEANTYPE item);
}
