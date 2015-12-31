package org.vaadin.addons.demo;

import org.vaadin.addons.ComboBoxMultiselect;
import org.vaadin.addons.demo.model.NamedObject;
import org.vaadin.addons.renderer.ComboBoxMultiselectItemCaptionRenderer;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;

@Theme("demo")
@Title("MyComponent Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI
{

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "org.vaadin.addons.demo.DemoWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
    	
    	List<NamedObject> list = new ArrayList<NamedObject>();
    	list.add(new NamedObject(1L, "Java"));
    	list.add(new NamedObject(2L, "Vaadin"));

        // Initialize our new UI component
        final ComboBoxMultiselect<NamedObject> comboBoxMultiselect = new ComboBoxMultiselect<NamedObject>("Clear selection");
        comboBoxMultiselect.setCaption("ComboBoxMultiselect");
        comboBoxMultiselect.setItemCaptionRenderer(new ComboBoxMultiselectItemCaptionRenderer<NamedObject>() {
			
			@Override
			public String getItemCaption(NamedObject arg0) {
				return arg0.getName();
			}
		});
        comboBoxMultiselect.addAll(list);
        
        // ComboBox
        final ComboBox comboBox = new ComboBox();
        comboBox.setCaption("ComboBox");
        comboBox.addItems(list);

        // Show it in the middle of the screen
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setStyleName("demoContentLayout");
        layout.setSizeFull();
        layout.addComponent(comboBoxMultiselect);
        layout.setComponentAlignment(comboBoxMultiselect, Alignment.MIDDLE_CENTER);
        layout.addComponent(comboBox);
        layout.setComponentAlignment(comboBox, Alignment.MIDDLE_CENTER);
        setContent(layout);
    }

}
