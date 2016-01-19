package org.vaadin.addons;

import junit.framework.Assert;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect;

// JUnit tests here
public class ComboBoxMultiselectTest {
	
	private ComboBoxMultiselect comboBoxMultiselect;
	private List<Object> items;
	
	@Test
	public void testAddItems() {
		init();
		
		comboBoxMultiselect.addItems(items);
		assertThat(comboBoxMultiselect.getItemIds(), is(items));
		
		String anotherItem = "Another Item";
		items.add(anotherItem);
		comboBoxMultiselect.addItem(anotherItem);
		assertThat(comboBoxMultiselect.getItemIds(), is(items));
	}
	
	@Test
	public void testDeSelectElements() {
		testAddItems();
		
		Assert.assertEquals(0, comboBoxMultiselect.getValue().size());
		
		List<Object> list = new ArrayList<>();
		list.add("Addon");
		list.add("Style");
		comboBoxMultiselect.select(list);
		Assert.assertEquals(2, comboBoxMultiselect.getValue().size());
		assertThat(comboBoxMultiselect.getValue(), is(list));
		
		comboBoxMultiselect.select("Java");
		list.add("Java");
		comboBoxMultiselect.select("Bonprix");
		list.add("Bonprix");
		comboBoxMultiselect.select("Item");
		list.add("Item");
		Assert.assertEquals(5, comboBoxMultiselect.getValue().size());
		assertThat(comboBoxMultiselect.getValue(), is(list));
		
		comboBoxMultiselect.deselect("Bonprix");
		list.remove("Bonprix");
		comboBoxMultiselect.deselect("Item");
		list.remove("Item");
		Assert.assertEquals(3, comboBoxMultiselect.getValue().size());
		assertThat(comboBoxMultiselect.getValue(), is(list));
		
		list.remove("Addon");
		comboBoxMultiselect.deselect(list);
		Assert.assertEquals(1, comboBoxMultiselect.getValue().size());
		assertThat(((List<Object>) comboBoxMultiselect.getValue()).get(0), is("Addon"));
	}
	
	@Test
	public void testSelectAll() {
		testAddItems();
		
		comboBoxMultiselect.selectAll();
		Assert.assertEquals(items.size(), comboBoxMultiselect.getValue().size());
		assertThat(comboBoxMultiselect.getValue(), is(items));
	}
	
	@Test
	public void testDeselectAll() {
		testSelectAll();
		
		comboBoxMultiselect.deselectAll();
		Assert.assertEquals(0, comboBoxMultiselect.getValue().size());
		assertThat(comboBoxMultiselect.getValue(), is(new ArrayList<Object>()));
	}
	
	private void init() {
		items = new ArrayList<>();
		items.add("Java");
		items.add("Vaadin");
		items.add("Bonprix");
		items.add("Addon");
		items.add("Widget");
		items.add("Style");
		items.add("Item");
		items.add("Publication");
		items.add("Frog");
		items.add("Note");
		items.add("Patching");
		items.add("Webbase");
		
		comboBoxMultiselect = new ComboBoxMultiselect();
	}
}
