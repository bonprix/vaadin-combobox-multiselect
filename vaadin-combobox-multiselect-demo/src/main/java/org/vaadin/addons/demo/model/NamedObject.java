package org.vaadin.addons.demo.model;

public class NamedObject {
	private final Long id;
	private final String name;
	
	public NamedObject(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
