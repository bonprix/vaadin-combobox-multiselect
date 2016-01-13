package org.vaadin.addons.demo.model;

import java.util.List;

public class FieldGroupItem /*implements Item*/ {

	private NamedObject comboBox;
	private List<NamedObject> comboBoxMultiselect;
	
	public NamedObject getComboBox() {
		return comboBox;
	}
	
	public void setComboBox(NamedObject comboBox) {
		this.comboBox = comboBox;
	}
	
	public List<NamedObject> getComboBoxMultiselect() {
		return comboBoxMultiselect;
	}
	
	public void setComboBoxMultiselect(List<NamedObject> comboBoxMultiselect) {
		this.comboBoxMultiselect = comboBoxMultiselect;
	}
	
	@Override
	public String toString() {
		return "FieldGroupItem: { comboBox: " + comboBox + ", comboBoxMultiselect: " + comboBoxMultiselect + " }";
	}
}
