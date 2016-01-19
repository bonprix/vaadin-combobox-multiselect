package org.vaadin.addons.demo;

import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect;
import org.vaadin.addons.comboboxmultiselect.renderers.ComboBoxMultiselectItemCaptionRenderer;
import org.vaadin.addons.demo.model.FieldGroupItem;
import org.vaadin.addons.demo.model.NamedObject;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

@Theme("demo")
@Title("MyComponent Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI {

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "org.vaadin.addons.demo.DemoWidgetSet")
	public static class Servlet extends VaadinServlet {
	}

	@Override
	protected void init(VaadinRequest request) {

		List<NamedObject> list = new ArrayList<NamedObject>();
		list.add(new NamedObject(1L, "Java"));
		list.add(new NamedObject(2L, "Vaadin"));
		list.add(new NamedObject(3L, "Bonprix"));
		list.add(new NamedObject(4L, "Addon"));
		list.add(new NamedObject(5L, "Widget"));
		list.add(new NamedObject(6L, "Style"));
		list.add(new NamedObject(7L, "Item"));
		list.add(new NamedObject(8L, "Publication"));
		list.add(new NamedObject(9L, "Frog"));
		list.add(new NamedObject(10L, "Note"));
		list.add(new NamedObject(11L, "Patching"));
		list.add(new NamedObject(12L, "Webbase"));

		// Initialize our new UI component
		final ComboBoxMultiselect comboBoxMultiselect = new ComboBoxMultiselect();
		comboBoxMultiselect.setInputPrompt("Type here");
		comboBoxMultiselect.setCaption("ComboBoxMultiselect");
		comboBoxMultiselect.addItems(list);

		// ComboBox
		final ComboBox comboBox = new ComboBox();
		comboBox.setInputPrompt("Type here");
		comboBox.setCaption("ComboBox");
		comboBox.addItems(list);

		final FieldGroupItem item = new FieldGroupItem();
		BeanItem<FieldGroupItem> beanItem = new BeanItem<FieldGroupItem>(item);
		final FieldGroup binder = new FieldGroup(beanItem);
		binder.bind(comboBox, "comboBox");
		binder.bind(comboBoxMultiselect, "comboBoxMultiselect");
		// binder.setBuffered(false);

		Button button = new Button("Show FieldGroupValues");
		button.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
				} catch (CommitException e) {
					e.printStackTrace();
				}
				Notification.show(item.toString());
			}
		});
		
		final Label toggleRepaintOnSelectionLabel = new Label("Toggle repaint on selection");
		toggleRepaintOnSelectionLabel.setWidthUndefined();
		
		final Button toggleRepaintOnSelectionButton = new Button();
		toggleRepaintOnSelectionButton.setCaption(toggleButton(comboBoxMultiselect.isRepaintOnSelection(), toggleRepaintOnSelectionButton));
		toggleRepaintOnSelectionButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				comboBoxMultiselect.setRepaintOnSelection(!comboBoxMultiselect.isRepaintOnSelection());
				String caption = toggleButton(comboBoxMultiselect.isRepaintOnSelection(), toggleRepaintOnSelectionButton);				
				
				Notification.show("Repaint on selection: " + caption);
				toggleRepaintOnSelectionButton.setCaption(caption);
			}
		});
		
		final Label toggleClearOnlyFilteredLabel = new Label("Toggle clear only filtered");
		toggleClearOnlyFilteredLabel.setWidthUndefined();
		
		final Button toggleClearOnlyFilteredButton = new Button();
		toggleClearOnlyFilteredButton.setCaption(toggleButton(comboBoxMultiselect.isClearOnlyFiltered(), toggleClearOnlyFilteredButton));
		toggleClearOnlyFilteredButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				comboBoxMultiselect.setClearOnlyFiltered(!comboBoxMultiselect.isClearOnlyFiltered());
				String caption = toggleButton(comboBoxMultiselect.isClearOnlyFiltered(), toggleClearOnlyFilteredButton);
				
				Notification.show("Clear only filtered: " + caption);
				toggleClearOnlyFilteredButton.setCaption(caption);
			}
		});
		
		final Label pageCountLabel = new Label("Page count");
		pageCountLabel.setWidthUndefined();
		
		final TextField pageCountTextField = new TextField();
		pageCountTextField.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				try {
					comboBoxMultiselect.setPageCount(Integer.parseInt(pageCountTextField.getValue()));
				} catch (NumberFormatException e) {
					Notification.show("Not a number: " + pageCountTextField.getValue(), Type.ERROR_MESSAGE);
				}
			}
		});
		pageCountTextField.setValue("3");

		VerticalLayout comboboxesMultiselectButtonsLayout = new VerticalLayout();
		comboboxesMultiselectButtonsLayout.setWidth(100, Unit.PERCENTAGE);
		comboboxesMultiselectButtonsLayout.addComponent(toggleRepaintOnSelectionLabel);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(toggleRepaintOnSelectionLabel, Alignment.MIDDLE_CENTER);
		comboboxesMultiselectButtonsLayout.addComponent(toggleRepaintOnSelectionButton);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(toggleRepaintOnSelectionButton, Alignment.MIDDLE_CENTER);
		comboboxesMultiselectButtonsLayout.addComponent(toggleClearOnlyFilteredLabel);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(toggleClearOnlyFilteredLabel, Alignment.MIDDLE_CENTER);
		comboboxesMultiselectButtonsLayout.addComponent(toggleClearOnlyFilteredButton);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(toggleClearOnlyFilteredButton, Alignment.MIDDLE_CENTER);
		comboboxesMultiselectButtonsLayout.addComponent(pageCountLabel);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(pageCountLabel, Alignment.MIDDLE_CENTER);
		comboboxesMultiselectButtonsLayout.addComponent(pageCountTextField);
		comboboxesMultiselectButtonsLayout.setComponentAlignment(pageCountTextField, Alignment.MIDDLE_CENTER);
		
		HorizontalLayout comboboxesButtonsLayout = new HorizontalLayout();
		comboboxesButtonsLayout.setWidth(100, Unit.PERCENTAGE);
		comboboxesButtonsLayout.addComponent(comboboxesMultiselectButtonsLayout);
		comboboxesButtonsLayout.addComponent(new VerticalLayout());
		
		// Show it in the middle of the screen
		final HorizontalLayout comboboxesLayout = new HorizontalLayout();
		comboboxesLayout.setSizeFull();
		comboboxesLayout.addComponent(comboBoxMultiselect);
		comboboxesLayout.setComponentAlignment(comboBoxMultiselect, Alignment.MIDDLE_CENTER);
		comboboxesLayout.addComponent(comboBox);
		comboboxesLayout.setComponentAlignment(comboBox, Alignment.MIDDLE_CENTER);

		final VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setStyleName("demoContentLayout");
		verticalLayout.setSizeFull();
		verticalLayout.addComponent(comboboxesLayout);
		verticalLayout.addComponent(comboboxesButtonsLayout);
		verticalLayout.addComponent(button);
		verticalLayout.setComponentAlignment(button, Alignment.MIDDLE_CENTER);

		setContent(verticalLayout);
	}

	protected String toggleButton(boolean on, Button button) {
		if (on) {
			button.addStyleName(ValoTheme.BUTTON_PRIMARY);
			return "on";
		} 
			
		button.removeStyleName(ValoTheme.BUTTON_PRIMARY);
		return "off";
	}

	
}
