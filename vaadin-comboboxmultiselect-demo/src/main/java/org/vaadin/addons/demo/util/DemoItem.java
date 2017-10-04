package org.vaadin.addons.demo.util;

import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.server.Resource;

public class DemoItem {

	private String caption;
	private int index;
	private String description;
	private Resource icon;

	public String getCaption() {
		return this.caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public int getIndex() {
		return this.index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Resource getIcon() {
		return this.icon;
	}

	public void setIcon(Resource icon) {
		this.icon = icon;
	}

	public static Collection<DemoItem> generate(final int size) {
		Collection<DemoItem> items = new ArrayList<>();

		TestIcon testIcon = new TestIcon(90);
		StringGenerator sg = new StringGenerator();
		for (int i = 1; i < size + 1; i++) {
			DemoItem demoItem = new DemoItem();
			demoItem.setCaption(sg.nextString(true) + " " + sg.nextString(false));
			demoItem.setIndex(i);
			demoItem.setDescription(sg.nextString(true) + " " + sg.nextString(false) + " " + sg.nextString(false));
			demoItem.setIcon(testIcon.get());
			items.add(demoItem);
		}

		return items;
	}
}
