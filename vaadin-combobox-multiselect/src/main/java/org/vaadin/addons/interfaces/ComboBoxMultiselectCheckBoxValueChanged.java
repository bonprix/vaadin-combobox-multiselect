package org.vaadin.addons.interfaces;

import org.vaadin.addons.ComboBoxMultiselect;

/**
 * Interace used for sending data back to the comboboxmultiselect
 * {@link ComboBoxMultiselect} from each row
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 *
 */

public interface ComboBoxMultiselectCheckBoxValueChanged {

	public void update(Boolean value);
	
}
