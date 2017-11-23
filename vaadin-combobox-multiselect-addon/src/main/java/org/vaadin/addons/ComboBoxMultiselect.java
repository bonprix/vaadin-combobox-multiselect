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

package org.vaadin.addons;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.vaadin.addons.client.ComboBoxMultiselectConstants;
import org.vaadin.addons.client.ComboBoxMultiselectServerRpc;
import org.vaadin.addons.client.ComboBoxMultiselectState;

import com.vaadin.data.HasFilterableDataProvider;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusAndBlurServerRpcDecorator;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.SerializableBiPredicate;
import com.vaadin.server.SerializableConsumer;
import com.vaadin.server.SerializableFunction;
import com.vaadin.server.SerializableToIntFunction;
import com.vaadin.shared.Registration;
import com.vaadin.shared.data.DataCommunicatorConstants;
import com.vaadin.ui.AbstractMultiSelect;
import com.vaadin.ui.IconGenerator;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.declarative.DesignAttributeHandler;
import com.vaadin.ui.declarative.DesignContext;
import com.vaadin.ui.declarative.DesignFormatter;

import elemental.json.JsonObject;

/**
 * A filtering dropdown single-select. Items are filtered based on user input. Supports the creation of new items when a handler is set by the user.
 *
 * @param <T> item (bean) type in ComboBoxMultiselect
 * @author Vaadin Ltd
 */
