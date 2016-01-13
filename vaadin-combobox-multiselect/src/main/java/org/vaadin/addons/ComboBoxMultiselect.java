package org.vaadin.addons;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.addons.components.ComboBoxMultiselectPopupRow;
import org.vaadin.addons.generators.ComboBoxMultiselectItemCaptionGenerator;
import org.vaadin.addons.interfaces.ComboBoxMultiselectCheckBoxValueChanged;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityEvent;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityListener;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Multiselect ComboBox combined from TextField and PopupButton {@link PopupButton}
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 *
 */
public class ComboBoxMultiselect extends CustomField<Object> {

    private static final long serialVersionUID = 8382983756053298383L;
    
    public static final String STYLE_COMBOBOX_MULTISELECT = "v-combobox-multiselect";
    public static final String STYLE_COMBOBOX_MULTISELECT_INPUT = "v-combobox-multiselect-input";
    public static final String STYLE_COMBOBOX_MULTISELECT_BUTTON = "v-combobox-multiselect-button";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT = "v-combobox-multiselect-popup-layout";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BUTTON = "v-combobox-multiselect-popup-layout-button";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE = "v-combobox-multiselect-popup-layout-line";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW = "v-combobox-multiselect-popup-layout-row";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_CHECKBOX = "v-combobox-multiselect-popup-layout-row-checkbox";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BOTTOM_INFO = "v-combobox-multiselect-popup-layout-bottom-info";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_EMPTY = "v-combobox-multiselect-popup-layout-row-empty";
    
    private Integer pageCount = 9;
    private Integer pageIndex = 0;
    
    private final Container dataSource;
    
	private boolean repaintOnSelection = false;
	private boolean clearOnlyFiltered = true;
	
	private String clearSelectionButtonCaption = "Clear selection";
	private String previousButtonCaption = "Previous";
	private String nextButtonCaption = "Next";
    
    private final TextField textField;
    private boolean containsSummary;
    
    private final CssLayout layout;
    private final PopupButton button;
    private final VerticalLayout popupLayout;
    private final Button previousButton;
    private final Button nextButton;
    private final Button clearSelectionButton;
    private final CssLayout lineLayoutTop;
    private final CssLayout lineLayoutBottom;
    private final Label bottomInfo;    
    
    private ComboBoxMultiselectItemCaptionGenerator<Object> itemCaptionGenerator = new ComboBoxMultiselectItemCaptionGenerator<Object>() {

		@Override
		public String getCaption(Object item) {
			return item.toString();
		}
	};
	private ComboBoxMultiselectItemCaptionGenerator<Object> shortItemCaptionGenerator = new ComboBoxMultiselectItemCaptionGenerator<Object>() {

		@Override
		public String getCaption(Object item) {
			return item.toString();
		}
	};

	protected final Set<Object> notRepaintedElements;
	
	@Override
	protected Component initContent() {
		return layout;
	}
	
	public ComboBoxMultiselect() {
        this(null);
    }
	
	public ComboBoxMultiselect(String caption) {
        this(caption, new IndexedContainer());
    }

    public ComboBoxMultiselect(String caption, Container container) {
    	super();
    	
    	if (!(container instanceof Container.Filterable)) {
    		throw new ClassCastException("Container of ComboBoxMultiselect needs to extend com.vaadin.data.Container.Filterable");
    	}
    	if (!(container instanceof Container.Sortable)) {
    		throw new ClassCastException("Container of ComboBoxMultiselect needs to extend com.vaadin.data.Container.Sortable");
    	}
    	
    	setWidthUndefined();
    	
    	notRepaintedElements = new HashSet<>();
    	
    	layout = new CssLayout();
    	layout.setWidthUndefined();
    	layout.setStyleName(STYLE_COMBOBOX_MULTISELECT);
    	
    	textField = new TextField();
    	textField.setStyleName(STYLE_COMBOBOX_MULTISELECT_INPUT);
    	textField.addTextChangeListener(new TextChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void textChange(TextChangeEvent event) {
				if (!containsSummary) {
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
				if (containsSummary) {
					textField.setValue("");	
					containsSummary = false;
				}
			}
		});
    	
    	button = new PopupButton();
    	button.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_BUTTON);
		
