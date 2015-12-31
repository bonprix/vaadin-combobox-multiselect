package org.vaadin.addons;

/**
 * Proxy for each element added to the multiselect combobox to
 * have special access on the selected and to give possibility
 * to change caption with itemCaptionRenderer
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 *
 */
public class ComboBoxMultiselectSelectableElement<O> {
    
    private final O object;
	private Boolean selected = false;
	private String labelCaption;

    public ComboBoxMultiselectSelectableElement(O object, String labelCaption) {
    	this.object = object;
		this.labelCaption = labelCaption;
    }

	public Boolean isSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	public O getObject() {
		return object;
	}
	
	@Override
	public String toString() {
		return this.object.toString();
	}

	public String getLabelCaption() {
		return labelCaption;
	}

	public void setLabelCaption(String labelCaption) {
		this.labelCaption = labelCaption;
	}

}