@SuppressWarnings("serial")
public class ComboBoxMultiselect<T> extends AbstractMultiSelect<T>
implements FieldEvents.BlurNotifier, FieldEvents.FocusNotifier, HasFilterableDataProvider<T, String> {

    /**
     * A callback method for fetching items. The callback is provided with a non-null string filter, offset index and limit.
     *
     * @param <T> item (bean) type in ComboBoxMultiselect
     * @since 8.0
     */
    @FunctionalInterface
    public interface FetchItemsCallback<T> extends Serializable {

        /**
         * Returns a stream of items that match the given filter, limiting the results with given offset and limit.
         * <p>
         * This method is called after the size of the data set is asked from a related size callback. The offset and limit are promised to be within the size
         * of the data set.
         *
         * @param filter a non-null filter string
         * @param offset the first index to fetch
         * @param limit the fetched item count
         * @return stream of items
         */
        public Stream<T> fetchItems(String filter, int offset, int limit);
    }

    /**
     * Handler that adds a new item based on user input when the new items allowed mode is active.
     *
     * @since 8.0
     */
    @FunctionalInterface
    public interface NewItemHandler extends SerializableConsumer<String> {
    }

    /**
     * Generator that handles the value of the textfield when not selected.
     *
     * @since 8.0
     */
    @FunctionalInterface
    public interface InputTextFieldCaptionGenerator<T> extends SerializableFunction<List<T>, String> {
    }

    /**
     * Item style generator class for declarative support.
     * <p>
     * Provides a straightforward mapping between an item and its style.
     *
     * @param <T> item type
     * @since 8.0
     */
    protected static class DeclarativeStyleGenerator<T> implements StyleGenerator<T> {

        private final StyleGenerator<T> fallback;
        private final Map<T, String> styles = new HashMap<>();

        public DeclarativeStyleGenerator(final StyleGenerator<T> fallback) {
            this.fallback = fallback;
        }

        @Override
        public String apply(final T item) {
            return this.styles.containsKey(item) ? this.styles.get(item) : this.fallback.apply(item);
        }

        /**
         * Sets a {@code style} for the {@code item}.
         *
         * @param item a data item
         * @param style a style for the {@code item}
         */
        protected void setStyle(final T item, final String style) {
            this.styles.put(item, style);
        }
    }

    private final ComboBoxMultiselectServerRpc rpc = new ComboBoxMultiselectServerRpc() {
        @Override
        public void createNewItem(final String itemValue) {
            // New option entered
            if (getNewItemHandler() != null && itemValue != null && itemValue.length() > 0) {
                getNewItemHandler().accept(itemValue);
            }
        }

        @Override
        public void setFilter(final String filterText) {
            ComboBoxMultiselect.this.currentFilterText = filterText;
            ComboBoxMultiselect.this.filterSlot.accept(filterText);
        }

        @Override
        public void updateSelection(final Set<String> selectedItemKeys, final Set<String> deselectedItemKeys, final boolean sortingNeeded) {
            ComboBoxMultiselect.this.updateSelection(getItemsForSelectionChange(selectedItemKeys), getItemsForSelectionChange(deselectedItemKeys), true,
                                                     sortingNeeded);
        }

        private Set<T> getItemsForSelectionChange(final Set<String> keys) {
            return keys.stream()
                    .map(key -> getItemForSelectionChange(key))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        }

        private Optional<T> getItemForSelectionChange(final String key) {
            final T item = getDataCommunicator().getKeyMapper()
                    .get(key);
            if (item == null || !getItemEnabledProvider().test(item)) {
                return Optional.empty();
            }

            return Optional.of(item);
        }

        @Override
        public void blur() {
            ComboBoxMultiselect.this.sortingSelection = Collections.unmodifiableCollection(getSelectedItems());
            getDataProvider().refreshAll();
        }

        @Override
        public void selectAll(final String filter) {
            final ListDataProvider<T> listDataProvider = ((ListDataProvider) getDataProvider());
            final Set<String> addedItems = listDataProvider.getItems()
                    .stream()
                    .filter(t -> {
                        final String caption = getItemCaptionGenerator().apply(t);
                        if (t == null) {
                            return false;
                        }
                        return caption.toLowerCase()
                                .contains(filter.toLowerCase());
                    })
                    .map(t -> itemToKey(t))
                    .collect(Collectors.toSet());
            updateSelection(addedItems, new HashSet<>(), true);
        }

        @Override
        public void clear(final String filter) {
            final ListDataProvider<T> listDataProvider = ((ListDataProvider) getDataProvider());
            final Set<String> removedItems = listDataProvider.getItems()
                    .stream()
                    .filter(t -> {
                        final String caption = getItemCaptionGenerator().apply(t);
                        if (t == null) {
                            return false;
                        }
                        return caption.toLowerCase()
                                .contains(filter.toLowerCase());
                    })
                    .map(t -> itemToKey(t))
                    .collect(Collectors.toSet());
            updateSelection(new HashSet<>(), removedItems, true);
        };
    };

    /**
     * Handler for new items entered by the user.
     */
    private NewItemHandler newItemHandler;

    private StyleGenerator<T> itemStyleGenerator = item -> null;

    private String currentFilterText;

    private SerializableConsumer<String> filterSlot = filter -> {
        // Just ignore when neither setDataProvider nor setItems has been called
    };

    private final InputTextFieldCaptionGenerator<T> inputTextFieldCaptionGenerator = items -> {
        if (items.isEmpty()) {
            return "";
        }

        final List<String> captions = new ArrayList<>();

        if (getState().selectedItemKeys != null) {
            for (final T item : items) {
                if (item != null) {
                    captions.add(getItemCaptionGenerator().apply(item));
                }
            }
        }

        return "(" + captions.size() + ") " + StringUtils.join(captions, "; ");
    };

    private Collection<T> sortingSelection = Collections.unmodifiableCollection(new ArrayList<>());

    /**
     * Constructs an empty combo box without a caption. The content of the combo box can be set with {@link #setDataProvider(DataProvider)} or
     * {@link #setItems(Collection)}
     */
    public ComboBoxMultiselect() {
        super();

        init();
    }

    /**
     * Constructs an empty combo box, whose content can be set with {@link #setDataProvider(DataProvider)} or {@link #setItems(Collection)}.
     *
     * @param caption the caption to show in the containing layout, null for no caption
     */
    public ComboBoxMultiselect(final String caption) {
        this();
        setCaption(caption);
    }

    /**
     * Constructs a combo box with a static in-memory data provider with the given options.
     *
     * @param caption the caption to show in the containing layout, null for no caption
     * @param options collection of options, not null
     */
    public ComboBoxMultiselect(final String caption, final Collection<T> options) {
        this(caption);

        setItems(options);
    }

    /**
     * Initialize the ComboBoxMultiselect with default settings and register client to server RPC implementation.
     */
    private void init() {
        registerRpc(this.rpc);
        registerRpc(new FocusAndBlurServerRpcDecorator(this, this::fireEvent));

        addDataGenerator((final T data, final JsonObject jsonObject) -> {
            String caption = getItemCaptionGenerator().apply(data);
            if (caption == null) {
                caption = "";
            }
            jsonObject.put(DataCommunicatorConstants.NAME, caption);
            final String style = this.itemStyleGenerator.apply(data);
            if (style != null) {
                jsonObject.put(ComboBoxMultiselectConstants.STYLE, style);
            }
            final Resource icon = getItemIconGenerator().apply(data);
            if (icon != null) {
                final String iconUrl = ResourceReference.create(icon, ComboBoxMultiselect.this, null)
                        .getURL();
                jsonObject.put(ComboBoxMultiselectConstants.ICON, iconUrl);
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Filtering will use a case insensitive match to show all items where the filter text is a substring of the caption displayed for that item.
     */
    @Override
    public void setItems(final Collection<T> items) {
        final ListDataProvider<T> listDataProvider = DataProvider.ofCollection(items);

        setDataProvider(listDataProvider);

        // sets the PageLength to 10.
        // if there are less items the 10 in the combobox, PageLength will get the amount of items.
        setPageLength(getDataProvider().size(new Query<>()) > 9 ? 10 : getDataProvider().size(new Query<>()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Filtering will use a case insensitive match to show all items where the filter text is a substring of the caption displayed for that item.
     */
    @Override
    public void setItems(final Stream<T> streamOfItems) {
        // Overridden only to add clarification to javadocs
        super.setItems(streamOfItems);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Filtering will use a case insensitive match to show all items where the filter text is a substring of the caption displayed for that item.
     */
    @Override
    public void setItems(@SuppressWarnings("unchecked") final T... items) {
        // Overridden only to add clarification to javadocs
        super.setItems(items);
    }

    /**
     * Sets a list data provider as the data provider of this combo box. Filtering will use a case insensitive match to show all items where the filter text is
     * a substring of the caption displayed for that item.
     * <p>
     * Note that this is a shorthand that calls {@link #setDataProvider(DataProvider)} with a wrapper of the provided list data provider. This means that
     * {@link #getDataProvider()} will return the wrapper instead of the original list data provider.
     *
     * @param listDataProvider the list data provider to use, not <code>null</code>
     * @since 8.0
     */
    public void setDataProvider(final ListDataProvider<T> listDataProvider) {
        // Cannot use the case insensitive contains shorthand from
        // ListDataProvider since it wouldn't react to locale changes
        final CaptionFilter defaultCaptionFilter = (itemText, filterText) -> itemText.toLowerCase(getLocale())
                .contains(filterText.toLowerCase(getLocale()));

        setDataProvider(defaultCaptionFilter, listDataProvider);
    }

    /**
     * Sets the data items of this listing and a simple string filter with which the item string and the text the user has input are compared.
     * <p>
     * Note that unlike {@link #setItems(Collection)}, no automatic case conversion is performed before the comparison.
     *
     * @param captionFilter filter to check if an item is shown when user typed some text into the ComboBoxMultiselect
     * @param items the data items to display
     * @since 8.0
     */
    public void setItems(final CaptionFilter captionFilter, final Collection<T> items) {
        final ListDataProvider<T> listDataProvider = DataProvider.ofCollection(items);

        setDataProvider(captionFilter, listDataProvider);
    }

    /**
     * Sets a list data provider with an item caption filter as the data provider of this combo box. The caption filter is used to compare the displayed caption
     * of each item to the filter text entered by the user.
     *
     * @param captionFilter filter to check if an item is shown when user typed some text into the ComboBoxMultiselect
     * @param listDataProvider the list data provider to use, not <code>null</code>
     * @since 8.0
     */
    public void setDataProvider(final CaptionFilter captionFilter, final ListDataProvider<T> listDataProvider) {
        Objects.requireNonNull(listDataProvider, "List data provider cannot be null");

        // Must do getItemCaptionGenerator() for each operation since it might
        // not be the same as when this method was invoked
        setDataProvider(listDataProvider, filterText -> item -> captionFilter.test(getItemCaptionGenerator().apply(item), filterText));
    }

    /**
     * Sets the data items of this listing and a simple string filter with which the item string and the text the user has input are compared.
     * <p>
     * Note that unlike {@link #setItems(Collection)}, no automatic case conversion is performed before the comparison.
     *
     * @param captionFilter filter to check if an item is shown when user typed some text into the ComboBoxMultiselect
     * @param items the data items to display
     * @since 8.0
     */
    public void setItems(final CaptionFilter captionFilter, @SuppressWarnings("unchecked") final T... items) {
        setItems(captionFilter, Arrays.asList(items));
    }

    /**
     * Gets the current placeholder text shown when the combo box would be empty.
     *
     * @see #setPlaceholder(String)
     * @return the current placeholder string, or null if not enabled
     * @since 8.0
     */
    public String getPlaceholder() {
        return getState(false).placeholder;
    }

    /**
     * Sets the placeholder string - a textual prompt that is displayed when the select would otherwise be empty, to prompt the user for input.
     *
     * @param placeholder the desired placeholder, or null to disable
     * @since 8.0
     */
    public void setPlaceholder(final String placeholder) {
        getState().placeholder = placeholder;
    }

    /**
     * Sets whether it is possible to input text into the field or whether the field area of the component is just used to show what is selected. By disabling
     * text input, the comboBox will work in the same way as a {@link NativeSelect}
     *
     * @see #isTextInputAllowed()
     *
     * @param textInputAllowed true to allow entering text, false to just show the current selection
     */
    public void setTextInputAllowed(final boolean textInputAllowed) {
        getState().textInputAllowed = textInputAllowed;
    }

    /**
     * Returns true if the user can enter text into the field to either filter the selections or enter a new value if new item handler is set (see
     * {@link #setNewItemHandler(NewItemHandler)}. If text input is disabled, the comboBox will work in the same way as a {@link NativeSelect}
     *
     * @return true if text input is allowed
     */
    public boolean isTextInputAllowed() {
        return getState(false).textInputAllowed;
    }

    @Override
    public Registration addBlurListener(final BlurListener listener) {
        return addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener, BlurListener.blurMethod);
    }

    @Override
    public Registration addFocusListener(final FocusListener listener) {
        return addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener, FocusListener.focusMethod);
    }

    /**
     * Returns the page length of the suggestion popup.
     *
     * @return the pageLength
     */
    public int getPageLength() {
        return getState(false).pageLength;
    }

    /**
     * Returns the suggestion pop-up's width as a CSS string. By default this width is set to "100%".
     *
     * @see #setPopupWidth
     * @since 7.7
     * @return explicitly set popup width as CSS size string or null if not set
     */
    public String getPopupWidth() {
        return getState(false).suggestionPopupWidth;
    }

    /**
     * Sets the page length for the suggestion popup. Setting the page length to 0 will disable suggestion popup paging (all items visible).
     *
     * @param pageLength the pageLength to set
     */
    public void setPageLength(final int pageLength) {
        getState().pageLength = pageLength;
    }

    /**
     * Sets the suggestion pop-up's width as a CSS string. By using relative units (e.g. "50%") it's possible to set the popup's width relative to the
     * ComboBoxMultiselect itself.
     * <p>
     * By default this width is set to "100%" so that the pop-up's width is equal to the width of the combobox. By setting width to null the pop-up's width will
     * automatically expand beyond 100% relative width to fit the content of all displayed items.
     *
     * @see #getPopupWidth()
     * @since 7.7
     * @param width the width
     */
    public void setPopupWidth(final String width) {
        getState().suggestionPopupWidth = width;
    }

    /**
     * Sets whether to scroll the selected item visible (directly open the page on which it is) when opening the combo box popup or not.
     * <p>
     * This requires finding the index of the item, which can be expensive in many large lazy loading containers.
     *
     * @param scrollToSelectedItem true to find the page with the selected item when opening the selection popup
     */
    public void setScrollToSelectedItem(final boolean scrollToSelectedItem) {
        getState().scrollToSelectedItem = scrollToSelectedItem;
    }

    /**
     * Returns true if the select should find the page with the selected item when opening the popup.
     *
     * @see #setScrollToSelectedItem(boolean)
     *
     * @return true if the page with the selected item will be shown when opening the popup
     */
    public boolean isScrollToSelectedItem() {
        return getState(false).scrollToSelectedItem;
    }

    /**
     * Sets the style generator that is used to produce custom class names for items visible in the popup. The CSS class name that will be added to the item is
     * <tt>v-filterselect-item-[style name]</tt>. Returning null from the generator results in no custom style name being set.
     *
     * @see StyleGenerator
     *
     * @param itemStyleGenerator the item style generator to set, not null
     * @throws NullPointerException if {@code itemStyleGenerator} is {@code null}
     * @since 8.0
     */
    public void setStyleGenerator(final StyleGenerator<T> itemStyleGenerator) {
        Objects.requireNonNull(itemStyleGenerator, "Item style generator must not be null");
        this.itemStyleGenerator = itemStyleGenerator;
        getDataCommunicator().reset();
    }

    /**
     * Gets the currently used style generator that is used to generate CSS class names for items. The default item style provider returns null for all items,
     * resulting in no custom item class names being set.
     *
     * @see StyleGenerator
     * @see #setStyleGenerator(StyleGenerator)
     *
     * @return the currently used item style generator, not null
     * @since 8.0
     */
    public StyleGenerator<T> getStyleGenerator() {
        return this.itemStyleGenerator;
    }

    @Override
    public void setItemIconGenerator(final IconGenerator<T> itemIconGenerator) {
        super.setItemIconGenerator(itemIconGenerator);
    }

    /**
     * Sets the handler that is called when user types a new item. The creation of new items is allowed when a new item handler has been set.
     *
     * @param newItemHandler handler called for new items, null to only permit the selection of existing items
     * @since 8.0
     */
    public void setNewItemHandler(final NewItemHandler newItemHandler) {
        this.newItemHandler = newItemHandler;
        getState().allowNewItems = newItemHandler != null;
        markAsDirty();
    }

    /**
     * Returns the handler called when the user enters a new item (not present in the data provider).
     *
     * @return new item handler or null if none specified
     */
    public NewItemHandler getNewItemHandler() {
        return this.newItemHandler;
    }

    // HasValue methods delegated to the selection model

    @Override
    public Registration addValueChangeListener(final HasValue.ValueChangeListener<Set<T>> listener) {
        return addSelectionListener(event -> listener
                                    .valueChange(new ValueChangeEvent<>(event.getComponent(), this, event.getOldValue(), event.isUserOriginated())));
    }

    @Override
    protected ComboBoxMultiselectState getState() {
        return (ComboBoxMultiselectState) super.getState();
    }

    @Override
    protected ComboBoxMultiselectState getState(final boolean markAsDirty) {
        return (ComboBoxMultiselectState) super.getState(markAsDirty);
    }

    @Override
    protected Element writeItem(final Element design, final T item, final DesignContext context) {
        final Element element = design.appendElement("option");

        final String caption = getItemCaptionGenerator().apply(item);
        if (caption != null) {
            element.html(DesignFormatter.encodeForTextNode(caption));
        }
        else {
            element.html(DesignFormatter.encodeForTextNode(item.toString()));
        }
        element.attr("item", item.toString());

        final Resource icon = getItemIconGenerator().apply(item);
        if (icon != null) {
            DesignAttributeHandler.writeAttribute("icon", element.attributes(), icon, null, Resource.class, context);
        }

        final String style = getStyleGenerator().apply(item);
        if (style != null) {
            element.attr("style", style);
        }

        if (isSelected(item)) {
            element.attr("selected", "");
        }

        return element;
    }

    @Override
    protected void readItems(final Element design, final DesignContext context) {
        setStyleGenerator(new DeclarativeStyleGenerator<>(getStyleGenerator()));
        super.readItems(design, context);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected T readItem(final Element child, final Set<T> selected, final DesignContext context) {
        final T item = super.readItem(child, selected, context);

        if (child.hasAttr("style")) {
            final StyleGenerator<T> styleGenerator = getStyleGenerator();
            if (styleGenerator instanceof DeclarativeStyleGenerator) {
                ((DeclarativeStyleGenerator) styleGenerator).setStyle(item, child.attr("style"));
            }
            else {
                throw new IllegalStateException(String.format("Don't know how " + "to set style using current style generator '%s'", styleGenerator.getClass()
                                                              .getName()));
            }
        }
        return item;
    }

    @Override
    public DataProvider<T, ?> getDataProvider() {
        return internalGetDataProvider();
    }

    @Override
    public <C> void setDataProvider(final DataProvider<T, C> dataProvider, final SerializableFunction<String, C> filterConverter) {
        Objects.requireNonNull(dataProvider, "dataProvider cannot be null");
        Objects.requireNonNull(filterConverter, "filterConverter cannot be null");

        final SerializableFunction<String, C> convertOrNull = filterText -> {
            if (filterText == null || filterText.isEmpty()) {
                return null;
            }

            return filterConverter.apply(filterText);
        };

        final SerializableConsumer<C> providerFilterSlot = internalSetDataProvider(dataProvider, convertOrNull.apply(this.currentFilterText));

        this.filterSlot = filter -> providerFilterSlot.accept(convertOrNull.apply(filter));
    }

    @Override
    protected <F> SerializableConsumer<F> internalSetDataProvider(final DataProvider<T, F> dataProvider, final F initialFilter) {
        final SerializableConsumer<F> consumer = super.internalSetDataProvider(dataProvider, initialFilter);

        if (getDataProvider() instanceof ListDataProvider) {
            final ListDataProvider<T> listDataProvider = ((ListDataProvider<T>) getDataProvider());
            listDataProvider.setSortComparator((o1, o2) -> {
                final boolean selected1 = this.sortingSelection.contains(o1);
                final boolean selected2 = this.sortingSelection.contains(o2);

                if (selected1 && !selected2) {
                    return -1;
                }
                if (!selected1 && selected2) {
                    return 1;
                }

                return getItemCaptionGenerator().apply(o1)
                        .compareToIgnoreCase(getItemCaptionGenerator().apply(o2));
            });
        }

        return consumer;
    }

    /**
     * Sets a CallbackDataProvider using the given fetch items callback and a size callback.
     * <p>
     * This method is a shorthand for making a {@link CallbackDataProvider} that handles a partial {@link Query} object.
     *
     * @param fetchItems a callback for fetching items
     * @param sizeCallback a callback for getting the count of items
     *
     * @see CallbackDataProvider
     * @see #setDataProvider(DataProvider)
     */
    public void setDataProvider(final FetchItemsCallback<T> fetchItems, final SerializableToIntFunction<String> sizeCallback) {
        setDataProvider(new CallbackDataProvider<>(q -> fetchItems.fetchItems(q.getFilter()
                                                                              .orElse(""), q.getOffset(), q.getLimit()),
                q -> sizeCallback.applyAsInt(q.getFilter()
                                             .orElse(""))));
    }

    /**
     * Predicate to check {@link ComboBoxMultiselect} item captions against user typed strings.
     *
     * @see #setItems(CaptionFilter, Collection)
     * @see #setItems(CaptionFilter, Object[])
     * @since 8.0
     */
    @FunctionalInterface
    public interface CaptionFilter extends SerializableBiPredicate<String, String> {

        /**
         * Check item caption against entered text.
         *
         * @param itemCaption the caption of the item to filter, not {@code null}
         * @param filterText user entered filter, not {@code null}
         * @return {@code true} if item passes the filter and should be listed, {@code false} otherwise
         */
        @Override
        public boolean test(String itemCaption, String filterText);
    }

    /**
     * Removes the given items. Any item that is not currently selected, is ignored. If none of the items are selected, does nothing.
     *
     * @param items the items to deselect, not {@code null}
     * @param userOriginated {@code true} if this was used originated, {@code false} if not
     */
    @Override
    protected void deselect(final Set<T> items, final boolean userOriginated) {
        Objects.requireNonNull(items);
        if (items.stream()
                .noneMatch(i -> isSelected(i))) {
            return;
        }

        updateSelection(set -> set.removeAll(items), userOriginated, true);
    }

    /**
     * Deselects the given item. If the item is not currently selected, does nothing.
     *
     * @param item the item to deselect, not null
     * @param userOriginated {@code true} if this was used originated, {@code false} if not
     */
    @Override
    protected void deselect(final T item, final boolean userOriginated) {
        if (!getSelectedItems().contains(item)) {
            return;
        }

        updateSelection(set -> set.remove(item), userOriginated, false);
    }

    @Override
    public void deselectAll() {
        if (getSelectedItems().isEmpty()) {
            return;
        }

        updateSelection(Collection::clear, false, true);
    }

    /**
     * Selects the given item. Depending on the implementation, may cause other items to be deselected. If the item is already selected, does nothing.
     *
     * @param item the item to select, not null
     * @param userOriginated {@code true} if this was used originated, {@code false} if not
     */
    @Override
    protected void select(final T item, final boolean userOriginated) {
        if (getSelectedItems().contains(item)) {
            return;
        }

        updateSelection(set -> set.add(item), userOriginated, true);
    }

    @Override
    protected void updateSelection(final Set<T> addedItems, final Set<T> removedItems, final boolean userOriginated) {
        updateSelection(addedItems, removedItems, userOriginated, true);
    }

    /**
     * Updates the selection by adding and removing the given items.
     *
     * @param addedItems the items added to selection, not {@code} null
     * @param removedItems the items removed from selection, not {@code} null
     * @param userOriginated {@code true} if this was used originated, {@code false} if not
     * @param sortingNeeded is sorting needed before sending data back to client
     */
    protected void updateSelection(final Set<T> addedItems, final Set<T> removedItems, final boolean userOriginated, final boolean sortingNeeded) {
        Objects.requireNonNull(addedItems);
        Objects.requireNonNull(removedItems);

        // if there are duplicates, some item is both added & removed, just
        // discard that and leave things as was before
        addedItems.removeIf(item -> removedItems.remove(item));

        if (getSelectedItems().containsAll(addedItems) && Collections.disjoint(getSelectedItems(), removedItems)) {
            return;
        }

        updateSelection(set -> {
            // order of add / remove does not matter since no duplicates
            set.removeAll(removedItems);
            set.addAll(addedItems);
        }, userOriginated, sortingNeeded);
    }

    private void updateSelection(final SerializableConsumer<Collection<T>> handler, final boolean userOriginated, final boolean sortingNeeded) {
        final LinkedHashSet<T> oldSelection = new LinkedHashSet<>(getSelectedItems());
        final List<T> selection = new ArrayList<>(getSelectedItems());
        handler.accept(selection);

        if (sortingNeeded) {
            this.sortingSelection = Collections.unmodifiableCollection(selection);
        }

        // TODO selection is private, have to use reflection (remove later)
        try {
            final Field f1 = getSelectionBaseClass().getDeclaredField("selection");
            f1.setAccessible(true);
            f1.set(this, selection);
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        doSetSelectedKeys(selection);

        fireEvent(new MultiSelectionEvent<>(this, oldSelection, userOriginated));

        getDataCommunicator().reset();
        getDataProvider().refreshAll();
    }

    protected Class<?> getSelectionBaseClass() {
        return this.getClass()
                .getSuperclass();
    }

    /**
     * Sets the selected item based on the given communication key. If the key is {@code null}, clears the current selection if any.
     *
     * @param items the selected items or {@code null} to clear selection
     */
    protected void doSetSelectedKeys(final List<T> items) {
        final Set<String> keys = itemsToKeys(items);

        getState().selectedItemKeys = keys;

        updateSelectedItemsCaption();
    }

    private void updateSelectedItemsCaption() {
        final List<T> items = new ArrayList<>();

        if (getState().selectedItemKeys != null && !getState().selectedItemKeys.isEmpty()) {
            for (final String selectedItemKey : getState().selectedItemKeys) {
                final T value = getDataCommunicator().getKeyMapper()
                        .get(selectedItemKey);
                if (value != null) {
                    items.add(value);
                }
            }
        }

        getState().selectedItemsCaption = this.inputTextFieldCaptionGenerator.apply(items);
    }

    /**
     * Returns the communication key assigned to the given item.
     *
     * @param item the item whose key to return
     * @return the assigned key
     */
    protected String itemToKey(final T item) {
        if (item == null) {
            return null;
        }
        // TODO creates a key if item not in data provider
        return getDataCommunicator().getKeyMapper()
                .key(item);
    }

    /**
     * Returns the communication keys assigned to the given items.
     *
     * @param items the items whose key to return
     * @return the assigned keys
     */
    protected Set<String> itemsToKeys(final List<T> items) {
        if (items == null) {
            return null;
        }

        final Set<String> keys = new LinkedHashSet<>();
        for (final T item : items) {
            keys.add(itemToKey(item));
        }
        return keys;
    }

    public void showClearButton(final boolean showClearButton) {
        getState().showClearButton = showClearButton;
    }

    public void showSelectAllButton(final boolean showSelectAllButton) {
        getState().showSelectAllButton = showSelectAllButton;
    }

    public void setClearButtonCaption(final String clearButtonCaption) {
        getState().clearButtonCaption = clearButtonCaption;
    }

    public void setSelectAllButtonCaption(final String selectAllButtonCaption) {
        getState().selectAllButtonCaption = selectAllButtonCaption;
    }
}