    	dataSource = container;
    	dataSource.addContainerProperty("selected", Boolean.class, false);
    	dataSource.addContainerProperty("labelCaption", String.class, "");
    	
    	popupLayout = new VerticalLayout();
    	popupLayout.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT);
    	
    	previousButton = new Button(previousButtonCaption);
    	previousButton.addStyleName(ValoTheme.BUTTON_QUIET);
		previousButton.setWidth(100, Unit.PERCENTAGE);
		previousButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				pageIndex--;
				repaintPopupLayout(isRepaintOnSelection());
			}
		});
		
		nextButton = new Button(nextButtonCaption);
		nextButton.addStyleName(ValoTheme.BUTTON_QUIET);
		nextButton.setWidth(100, Unit.PERCENTAGE);
		nextButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				pageIndex++;
				repaintPopupLayout(isRepaintOnSelection());
			}
		});
    	
    	button.setContent(popupLayout);
    	button.addPopupVisibilityListener(new PopupVisibilityListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void popupVisibilityChange(PopupVisibilityEvent event) {
				if (!event.isPopupVisible()) {
					pageIndex = 0;
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
				if (isClearOnlyFiltered()) {
					List<Object> value = getValue();
					for (Object itemId : dataSource.getItemIds()) {
						if (value.contains(itemId)) {
							value.remove(itemId);
							@SuppressWarnings("unchecked")
							Property<Boolean> selectedProperty = dataSource.getContainerProperty(itemId, "selected");
							selectedProperty.setValue(false);
						}
					}
					setValue(value);
				} else {
					setValue(null);
					Collection<Filter> filters = ((Container.Filterable) dataSource).getContainerFilters();
					((Container.Filterable) dataSource).removeAllContainerFilters();
					for (Object itemId : dataSource.getItemIds()) {
						@SuppressWarnings("unchecked")
						Property<Boolean> selectedProperty = dataSource.getContainerProperty(itemId, "selected");
						selectedProperty.setValue(false);
					}
					for (Filter filter : filters) {
						((Container.Filterable) dataSource).addContainerFilter(filter);
					}
				}
			}
		});
    	
    	lineLayoutTop = new CssLayout();
    	lineLayoutTop.setWidth(100, Unit.PERCENTAGE);
    	lineLayoutTop.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE);
    	
    	lineLayoutBottom = new CssLayout();
    	lineLayoutBottom.setWidth(100, Unit.PERCENTAGE);
    	lineLayoutBottom.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE);
    	
    	bottomInfo = new Label();
    	bottomInfo.setStyleName(STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BOTTOM_INFO);
    	
    	layout.addComponent(textField);
    	layout.addComponent(button);
    }

	private void setTextFieldValue() {
		StringBuffer textFieldValueBuffer = new StringBuffer(); 
		List<Object> value = getValue();
		if (!value.isEmpty()) {
			for (Object element : value) {
					textFieldValueBuffer.append(shortItemCaptionGenerator.getCaption(element));
					textFieldValueBuffer.append(";");
			}
			containsSummary = true;
			textField.setValue("(" + value.size() + ") " + textFieldValueBuffer.toString());
		} else {
			textField.setValue("");
		}
		((Container.Filterable) dataSource).removeAllContainerFilters();
	}

	protected void repaintPopupLayout() {
		repaintPopupLayout(true);
	}
	
	protected void repaintPopupLayout(Boolean sort) {
		Boolean previousSelected = true;
		
		popupLayout.removeAllComponents();
		popupLayout.addComponent(clearSelectionButton);
		
		List<Object> value = getValue();
		Integer i = 0;
		
		Integer filteredItemsCount = dataSource.size();

		if (sort) {
			notRepaintedElements.clear();
			sortPopupLayout();
		}
		
		for (Object itemId : value) {
			if (dataSource.containsId(itemId) && !notRepaintedElements.contains(itemId)) {
				filteredItemsCount--;
			}
		}
		
		Integer pageIndexMax = (filteredItemsCount + pageCount - 1) / pageCount;
		pageIndex = pageIndex >= pageIndexMax ? pageIndexMax-1 : pageIndex < 0 ? 0 : pageIndex;
		
		Boolean addedPreviousButton = false;
		
		for (Object element : dataSource.getItemIds()) {
			Boolean selected = value.contains(element);
			if (!addedPreviousButton && (!selected || (selected && notRepaintedElements.contains(element))) && previousSelected) {
				popupLayout.addComponent(lineLayoutTop);
				popupLayout.addComponent(previousButton);
				previousButton.setVisible(pageIndexMax > 1);
				previousButton.setEnabled(pageIndex > 0);
				addedPreviousButton = true;
			}
			
			if (addedPreviousButton) {
				if (i < pageCount * pageIndex) {
					i++;
					continue;
				}
				if (i > pageCount * pageIndex + pageCount - 1) {
					break;
				}
				
				i++;
			}
			
			@SuppressWarnings("unchecked")
			Property<Boolean> selectedProperty = dataSource.getContainerProperty(element, "selected");
			@SuppressWarnings("unchecked")
			Property<String> labelCaptionProperty = dataSource.getContainerProperty(element, "labelCaption");
			
			popupLayout.addComponent(new ComboBoxMultiselectPopupRow<Object>(element, labelCaptionProperty.getValue(), selectedProperty.getValue(), new ComboBoxMultiselectCheckBoxValueChanged() {

				@Override
				public void update(Boolean selected) {
					selectedProperty.setValue(selected);
					
					List<Object> values = getValue();
					if (selected) {
						values.add(element);
						if (!isRepaintOnSelection()) {
							notRepaintedElements.add(element);
						}
					} else {
						values.remove(element);
						if (!isRepaintOnSelection()) {
							notRepaintedElements.remove(element);
						}
					}
					
					
					
					setValue(values, !isRepaintOnSelection());
					clearSelectionButton.setEnabled(!values.isEmpty());
				}
			}));
			previousSelected = value.contains(element);
		}
		
		if (pageIndexMax > 1) {
			for (; i < pageCount * pageIndex + pageCount; i++) {
				CssLayout emptyRow = new CssLayout();
				emptyRow.setStyleName(STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_EMPTY);
				emptyRow.setWidth(100, Unit.PERCENTAGE);
				popupLayout.addComponent(emptyRow);
			}
		}
		
		popupLayout.addComponent(nextButton);
		nextButton.setVisible(pageIndexMax > 1);
		nextButton.setEnabled(pageIndex + 1 < pageIndexMax);
		
		popupLayout.addComponent(lineLayoutBottom);
		lineLayoutBottom.setVisible(pageIndexMax > 1);
		
		popupLayout.addComponent(bottomInfo);
		popupLayout.setComponentAlignment(bottomInfo, Alignment.MIDDLE_CENTER);
		bottomInfo.setValue(pageCount * pageIndex+1 + "-" + (pageCount * pageIndex + pageCount > filteredItemsCount ? filteredItemsCount : pageCount * pageIndex + pageCount) + "/" + filteredItemsCount);
		bottomInfo.setVisible(pageIndexMax > 1);
		
		clearSelectionButton.setEnabled(!getValue().isEmpty());
	}
	
	private void sortPopupLayout() {
		((Container.Sortable) dataSource).sort(new Object[] { "selected", "labelCaption" }, new boolean[] { false, true });
	}

	private void addContainerFilter(Object propertyId, String filterString, boolean ignoreCase,
			boolean onlyMatchPrefix) {
		((Container.Filterable) dataSource).removeAllContainerFilters();
		((Container.Filterable) dataSource).addContainerFilter(new SimpleStringFilter(propertyId, filterString, ignoreCase, onlyMatchPrefix));
		pageIndex = 0;
		repaintPopupLayout();
	}

	public void setItemCaptionRenderer(ComboBoxMultiselectItemCaptionGenerator<Object> itemCaptionGenerator) {
		this.itemCaptionGenerator = itemCaptionGenerator;
	}
	
	public void setShortItemCaptionRenderer(ComboBoxMultiselectItemCaptionGenerator<Object> shortItemCaptionGenerator) {
		this.shortItemCaptionGenerator = shortItemCaptionGenerator;
	}
	
	@Override
	protected void setValue(Object newFieldValue, boolean repaintIsNotNeeded, boolean ignoreReadOnly)
			throws com.vaadin.data.Property.ReadOnlyException, ConversionException, InvalidValueException {
		if (newFieldValue == null) {
			newFieldValue = new ArrayList<>();
		}
		super.setValue(newFieldValue, repaintIsNotNeeded, ignoreReadOnly);
		if (!repaintIsNotNeeded) {
			repaintPopupLayout();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValue() {
		List<Object> list = (List<Object>) super.getValue();
		if (list == null) {
			list = new ArrayList<Object>();
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends List<Object>> getType() {
		return (Class<? extends List<Object>>)  new ArrayList<>().getClass();
	}
	
	public void addItem(Object item) {
		dataSource.addItem(item);
        @SuppressWarnings("unchecked")
		Property<Boolean> selectedProperty = dataSource.getContainerProperty(item, "selected");
		selectedProperty.setValue(getValue().contains(item));
		@SuppressWarnings("unchecked")
		Property<String> labelCaptionProperty = dataSource.getContainerProperty(item, "labelCaption");
		labelCaptionProperty.setValue(itemCaptionGenerator.getCaption(item));
	}

	public void addItems(Object... items) throws UnsupportedOperationException {
        for (Object item : items) {
            addItem(item);
        }
        repaintPopupLayout();
    }
	
	public void addItems(Collection<?> list) {
		addItems(list.toArray());
	}
	
	public Collection<?> getItems() {
		return dataSource.getItemIds();
	}
	
    public boolean removeItem(Object itemId)
            throws UnsupportedOperationException {

    	setValue(getValue().remove(itemId));
    	return dataSource.removeItem(itemId);
    }
    
    public boolean removeAllItems() {
    	setValue(null);
    	return dataSource.removeAllItems();
    }

	public boolean isRepaintOnSelection() {
		return repaintOnSelection;
	}

	public void setRepaintOnSelection(boolean repaintOnSelection) {
		this.repaintOnSelection = repaintOnSelection;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
		repaintPopupLayout();
	}
	
	public Integer getPageCount() {
		return this.pageCount;
	}

	public boolean isClearOnlyFiltered() {
		return clearOnlyFiltered;
	}

	public void setClearOnlyFiltered(boolean clearOnlyFiltered) {
		this.clearOnlyFiltered = clearOnlyFiltered;
	}

	public String getClearSelectionButtonCaption() {
		return clearSelectionButtonCaption;
	}

	public void setClearSelectionButtonCaption(String clearSelectionButtonCaption) {
		this.clearSelectionButtonCaption = clearSelectionButtonCaption;
		this.clearSelectionButton.setCaption(this.clearSelectionButtonCaption);
	}

	public String getPreviousButtonCaption() {
		return previousButtonCaption;
	}

	public void setPreviousButtonCaption(String previousButtonCaption) {
		this.previousButtonCaption = previousButtonCaption;
		this.previousButton.setCaption(this.previousButtonCaption);
	}

	public String getNextButtonCaption() {
		return nextButtonCaption;
	}

	public void setNextButtonCaption(String nextButtonCaption) {
		this.nextButtonCaption = nextButtonCaption;
		this.nextButton.setCaption(this.nextButtonCaption);
	}

	public Container getContainerDataSource() {
		return dataSource;
	}
}
