package org.vaadin.addons.comboboxmultiselect.model;

public class ComboBoxMultiselectItem {

	private String caption;
	private Boolean selected;
	
	public ComboBoxMultiselectItem(Object item, String caption) {
		this(item, caption, false);
	}
	
	public ComboBoxMultiselectItem(Object item, String caption, Boolean selected) {
		this.setCaption(caption);
		this.setSelected(selected);
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}
}
