package org.vaadin.addons.renderer;



/**
 * Renderer used to create caption of label used in each row
 * for each element
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 */
public interface ComboBoxMultiselectItemCaptionRenderer<O> {

	public String getItemCaption(O object);
}
