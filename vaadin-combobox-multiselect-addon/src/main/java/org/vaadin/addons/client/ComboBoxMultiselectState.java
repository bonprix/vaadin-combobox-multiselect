/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.addons.client;

import java.util.LinkedHashSet;
import java.util.Set;

import com.vaadin.shared.annotations.DelegateToWidget;
import com.vaadin.shared.annotations.NoLayout;
import com.vaadin.shared.ui.abstractmultiselect.AbstractMultiSelectState;

/**
 * Shared state for the ComboBoxMultiselect component.
 *
 * @since 7.0
 */
public class ComboBoxMultiselectState extends AbstractMultiSelectState {
    {
        // TODO ideally this would be v-combobox, but that would affect a lot of
        // themes
        this.primaryStyleName = "v-filterselect";
    }

    /**
     * The keys of the currently selected items or {@code null} if no item is
     * selected.
     */
    public Set<String> selectedItemKeys = new LinkedHashSet<>();

    /**
     * If text input is not allowed, the ComboBoxMultiselect behaves like a
     * pretty NativeSelect - the user can not enter any text and clicking the
     * text field opens the drop down with options.
     *
     * @since 8.0
     */
    @DelegateToWidget
    public boolean textInputAllowed = true;

    /**
     * The prompt to display in an empty field. Null when disabled.
     */
    @DelegateToWidget
    @NoLayout
    public String placeholder = null;

    /**
     * Number of items to show per page or 0 to disable paging.
     */
    @DelegateToWidget
    public int pageLength;

    /**
     * Suggestion pop-up's width as a CSS string. By using relative units (e.g.
     * "50%") it's possible to set the popup's width relative to the
     * ComboBoxMultiselect itself.
     */
    @DelegateToWidget
    public String suggestionPopupWidth = "100%";

    /**
     * True to allow the user to send new items to the server, false to only
     * select among existing items.
     */
    @DelegateToWidget
    public boolean allowNewItems = false;

    /**
     * True to automatically scroll the ComboBoxMultiselect to show the selected
     * item, false not to search for it in the results.
     */
    public boolean scrollToSelectedItem = false;

    /**
     * The caption of the currently selected items or {@code null} if no item is
     * selected.
     */
    public String selectedItemsCaption;

    /**
     * The caption of the clear button.
     */
    @DelegateToWidget
    public String clearButtonCaption = "clear";

    /**
     * The caption of the select all button.
     */
    @DelegateToWidget
    public String selectAllButtonCaption = "select all";

    /**
     * If the clear button should be visible.
     */
    @DelegateToWidget
    public boolean showClearButton;

    /**
     * If the select all button should be visible.
     */
    @DelegateToWidget
    public boolean showSelectAllButton;

}
