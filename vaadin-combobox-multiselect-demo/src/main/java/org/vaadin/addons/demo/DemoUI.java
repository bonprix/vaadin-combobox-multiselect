package org.vaadin.addons.demo;

import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect;
import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect.ShowButton;
import org.vaadin.addons.demo.model.NamedObject;
import org.vaadin.addons.demo.theme.ValoThemeUI;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("demo")
@Title("ComboBoxMultiselect Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI {

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "org.vaadin.addons.demo.DemoWidgetSet")
	public static class Servlet extends VaadinServlet {
	}

	@Override
	protected void init(VaadinRequest request) {
		
		/*
		 * ComboBoxMultiselect Demo
		 * 
		 * Vaadin ComboBox reference
		 * https://github.com/vaadin/vaadin/blob/master/uitest/src/com/vaadin/tests/themes/valo/ComboBoxes.java
		 */
		
		/* ComboBoxMultiselect */
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(true);
		
		BeanItemContainer beanContainer = new BeanItemContainer(NamedObject.class);
		beanContainer.addBean(new NamedObject(1L, "Vaadin"));
		beanContainer.addBean(new NamedObject(2L, "Bonprix"));
		beanContainer.addBean(new NamedObject(3L, "ComboBox"));
		beanContainer.addBean(new NamedObject(4L, "Multiselect"));
		ComboBoxMultiselect beanComboBox = new ComboBoxMultiselect("beanItemContainer", beanContainer);
		beanComboBox.setShowSelectAllButton(new ShowButton() {
			
			@Override
			public boolean isShow(String filter, int page) {
				// TODO Auto-generated method stub
				return true;
			}
		});
		main.addComponent(beanComboBox);
		
		Label h1 = new Label("org.vaadin.addons.ComboBoxMultiselect");
        h1.addStyleName("h1");
        main.addComponent(h1);

        HorizontalLayout row = new HorizontalLayout();
        row.addStyleName("wrapping");
        row.setSpacing(true);
        main.addComponent(row);

        ComboBoxMultiselect comboBoxMultiselect = new ComboBoxMultiselect("Normal");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
//        comboBoxMultiselect.select(comboBoxMultiselect.getItemIds()
//                          .iterator()
//                          .next());
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.setItemIcon(comboBoxMultiselect.getItemIds()
                               .iterator()
                               .next(),
                          new ThemeResource("../runo/icons/16/document.png"));
        comboBoxMultiselect.selectAll();
        comboBoxMultiselect.setShowSelectAllButton(new ShowButton() {
			
			@Override
			public boolean isShow(String filter, int page) {
				// TODO Auto-generated method stub
				return true;
			}
		});
        row.addComponent(comboBoxMultiselect);

      /*  CssLayout group = new CssLayout();
        group.setCaption("Grouped with a Button");
        group.addStyleName("v-component-group");
        row.addComponent(group);

        comboBoxMultiselect = new ComboBoxMultiselect();
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.select(comboBoxMultiselect.getItemIds()
                          .iterator()
                          .next());
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.setWidth("240px");
        group.addComponent(comboBoxMultiselect);
        Button today = new Button("Do It");
        group.addComponent(today);

        comboBoxMultiselect = new ComboBoxMultiselect("Explicit size");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.setWidth("260px");
        comboBoxMultiselect.setHeight("60px");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("No text input allowed");
        comboBoxMultiselect.setInputPrompt("You can click here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.setTextInputAllowed(false);
        comboBoxMultiselect.select("Option One");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Error");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.select("Option One");
        comboBoxMultiselect.setComponentError(new UserError("Fix it, now!"));
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Error, borderless");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.select("Option One");
        comboBoxMultiselect.setComponentError(new UserError("Fix it, now!"));
        comboBoxMultiselect.addStyleName("borderless");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Disabled");
        comboBoxMultiselect.setInputPrompt("You can't type here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.setEnabled(false);
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Custom color");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("color1");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Custom color");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("color2");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Custom color");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("color3");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Small");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("small");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Large");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("large");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Borderless");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.addItem("Option One");
        comboBoxMultiselect.addItem("Option Two");
        comboBoxMultiselect.addItem("Option Three");
        comboBoxMultiselect.addStyleName("borderless");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Tiny");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("tiny");
        row.addComponent(comboBoxMultiselect);

        comboBoxMultiselect = new ComboBoxMultiselect("Huge");
        comboBoxMultiselect.setInputPrompt("You can type here");
        comboBoxMultiselect.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBoxMultiselect.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBoxMultiselect.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBoxMultiselect.addStyleName("huge");
        row.addComponent(comboBoxMultiselect);
*/
        /* Vaadin ComboBox
         * 
         * just as a reference in this demo
         */
        /*
		Label h2 = new Label("com.vaadin.ui.ComboBox");
        h2.addStyleName("h1");
        main.addComponent(h2);

        HorizontalLayout rowComboBox = new HorizontalLayout();
        rowComboBox.addStyleName("wrapping");
        rowComboBox.setSpacing(true);
        main.addComponent(rowComboBox);

        ComboBox comboBox = new ComboBox("Normal");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setNullSelectionAllowed(false);
        comboBox.select(comboBox.getItemIds()
                          .iterator()
                          .next());
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.setItemIcon(comboBox.getItemIds()
                               .iterator()
                               .next(),
                          new ThemeResource("../runo/icons/16/document.png"));
        rowComboBox.addComponent(comboBox);

        CssLayout groupComboBox = new CssLayout();
        groupComboBox.setCaption("Grouped with a Button");
        groupComboBox.addStyleName("v-component-group");
        rowComboBox.addComponent(groupComboBox);

        comboBox = new ComboBox();
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setNullSelectionAllowed(false);
        comboBox.select(comboBox.getItemIds()
                          .iterator()
                          .next());
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.setWidth("240px");
        groupComboBox.addComponent(comboBox);
        Button todayComboBox = new Button("Do It");
        groupComboBox.addComponent(todayComboBox);

        comboBox = new ComboBox("Explicit size");
        comboBox.setInputPrompt("You can type here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.setWidth("260px");
        comboBox.setHeight("60px");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("No text input allowed");
        comboBox.setInputPrompt("You can click here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.setTextInputAllowed(false);
        comboBox.setNullSelectionAllowed(false);
        comboBox.select("Option One");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Error");
        comboBox.setInputPrompt("You can type here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.setNullSelectionAllowed(false);
        comboBox.select("Option One");
        comboBox.setComponentError(new UserError("Fix it, now!"));
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Error, borderless");
        comboBox.setInputPrompt("You can type here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.setNullSelectionAllowed(false);
        comboBox.select("Option One");
        comboBox.setComponentError(new UserError("Fix it, now!"));
        comboBox.addStyleName("borderless");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Disabled");
        comboBox.setInputPrompt("You can't type here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.setEnabled(false);
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Custom color");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("color1");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Custom color");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("color2");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Custom color");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("color3");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Small");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("small");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Large");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("large");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Borderless");
        comboBox.setInputPrompt("You can type here");
        comboBox.addItem("Option One");
        comboBox.addItem("Option Two");
        comboBox.addItem("Option Three");
        comboBox.addStyleName("borderless");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Tiny");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("tiny");
        rowComboBox.addComponent(comboBox);

        comboBox = new ComboBox("Huge");
        comboBox.setInputPrompt("You can type here");
        comboBox.setContainerDataSource(ValoThemeUI.generateContainer(200, false));
        comboBox.setItemCaptionPropertyId(ValoThemeUI.CAPTION_PROPERTY);
        comboBox.setItemIconPropertyId(ValoThemeUI.ICON_PROPERTY);
        comboBox.addStyleName("huge");
        rowComboBox.addComponent(comboBox);
        */
	        
        setContent(main);
	}
	
}
