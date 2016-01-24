package org.vaadin.addons.demo.model;

import java.util.List;
import java.util.Set;

public class FieldGroupItem /*implements Item*/ {

	private NamedObject comboBox;
	private List<NamedObject> comboBoxMultiselect;
	private Set<NamedObject> myComponent;
	
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
		return "FieldGroupItem: { comboBox: " + comboBox + ", myComponent: " + myComponent + ", comboBoxMultiselect: " + comboBoxMultiselect + " }";
	}

	public Set<NamedObject> getMyComponent() {
		return myComponent;
	}

	public void setMyComponent(Set<NamedObject> myComponent) {
		this.myComponent = myComponent;
	}
}
