package org.vaadin.addons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.vaadin.addons.interfaces.ComboBoxMultiselectCheckBoxValueChanged;
import org.vaadin.addons.renderer.ComboBoxMultiselectItemCaptionRenderer;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityEvent;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityListener;

import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Multiselect ComboBox combined from TextField and PopupButton {@link PopupButton}
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 *
 */
public class ComboBoxMultiselect<O> extends CssLayout {

    private static final long serialVersionUID = 8382983756053298383L;
    
    public static final String STYLE_COMBOBOX_MULTISELECT = "v-combobox-multiselect";
    public static final String STYLE_COMBOBOX_MULTISELECT_INPUT = "v-combobox-multiselect-input";
    public static final String STYLE_COMBOBOX_MULTISELECT_BUTTON = "v-combobox-multiselect-button";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT = "v-combobox-multiselect-popup-layout";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BUTTON = "v-combobox-multiselect-popup-layout-button";
    private static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE = "v-combobox-multiselect-popup-layout-line";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW = "v-combobox-multiselect-popup-layout-row";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_CHECKBOX = "v-combobox-multiselect-popup-layout-row-checkbox";
    
    private final TextField textField;
    
    private final PopupButton button;
    private final VerticalLayout popupLayout;
    private final Button clearSelectionButton;
    private final CssLayout lineLayout;
    
    BeanItemContainer<ComboBoxMultiselectSelectableElement<O>> selectableElements;
    private int selectedElements = 0;
	private ComboBoxMultiselectItemCaptionRenderer<O> itemCaptionRenderer = new ComboBoxMultiselectItemCaptionRenderer<O>() {
		@Override
		public String getItemCaption(O object) {
			return object.toString();
		}
	};

