package org.vaadin.addons;

import org.vaadin.addons.interfaces.ComboBoxMultiselectCheckBoxValueChanged;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

/**
 * Row layout for each object added to the container {@link ComboBoxMultiselect}
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 *
 */
public class ComboBoxMultiselectPopupRow<O> extends HorizontalLayout {

    private static final long serialVersionUID = 8382983756053298383L;
    
    private final ComboBoxMultiselectSelectableElement<O> selectable;
    
    private final CheckBox checkBox = new CheckBox();
    private final Label	label = new Label();

    public ComboBoxMultiselectPopupRow(ComboBoxMultiselectSelectableElement<O> selectable, ComboBoxMultiselectCheckBoxValueChanged valueChanged) {
    	this.selectable = selectable;
    	
    	label.setValue(this.selectable.getLabelCaption());
    	checkBox.setValue(this.selectable.isSelected());
    	
    	addComponent(checkBox);
    	addComponent(label);
    	addLayoutClickListener(new LayoutClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void layoutClick(LayoutClickEvent event) {
				checkBox.setValue(!checkBox.getValue());
			}
		});
    	checkBox.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				ComboBoxMultiselectPopupRow.this.selectable.setSelected(checkBox.getValue());
				valueChanged.update(checkBox.getValue());
			}
		});
    	
    	setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW);
    	checkBox.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_CHECKBOX);
    }
        
    

}
