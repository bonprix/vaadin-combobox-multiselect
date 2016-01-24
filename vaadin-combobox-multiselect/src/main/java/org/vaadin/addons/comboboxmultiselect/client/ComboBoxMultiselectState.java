package org.vaadin.addons.comboboxmultiselect.client;

/**
 * Basis of this is the single-select {@link ComboBoxState} from Vaadin
 * modified to multiselect with a checkbox for each row.
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 */
import com.vaadin.shared.AbstractFieldState;

public class ComboBoxMultiselectState extends AbstractFieldState {
    {
    	// Used to have same look and feel as the ComboBox from Vaadin
        primaryStyleName = "v-filterselect";
    }
}
