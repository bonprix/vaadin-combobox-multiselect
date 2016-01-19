package org.vaadin.addons.comboboxmultiselect;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.addons.comboboxmultiselect.components.ComboBoxMultiselectPopupRow;
import org.vaadin.addons.comboboxmultiselect.interfaces.ComboBoxMultiselectCheckBoxValueChanged;
import org.vaadin.addons.comboboxmultiselect.renderers.ComboBoxMultiselectItemCaptionRenderer;
import org.vaadin.hene.popupbutton.PopupButton;

import com.vaadin.data.Container;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
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
    
    private static final String STYLE_V_FILTERSELECT = "v-filterselect";
    private static final String STYLE_V_FILTERSELECT_INPUT = "v-filterselect-input";
    private static final String STYLE_V_FILTERSELECT_BUTTON = "v-filterselect-button";
    
    public static final String STYLE_COMBOBOX_MULTISELECT = "v-combobox-multiselect";
    public static final String STYLE_COMBOBOX_MULTISELECT_INPUT = "v-combobox-multiselect-input";
    public static final String STYLE_COMBOBOX_MULTISELECT_BUTTON = "v-combobox-multiselect-button";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT = "v-combobox-multiselect-popup-layout";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BUTTON = "v-combobox-multiselect-popup-layout-button";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_LINE = "v-combobox-multiselect-popup-layout-line";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW = "v-combobox-multiselect-popup-layout-row";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_CHECKBOX = "v-combobox-multiselect-popup-layout-row-checkbox";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_LABEL = "v-combobox-multiselect-popup-layout-row-label";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_BOTTOM_INFO = "v-combobox-multiselect-popup-layout-bottom-info";
    public static final String STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT_ROW_EMPTY = "v-combobox-multiselect-popup-layout-row-empty";
    
    private Integer pageCount = 9;
    private Integer pageIndex = 0;
    
    private Container dataSource;
	private List<Object> sortedItems;
	private String filterString = "";
    
	private boolean repaintOnSelection = false;
	private boolean clearOnlyFiltered = true;
	
	private String clearSelectionButtonCaption = "Clear selection";
	private String previousButtonCaption = "Previous";
	private String nextButtonCaption = "Next";
    
    private final TextField textField;
	private String textFieldInputPrompt;
    private boolean containsSummary;
    
    private final CssLayout layout;
    private final PopupButton popupButton;
    private final VerticalLayout popupLayout;
    private final Button previousButton;
    private final Button nextButton;
    private final Button clearSelectionButton;
    private final CssLayout lineLayoutTop;
    private final CssLayout lineLayoutBottom;
    private final Label bottomInfo;    
    
    private final Set<Object> notRepaintedElements;   
    
    private ComboBoxMultiselectItemCaptionRenderer<Object> itemCaptionRenderer = new ComboBoxMultiselectItemCaptionRenderer<Object>() {

		@Override
		public String getCaption(Object item) {
			return item.toString();
		}
	};
	private ComboBoxMultiselectItemCaptionRenderer<Object> itemCaptionShortRenderer = new ComboBoxMultiselectItemCaptionRenderer<Object>() {

		@Override
		public String getCaption(Object item) {
			return item.toString();
		}
	};

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
    	
    	setWidthUndefined();
    	setCaption(caption);
    	
    	notRepaintedElements = new HashSet<>();
    	
    	layout = new CssLayout();
    	layout.setWidthUndefined();
    	layout.addStyleName(STYLE_V_FILTERSELECT);
    	layout.addStyleName(STYLE_COMBOBOX_MULTISELECT);
    	
    	textField = new TextField();
    	textField.addStyleName(STYLE_COMBOBOX_MULTISELECT_INPUT);
    	textField.addStyleName(STYLE_V_FILTERSELECT_INPUT);
    	textField.addTextChangeListener(new TextChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void textChange(TextChangeEvent event) {
				if (!containsSummary && !event.getText().isEmpty()) {
					filterString = event.getText().toLowerCase();
					repaintPopupLayout();
					popupButton.setPopupVisible(true);
				}
			}
		});
    	textField.addBlurListener(new BlurListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void blur(BlurEvent event) {
				if (!popupButton.isPopupVisible()) {
					setTextFieldValue();
					textField.setInputPrompt(textFieldInputPrompt);
					pageIndex = 0;
					repaintPopupLayout();
				}
			}
		});
    	textField.addFocusListener(new FocusListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void focus(FocusEvent event) {
				textField.setInputPrompt("");
				if (containsSummary) {
					textField.setValue("");	
					containsSummary = false;
				}
			}
		});
    	
    	popupButton = new PopupButton();
    	popupButton.addStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_BUTTON);
    	popupButton.setPrimaryStyleName(ComboBoxMultiselect.STYLE_V_FILTERSELECT_BUTTON);
		
    	setContainerDataSource(container);
    	
    	popupLayout = new VerticalLayout();
    	popupLayout.setStyleName(ComboBoxMultiselect.STYLE_COMBOBOX_MULTISELECT_POPUP_LAYOUT);
    	popupLayout.addLayoutClickListener(new LayoutClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void layoutClick(LayoutClickEvent event) {
				textField.setCursorPosition(textField.getCursorPosition());
			}
		});
    	
    	previousButton = new Button(previousButtonCaption);
    	previousButton.addStyleName(ValoTheme.BUTTON_QUIET);
		previousButton.setWidth(100, Unit.PERCENTAGE);
		previousButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				pageIndex--;
				repaintPopupLayout(isRepaintOnSelection());
				textField.setCursorPosition(textField.getCursorPosition());
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
				textField.setCursorPosition(textField.getCursorPosition());
			}
		});
    	
    	popupButton.setContent(popupLayout);
    	popupButton.addClickListener(new ClickListener() {
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
					Collection<Object> value = getValue();
					for (Object itemId : dataSource.getItemIds()) {
						value.remove(itemId);
					}
					setValue(value);
				} else {
					setValue(null);
				}
				
				textField.setCursorPosition(textField.getCursorPosition());
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
    	layout.addComponent(popupButton);
    }

	public void setInputPrompt(String inputPrompt) {
		textFieldInputPrompt = inputPrompt;
		textField.setInputPrompt(inputPrompt);
	}

	private void setTextFieldValue() {
		StringBuffer textFieldValueBuffer = new StringBuffer(); 
		Collection<Object> value = getValue();
		if (!value.isEmpty()) {
			for (Object element : value) {
					textFieldValueBuffer.append(getCaptionShort(element));
					textFieldValueBuffer.append(";");
			}
			containsSummary = true;
			textField.setValue("(" + value.size() + ") " + textFieldValueBuffer.toString());
		} else {
			textField.setValue("");
		}
		filterString = "";
	}

	protected void repaintPopupLayout() {
		repaintPopupLayout(true);
	}
	
	protected void repaintPopupLayout(Boolean sort) {
		Boolean previousSelected = true;
		
		popupLayout.removeAllComponents();
		popupLayout.addComponent(clearSelectionButton);
		
		Collection<Object> value = getValue();
		Integer i = 0;
		
		if (sort) {
			notRepaintedElements.clear();
			sortedItems = sortItemIds(filterItemIds(getItemIds()));
		}
		
		Integer filteredItemsCount = sortedItems.size();
		
		for (Object itemId : value) {
			if (dataSource.containsId(itemId) && !notRepaintedElements.contains(itemId)) {
				filteredItemsCount--;
			}
		}
		
		Integer pageIndexMax = (filteredItemsCount + pageCount - 1) / pageCount;
		pageIndex = pageIndex >= pageIndexMax ? pageIndexMax-1 : pageIndex < 0 ? 0 : pageIndex;
		
		Boolean addedPreviousButton = false;
		
		for (Object element : sortedItems) {
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
			
			popupLayout.addComponent(new ComboBoxMultiselectPopupRow(getCaption(element), getValue().contains(element), new ComboBoxMultiselectCheckBoxValueChanged() {

				@Override
				public void update(Boolean selected) {
					Collection<Object> values = getValue();
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
	
	private List<Object> filterItemIds(Collection<?> itemIds) {
		List<Object> filteredItemIds = new ArrayList<>();
 		for (Object object : itemIds) {
 			if (getCaption(object).toLowerCase().contains(filterString)) {
 				filteredItemIds.add(object);
 			}
		}
 		return filteredItemIds;
	}
	
	private List<Object> sortItemIds(Collection<?> itemIds) {
		List<Object> sortedItemIds = new ArrayList<>(itemIds);
 		Collections.sort(sortedItemIds, new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				boolean b1 = getValue().contains(o1);
				boolean b2 = getValue().contains(o2);
				if( b1 && ! b2 ) {
			      return -1;
				}
				if( ! b1 && b2 ) {
					return 1;
				}
				
				String c1 = getCaption(o1);
				String c2 = getCaption(o2);
				return c1.toLowerCase().compareTo(c2.toLowerCase());
			}
			
		});
 		return sortedItemIds;
	}

	public void setItemCaptionRenderer(ComboBoxMultiselectItemCaptionRenderer<Object> itemCaptionRenderer) {
		this.itemCaptionRenderer = itemCaptionRenderer;
	}
	
	public void setItemCaptionShortRenderer(ComboBoxMultiselectItemCaptionRenderer<Object> itemCaptionShortRenderer) {
		this.itemCaptionShortRenderer = itemCaptionShortRenderer;
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
	public Collection<Object> getValue() {
		Collection<Object> list = (Collection<Object>) super.getValue();
		if (list == null) {
			list = new ArrayList<Object>();
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Collection<Object>> getType() {
		return (Class<? extends Collection<Object>>)  new ArrayList<>().getClass();
	}
	
	public void addItem(Object item) {
		dataSource.addItem(item);
		repaintPopupLayout();
	}

	private String getCaption(Object item) {
		return itemCaptionRenderer.getCaption(item);
	}
	
	private String getCaptionShort(Object item) {
		return itemCaptionShortRenderer.getCaption(item);
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
	
	public Collection<?> getItemIds() {
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

	public void setContainerDataSource(Container container) {
		this.dataSource = container;
	}
	
	public Container getContainerDataSource() {
		return this.dataSource;
	}
	
	public boolean select(Object itemId) {
		List<Object> itemIds = new ArrayList<Object>();
		itemIds.add(itemId);
		return select(itemIds);
	}
	
	public boolean select(Collection<Object> itemIds) {
		Boolean res = false;
		Collection<Object> value = getValue();
		
		for (Object item : itemIds) {
			if (dataSource.containsId(item) && !value.contains(item)) {
				value.add(item);
				res = true;
			}
		}
		
		setValue(value);
		
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public boolean selectAll() {
		return select((Collection<Object>)getItemIds());
	}
	
	public boolean deselect(Object itemId) {
		Collection<Object> itemIds = new ArrayList<Object>();
		itemIds.add(itemId);
		return deselect(itemIds);
	}
	
	public boolean deselect(Collection<Object> itemIds) {
		Boolean res = false;
		Collection<Object> value = getValue();
		
		for (Object item : itemIds) {
			if (dataSource.containsId(item) && value.contains(item)) {
				value.remove(item);
				res = true;
			}
		}
		
		setValue(value);
		
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public boolean deselectAll() {
		return deselect((Collection<Object>)getItemIds());
	}
}
