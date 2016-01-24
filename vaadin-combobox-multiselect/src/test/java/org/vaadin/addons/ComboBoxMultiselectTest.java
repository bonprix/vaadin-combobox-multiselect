package org.vaadin.addons;

import junit.framework.Assert;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUnselectElements() {
		testAddItems();
		
		Assert.assertEquals(0, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		List<Object> list = new ArrayList<>();
		list.add("Addon");
		list.add("Style");
		comboBoxMultiselect.select(list);
		Assert.assertEquals(2, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertThat(comboBoxMultiselect.getValue(), is(new HashSet<Object>(list)));
		
		comboBoxMultiselect.select("Java");
		list.add("Java");
		comboBoxMultiselect.select("Bonprix");
		list.add("Bonprix");
		comboBoxMultiselect.select("Item");
		list.add("Item");
		Assert.assertEquals(5, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertThat(comboBoxMultiselect.getValue(), is(new HashSet<Object>(list)));
		
		comboBoxMultiselect.unselect("Bonprix");
		list.remove("Bonprix");
		comboBoxMultiselect.unselect("Item");
		list.remove("Item");
		Assert.assertEquals(3, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertThat(comboBoxMultiselect.getValue(), is(new HashSet<Object>(list)));
		
		list.remove("Addon");
		comboBoxMultiselect.unselect(list);
		Assert.assertEquals(1, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertTrue(((Set<Object>) comboBoxMultiselect.getValue()).contains("Addon"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSelectAll() {
		testAddItems();
		
		comboBoxMultiselect.selectAll();
		Assert.assertEquals(items.size(), ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertThat(comboBoxMultiselect.getValue(), is(new HashSet<Object>(items)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUnselectAll() {
		testSelectAll();
		
		comboBoxMultiselect.unselectAll();
		Assert.assertEquals(0, ((Set<Object>) comboBoxMultiselect.getValue()).size());
		assertThat(comboBoxMultiselect.getValue(), is(new HashSet<Object>()));
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