    public ComboBoxMultiselect(String clearSelectionButtonCaption) {
    	super();
    	setStyleName(STYLE_COMBOBOX_MULTISELECT);
    	
    	textField = new TextField();
    	textField.setStyleName(STYLE_COMBOBOX_MULTISELECT_INPUT);
    	textField.addTextChangeListener(new TextChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void textChange(TextChangeEvent event) {
				if (!event.getText().contains(";") && !event.getText().isEmpty()) {
					addContainerFilter("labelCaption", event.getText(), true, false);
					button.setPopupVisible(true);
				}
			}
		});
    	textField.addBlurListener(new BlurListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void blur(BlurEvent event) {
				if (!button.isPopupVisible()) {
					setTextFieldValue();
				}
			}
		});
    	textField.addFocusListener(new FocusListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void focus(FocusEvent event) {
				if (textField.getValue().contains(";")) {
					textField.setValue("");				
				}
			}
		});
    	
    	button = new PopupButton();
    	button.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_BUTTON);
		
    	selectableElements = new BeanItemContainer<>(ComboBoxMultiselectSelectableElement.class);
    	selectableElements.addNestedContainerProperty("selected");
    	selectableElements.addNestedContainerProperty("labelCaption");
    	
    	popupLayout = new VerticalLayout();
    	popupLayout.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT);
    	
    	button.setContent(popupLayout);
    	button.addPopupVisibilityListener(new PopupVisibilityListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void popupVisibilityChange(PopupVisibilityEvent event) {
				if (!event.isPopupVisible()) {
					sortPopupLayout();
					repaintPopupLayout();
					setTextFieldValue();
				}
			}
		});
    	button.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				textField.focus();
			}
		});
    	
    	clearSelectionButton = new Button();
    	clearSelectionButton.setWidth(100, Unit.PERCENTAGE);
    	clearSelectionButton.addStyleName(ValoTheme.BUTTON_QUIET);
    	clearSelectionButton.setCaption(clearSelectionButtonCaption);
    	clearSelectionButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				for (ComboBoxMultiselectSelectableElement<O> selectableElement : selectableElements.getItemIds()) {
					selectableElement.setSelected(false);
				}
				repaintPopupLayout();
			}
		});
    	
    	lineLayout = new CssLayout();
    	lineLayout.setWidth(100, Unit.PERCENTAGE);
    	lineLayout.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE);
    	
    	addComponent(textField);
    	addComponent(button);
    }
    
	private void setTextFieldValue() {
		StringBuffer textFieldValueBuffer = new StringBuffer(); 
		Integer cnt = 0;
		for (ComboBoxMultiselectSelectableElement<O> selectableElement : selectableElements.getItemIds()) {
			if (selectableElement.isSelected()) {
				textFieldValueBuffer.append(selectableElement.getLabelCaption());
				textFieldValueBuffer.append(";");
				cnt++;
			}
		}
		if (cnt > 0) {
			textField.setValue("(" + cnt + ") " + textFieldValueBuffer.toString());
		} else {
			textField.setValue("");
		}
		removeContainerFilters("labelCaption");
	}

	private void repaintPopupLayout() {
		Boolean wasPreviousElementSelected = true;
		selectedElements = 0;
		
		popupLayout.removeAllComponents();
		popupLayout.addComponent(clearSelectionButton);
		sortPopupLayout();
		for (ComboBoxMultiselectSelectableElement<O> selectableElement : selectableElements.getItemIds()) {
			if (selectableElement.isSelected()) {
				selectedElements++;
			} else if (!selectableElement.isSelected() && wasPreviousElementSelected) {
				popupLayout.addComponent(lineLayout);
			}
			popupLayout.addComponent(new ComboBoxMultiselectPopupRow<>(selectableElement, new ComboBoxMultiselectCheckBoxValueChanged() {
				
				@Override
				public void update(Boolean value) {
					if (value) {
						selectedElements++;
					} else {
						selectedElements--;
					}
					clearSelectionButton.setEnabled(selectedElements > 0);
				}
			}));
			wasPreviousElementSelected = selectableElement.isSelected();
		}
		
		clearSelectionButton.setEnabled(selectedElements > 0);
	}
	
	private void sortPopupLayout() {
		selectableElements.sort(new Object[] { "selected", "labelCaption" }, new boolean[] { false, true });
	}
    
    // container delegating methodes
    public void addAll(Collection<O> collection) {
    	for (O bean : collection) {
			selectableElements.addBean(new ComboBoxMultiselectSelectableElement<O>(bean, itemCaptionRenderer.getItemCaption(bean)));
		}
		repaintPopupLayout();
	}

	public boolean removeAllItems() {
		boolean result = selectableElements.removeAllItems();
		repaintPopupLayout();
		return result;
	}

	public BeanItem<ComboBoxMultiselectSelectableElement<O>> addBean(O bean) {
		BeanItem<ComboBoxMultiselectSelectableElement<O>> result = selectableElements.addBean(new ComboBoxMultiselectSelectableElement<O>(bean, itemCaptionRenderer.getItemCaption(bean)));
		repaintPopupLayout();
		return result;
	}

	private void addContainerFilter(Object propertyId, String filterString, boolean ignoreCase,
			boolean onlyMatchPrefix) {
		selectableElements.removeContainerFilters(propertyId);
		selectableElements.addContainerFilter(propertyId, filterString, ignoreCase, onlyMatchPrefix);
		repaintPopupLayout();
	}

	private void removeContainerFilters(Object propertyId) {
		selectableElements.removeContainerFilters(propertyId);
		repaintPopupLayout();
	}

	public void setItemCaptionRenderer(ComboBoxMultiselectItemCaptionRenderer<O> itemCaptionRenderer) {
		this.itemCaptionRenderer = itemCaptionRenderer;
	}
	
	public List<O> getSelected() {
		List<O> result = new ArrayList<>();
		for (ComboBoxMultiselectSelectableElement<O> selectableElement : selectableElements.getItemIds()) {
			if (selectableElement.isSelected()) {
				result.add(selectableElement.getObject());
			}
		}
		return result;
	}

}
