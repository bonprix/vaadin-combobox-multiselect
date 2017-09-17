package org.vaadin.addons.comboboxmultiselect.client;

import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect.SelectedCaptionGenerator;

/**
 * Basis of this is the single-select {@link ComboBoxState} from Vaadin
 * modified to multiselect with a checkbox for each row.
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 */
import com.vaadin.v7.shared.AbstractFieldState;

public class ComboBoxMultiselectState extends AbstractFieldState {
    {
    	// Used to have same look and feel as the ComboBox from Vaadin
        primaryStyleName = "v-filterselect";
    }
  
}
