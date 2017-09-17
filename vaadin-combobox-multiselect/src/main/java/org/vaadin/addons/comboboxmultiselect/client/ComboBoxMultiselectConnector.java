package org.vaadin.addons.comboboxmultiselect.client;

/**
 * Client side implementation of the ComboBoxMultiselect component.
 * Basis is the ComboBoxConnector {@link ComboBoxConnector}
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.vaadin.addons.comboboxmultiselect.ComboBoxMultiselect;
import org.vaadin.addons.comboboxmultiselect.client.VComboBoxMultiselect.FilterSelectSuggestion;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Paintable;
import com.vaadin.client.UIDL;
import com.vaadin.client.VConsole;
import com.vaadin.v7.client.ui.AbstractFieldConnector;
import com.vaadin.client.ui.SimpleManagedLayout;
import com.vaadin.shared.ui.Connect;
import com.vaadin.v7.shared.ui.combobox.ComboBoxConstants;
import com.vaadin.v7.shared.ui.combobox.ComboBoxState;
import com.vaadin.v7.shared.ui.combobox.FilteringMode;

@SuppressWarnings("serial")
@Connect(ComboBoxMultiselect.class)
public class ComboBoxMultiselectConnector extends AbstractFieldConnector implements
        Paintable, SimpleManagedLayout {

	private boolean enableDebug = false;
	
    // oldSuggestionTextMatchTheOldSelection is used to detect when it's safe to
    // update textbox text by a changed item caption.
    private boolean oldSuggestionTextMatchTheOldSelection;

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.client.Paintable#updateFromUIDL(com.vaadin.client.UIDL,
     * com.vaadin.client.ApplicationConnection)
     */
    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
    	debug("updateFromUIDL");
    	
        // Save details
        getWidget().client = client;
        getWidget().paintableId = uidl.getId();

        getWidget().readonly = isReadOnly();
        getWidget().updateReadOnly();

        if (!isRealUpdate(uidl)) {
            return;
        }

        // Inverse logic here to make the default case (text input enabled)
        // work without additional UIDL messages
        boolean noTextInput = uidl
                .hasAttribute(ComboBoxConstants.ATTR_NO_TEXT_INPUT)
                && uidl.getBooleanAttribute(ComboBoxConstants.ATTR_NO_TEXT_INPUT);
        getWidget().setTextInputEnabled(!noTextInput);

        // not a FocusWidget -> needs own tabindex handling
        getWidget().tb.setTabIndex(getState().tabIndex);

        if (uidl.hasAttribute("filteringmode")) {
            getWidget().filteringmode = FilteringMode.valueOf(uidl
                    .getStringAttribute("filteringmode"));
        }

        getWidget().immediate = getState().immediate;

        getWidget().nullSelectionAllowed = uidl.hasAttribute("nullselect");

        getWidget().nullSelectItem = uidl.hasAttribute("nullselectitem")
                && uidl.getBooleanAttribute("nullselectitem");
        
        getWidget().multiselect = uidl.hasAttribute("multiselect");

        getWidget().currentPage = uidl.getIntVariable("page");

        if (uidl.hasAttribute("pagelength")) {
            getWidget().pageLength = uidl.getIntAttribute("pagelength");
        }

        if (uidl.hasAttribute(ComboBoxConstants.ATTR_INPUTPROMPT)) {
            // input prompt changed from server
            getWidget().inputPrompt = uidl
                    .getStringAttribute(ComboBoxConstants.ATTR_INPUTPROMPT);
        } else {
            getWidget().inputPrompt = "";
        }
        
        getWidget().showClearButton = uidl.getBooleanVariable("showClearButton");
        getWidget().clearButtonCaption = uidl.getStringVariable("clearButtonCaption");
        getWidget().showSelectAllButton = uidl.getBooleanVariable("showSelectAllButton");
        getWidget().selectAllButtonCaption = uidl.getStringVariable("selectAllButtonCaption");

        getWidget().suggestionPopup.updateStyleNames(uidl, getState());

        getWidget().allowNewItem = uidl.hasAttribute("allownewitem");
        getWidget().lastNewItemString = null;

        final UIDL options = uidl.getChildUIDL(0);
        debug("options: " + options.toString());
        if (uidl.hasAttribute("totalMatches")) {
            getWidget().totalMatches = uidl.getIntAttribute("totalMatches");
        } else {
            getWidget().totalMatches = 0;
        }

        List<FilterSelectSuggestion> newSuggestions = new ArrayList<FilterSelectSuggestion>();

        for (final Iterator<?> i = options.getChildIterator(); i.hasNext();) {
            final UIDL optionUidl = (UIDL) i.next();
            final FilterSelectSuggestion suggestion = getWidget().new FilterSelectSuggestion(
                    optionUidl);
            newSuggestions.add(suggestion);
        }

        // only close the popup if the suggestions list has actually changed
        boolean suggestionsChanged = !getWidget().initDone
                || !newSuggestions.equals(getWidget().currentSuggestions);

        // An ItemSetChangeEvent on server side clears the current suggestion
        // popup. Popup needs to be repopulated with suggestions from UIDL.
        boolean popupOpenAndCleared = false;

        oldSuggestionTextMatchTheOldSelection = false;

        if (suggestionsChanged) {
            oldSuggestionTextMatchTheOldSelection = isWidgetsCurrentSelectionTextInTextBox();
            getWidget().currentSuggestions.clear();

            if (!getWidget().waitingForFilteringResponse) {
                /*
                 * Clear the current suggestions as the server response always
                 * includes the new ones. Exception is when filtering, then we
                 * need to retain the value if the user does not select any of
                 * the options matching the filter.
                 */
                getWidget().currentSuggestion = null;
                /*
                 * Also ensure no old items in menu. Unless cleared the old
                 * values may cause odd effects on blur events. Suggestions in
                 * menu might not necessary exist in select at all anymore.
                 */
                getWidget().suggestionPopup.menu.clearItems();
                popupOpenAndCleared = getWidget().suggestionPopup.isAttached();

            }
            
            for (FilterSelectSuggestion suggestion : newSuggestions) {
                getWidget().currentSuggestions.add(suggestion);
            }
        }

        // handle selection (null or a single value)
      //  if (uidl.hasVariable("selected")

        // In case we're switching page no need to update the selection as the
        // selection process didn't finish.
        // && getWidget().selectPopupItemWhenResponseIsReceived ==
        // MyComponentWidget.Select.NONE
        //
      //  ) {
        	Set<FilterSelectSuggestion> selectedSuggestions = new HashSet<>();
        	
        	debug("uidl.getChildUIDL(0): " + uidl.getChildUIDL(0).toString());
        	debug("uidl.getChildUIDL(1): " + uidl.getChildUIDL(1).toString());
        	
            if (getWidget().multiselect) {
            	 final UIDL selectedOptions = uidl.getChildUIDL(1);
                 if (uidl.hasAttribute("totalMatches")) {
                     getWidget().totalMatches = uidl.getIntAttribute("totalMatches");
                 } else {
                     getWidget().totalMatches = 0;
                 }

                 if (uidl.hasAttribute("singleSelectionCaption")) {
                	 getWidget().singleSelectionCaption = uidl.getStringAttribute("singleSelectionCaption");
                 }
                 
                 if (uidl.hasAttribute("multiSelectionCaption")) {
                	 getWidget().multiSelectionCaption = uidl.getStringAttribute("multiSelectionCaption");
                 }

                 for (final Iterator<?> i = selectedOptions.getChildIterator(); i.hasNext();) {
                     final UIDL optionUidl = (UIDL) i.next();
                     final FilterSelectSuggestion suggestion = getWidget().new FilterSelectSuggestion(
                             optionUidl);
                     debug("selectedSuggestion: " + suggestion.getOptionKey());
                     selectedSuggestions.add(suggestion);
                 }
            
	            getWidget().selectedOptionKeys = selectedSuggestions;
            }

            // when filtering with empty filter, server sets the selected key
            // to "", which we don't select here. Otherwise we won't be able to
            // reset back to the item that was selected before filtering
            // started.
            if (selectedSuggestions.size() > 0 && !selectedSuggestions.iterator().next().equals("")) {
            	if (!getWidget().multiselect) {
            		if (selectedSuggestions.size() == 1) {
            			performSelection(selectedSuggestions.iterator().next().getOptionKey());
            		}
            	} else {
            		debug("performSelection");
            		for (FilterSelectSuggestion selectedKey : selectedSuggestions) {
						performSelection(selectedKey.getOptionKey());
					}
            		if (!getWidget().suggestionPopup.isShowing() && !getWidget().focused) {
            			getWidget().setPromptingOff(uidl
                                		.getStringAttribute("selectedCaption"));
            		}
            	}
            } else if (!getWidget().waitingForFilteringResponse
                    && uidl.hasAttribute("selectedCaption")) {
                // scrolling to correct page is disabled, caption is passed as a
                // special parameter
                getWidget().tb.setText(uidl
                        .getStringAttribute("selectedCaption"));
            } else {
                resetSelection();
            }
       // }

        if ((getWidget().waitingForFilteringResponse && getWidget().lastFilter
                .toLowerCase().equals(uidl.getStringVariable("filter")))
                || popupOpenAndCleared) {

            getWidget().suggestionPopup.showSuggestions(
                    getWidget().currentSuggestions, getWidget().currentPage,
                    getWidget().totalMatches);

            getWidget().waitingForFilteringResponse = false;

            if (!getWidget().popupOpenerClicked
                    && getWidget().selectPopupItemWhenResponseIsReceived != VComboBoxMultiselect.Select.NONE) {

                // we're paging w/ arrows
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        navigateItemAfterPageChange();
                    }
                });
            }

            if (getWidget().updateSelectionWhenReponseIsReceived) {
                getWidget().suggestionPopup.menu
                        .doPostFilterSelectedItemAction();
            }
        }

        // Calculate minimum textarea width
        getWidget().updateSuggestionPopupMinWidth();

        getWidget().popupOpenerClicked = false;

        /*
         * if this is our first time we need to recalculate the root width.
         */
        if (!getWidget().initDone) {

            getWidget().updateRootWidth();
        }

        // Focus dependent style names are lost during the update, so we add
        // them here back again
        if (getWidget().focused) {
            getWidget().addStyleDependentName("focus");
        }

        getWidget().initDone = true;
    }

    /*
     * This method navigates to the proper item in the combobox page. This
     * should be executed after setSuggestions() method which is called from
     * MyComponentWidget.showSuggestions(). ShowSuggestions() method builds the page
     * content. As far as setSuggestions() method is called as deferred,
     * navigateItemAfterPageChange method should be also be called as deferred.
     * #11333
     */
    private void navigateItemAfterPageChange() {
        if (getWidget().selectPopupItemWhenResponseIsReceived == VComboBoxMultiselect.Select.LAST) {
            getWidget().suggestionPopup.selectLastItem();
        } else {
        	getWidget().suggestionPopup.selectFirstItem();
        }

        // If you're in between 2 requests both changing the page back and
        // forth, you don't want this here, instead you need it before any
        // other request.
        // getWidget().selectPopupItemWhenResponseIsReceived =
        // MyComponentWidget.Select.NONE; // reset
    }

    private void performSelection(String selectedKey) {
    	debug("performSelection(" + selectedKey + ")");
        // some item selected
        for (FilterSelectSuggestion suggestion : getWidget().currentSuggestions) {
            String suggestionKey = suggestion.getOptionKey();
            if (!suggestionKey.equals(selectedKey)) {
                continue;
            }
            if (!getWidget().waitingForFilteringResponse
                    || getWidget().popupOpenerClicked) {
                if (!getWidget().selectedOptionKeys.contains(suggestionKey)
                        || suggestion.getReplacementString().equals(
                                getWidget().tb.getText())
                        || oldSuggestionTextMatchTheOldSelection) {
                    // Update text field if we've got a new
                    // selection
                    // Also update if we've got the same text to
                    // retain old text selection behavior
                    // OR if selected item caption is changed.
                	if(!getWidget().multiselect) {
	                    getWidget().setPromptingOff(
	                            suggestion.getReplacementString());
                	}
                    getWidget().selectedOptionKeys.add(suggestion);
                }
            }
            getWidget().currentSuggestion = suggestion;
            getWidget().setSelectedItemIcon(suggestion.getIconUri());
            // only a single item can be selected
            break;
        }
    }

    private boolean isWidgetsCurrentSelectionTextInTextBox() {
        return getWidget().currentSuggestion != null
                && getWidget().currentSuggestion.getReplacementString().equals(
                        getWidget().tb.getText());
    }

    private void resetSelection() {
        if (!getWidget().waitingForFilteringResponse
                || getWidget().popupOpenerClicked) {
            // select nulled
            if (!getWidget().focused) {
                /*
                 * client.updateComponent overwrites all styles so we must
                 * ALWAYS set the prompting style at this point, even though we
                 * think it has been set already...
                 */
                getWidget().setPromptingOff("");
                if (getWidget().enabled && !getWidget().readonly) {
                    getWidget().setPromptingOn();
                }
            } else {
                // we have focus in field, prompting can't be set on, instead
                // just clear the input if the value has changed from something
                // else to null
                if (!getWidget().selectedOptionKeys.isEmpty()
                        || (getWidget().allowNewItem && !getWidget().tb
                                .getValue().isEmpty())) {
                    getWidget().tb.setValue("");
                }
            }
            getWidget().currentSuggestion = null; // #13217
            getWidget().setSelectedItemIcon(null);
            getWidget().selectedOptionKeys.clear();
        }
    }

    @Override
    public VComboBoxMultiselect getWidget() {
        return (VComboBoxMultiselect) super.getWidget();
    }

    @Override
    public ComboBoxState getState() {
        return (ComboBoxState) super.getState();
    }

    @Override
    public void layout() {
        VComboBoxMultiselect widget = getWidget();
        if (widget.initDone) {
            widget.updateRootWidth();
        }
    }

    @Override
    public void setWidgetEnabled(boolean widgetEnabled) {
        super.setWidgetEnabled(widgetEnabled);
        getWidget().enabled = widgetEnabled;
        getWidget().tb.setEnabled(widgetEnabled);
    }
    
	private void debug(String string) {
		if (enableDebug) {
			VConsole.error(string);
		}
	}

}
