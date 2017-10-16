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

import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import org.vaadin.addons.ComboBoxMultiselect;
import org.vaadin.addons.client.VComboBoxMultiselect.ComboBoxMultiselectSuggestion;
import org.vaadin.addons.client.VComboBoxMultiselect.DataReceivedHandler;

import com.vaadin.client.Profiler;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.connectors.AbstractListingConnector;
import com.vaadin.client.connectors.data.HasDataSource;
import com.vaadin.client.data.DataChangeHandler;
import com.vaadin.client.data.DataSource;
import com.vaadin.client.ui.HasErrorIndicator;
import com.vaadin.client.ui.HasRequiredIndicator;
import com.vaadin.client.ui.SimpleManagedLayout;
import com.vaadin.shared.EventId;
import com.vaadin.shared.Registration;
import com.vaadin.shared.communication.FieldRpc.FocusAndBlurServerRpc;
import com.vaadin.shared.data.DataCommunicatorConstants;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

@Connect(ComboBoxMultiselect.class)
public class ComboBoxMultiselectConnector extends AbstractListingConnector
		implements HasRequiredIndicator, HasDataSource, SimpleManagedLayout, HasErrorIndicator {

	private ComboBoxMultiselectServerRpc rpc = getRpcProxy(ComboBoxMultiselectServerRpc.class);

	private FocusAndBlurServerRpc focusAndBlurRpc = getRpcProxy(FocusAndBlurServerRpc.class);

	private Registration dataChangeHandlerRegistration;

	@Override
	protected void init() {
		super.init();
		getWidget().connector = this;
	}

	@Override
	public void onStateChanged(StateChangeEvent stateChangeEvent) {
		super.onStateChanged(stateChangeEvent);

		Profiler.enter("ComboBoxMultiselectConnector.onStateChanged update content");

		getWidget().readonly = isReadOnly();
		getWidget().updateReadOnly();

		// not a FocusWidget -> needs own tabindex handling
		getWidget().tb.setTabIndex(getState().tabIndex);

		getWidget().suggestionPopup.updateStyleNames(getState());

		// make sure the input prompt is updated
		getWidget().updatePlaceholder();

		getDataReceivedHandler().serverReplyHandled();

		// all updates except options have been done
		getWidget().initDone = true;

		Profiler.leave("ComboBoxMultiselectConnector.onStateChanged update content");
	}

	@OnStateChange({ "selectedItemKeys", "selectedItemsCaption" })
	private void onSelectionChange() {
		getDataReceivedHandler().updateSelectionFromServer(	getState().selectedItemKeys,
															getState().selectedItemsCaption);
	}

	@Override
	public VComboBoxMultiselect getWidget() {
		return (VComboBoxMultiselect) super.getWidget();
	}

	private DataReceivedHandler getDataReceivedHandler() {
		return getWidget().getDataReceivedHandler();
	}

	@Override
	public ComboBoxMultiselectState getState() {
		return (ComboBoxMultiselectState) super.getState();
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

	/*
	 * These methods exist to move communications out of VComboBoxMultiselect,
	 * and may be refactored/removed in the future
	 */

	/**
	 * Send a message about a newly created item to the server.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 * @param itemValue
	 *            user entered string value for the new item
	 */
	public void sendNewItem(String itemValue) {
		this.rpc.createNewItem(itemValue);
		getDataReceivedHandler().clearPendingNavigation();
	}

	/**
	 * Send a message to the server set the current filter.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 * @param filter
	 *            the current filter string
	 */
	protected void setFilter(String filter) {
		if (!Objects.equals(filter, getWidget().lastFilter)) {
			getDataReceivedHandler().clearPendingNavigation();

			this.rpc.setFilter(filter);
		}
	}

	/**
	 * Send a message to the server to request a page of items with the current
	 * filter.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 * @param page
	 *            the page number to get or -1 to let the server/connector
	 *            decide based on current selection (possibly loading more data
	 *            from the server)
	 * @param filter
	 *            the filter to apply, never {@code null}
	 */
	public void requestPage(int page, String filter) {
		setFilter(filter);

		if (page < 0) {
			if (getState().scrollToSelectedItem) {
				// TODO this should be optimized not to try to fetch everything
				getDataSource().ensureAvailability(0, getDataSource().size());
				return;
			} else {
				page = 0;
			}
		}
		int startIndex = Math.max(0, page * getWidget().pageLength);
		int pageLength = getWidget().pageLength > 0 ? getWidget().pageLength : getDataSource().size();
		getDataSource().ensureAvailability(startIndex, pageLength);
	}

	/**
	 * Send a message to the server updating the current selection.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 * @param addedItemKeys
	 *            the item keys added to selection
	 * @param removedItemKeys
	 *            the item keys removed from selection
	 */
	public void sendSelections(Set<String> addedItemKeys, Set<String> removedItemKeys) {
		this.rpc.updateSelection(addedItemKeys, removedItemKeys, false);
		getDataReceivedHandler().clearPendingNavigation();
	}

	/**
	 * Notify the server that the combo box received focus.
	 *
	 * For timing reasons, ConnectorFocusAndBlurHandler is not used at the
	 * moment.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 */
	public void sendFocusEvent() {
		boolean registeredListeners = hasEventListener(EventId.FOCUS);
		if (registeredListeners) {
			this.focusAndBlurRpc.focus();
			getDataReceivedHandler().clearPendingNavigation();
		}
	}

	/**
	 * Notify the server that the combo box lost focus.
	 *
	 * For timing reasons, ConnectorFocusAndBlurHandler is not used at the
	 * moment.
	 *
	 * This method is for internal use only and may be removed in future
	 * versions.
	 *
	 * @since 8.0
	 */
	public void sendBlurEvent() {
		boolean registeredListeners = hasEventListener(EventId.BLUR);
		if (registeredListeners) {
			this.focusAndBlurRpc.blur();
			getDataReceivedHandler().clearPendingNavigation();
		}

		getDataReceivedHandler().setBlurUpdate(true);
		this.rpc.blur();
	}

	@Override
	public void setDataSource(DataSource<JsonObject> dataSource) {
		super.setDataSource(dataSource);
		this.dataChangeHandlerRegistration = dataSource.addDataChangeHandler(new PagedDataChangeHandler(dataSource));
	}

	@Override
	public void onUnregister() {
		super.onUnregister();
		this.dataChangeHandlerRegistration.remove();
	}

	@Override
	public boolean isRequiredIndicatorVisible() {
		return getState().required && !isReadOnly();
	}

	private void refreshData() {
		updateCurrentPage();

		int start = getWidget().currentPage * getWidget().pageLength;
		int end = getWidget().pageLength > 0 ? start + getWidget().pageLength : getDataSource().size();

		getWidget().currentSuggestions.clear();

		updateSuggestions(start, end);
		getWidget().setTotalSuggestions(getDataSource().size());

		getDataReceivedHandler().dataReceived();
	}

	private void updateSuggestions(int start, int end) {
		for (int i = start; i < end; ++i) {
			JsonObject row = getDataSource().getRow(i);
			if (row != null) {
				String key = getRowKey(row);
				String caption = row.getString(DataCommunicatorConstants.NAME);
				String style = row.getString(ComboBoxMultiselectConstants.STYLE);
				String untranslatedIconUri = row.getString(ComboBoxMultiselectConstants.ICON);
				ComboBoxMultiselectSuggestion suggestion = getWidget().new ComboBoxMultiselectSuggestion(key, caption,
						style, untranslatedIconUri);
				getWidget().currentSuggestions.add(suggestion);
			} else {
				// there is not enough options to fill the page
				return;
			}
		}
	}

	private boolean isFirstPage() {
		return getWidget().currentPage == 0;
	}

	private void updateCurrentPage() {
		// try to find selected item if requested
		if (getState().scrollToSelectedItem && getState().pageLength > 0 && getWidget().currentPage < 0
				&& getWidget().selectedOptionKeys != null) {
			// search for the item with the selected key
			getWidget().currentPage = 0;
			for (int i = 0; i < getDataSource().size(); ++i) {
				JsonObject row = getDataSource().getRow(i);
				if (row != null) {
					String key = getRowKey(row);
					if (getWidget().selectedOptionKeys.contains(key)) {
						getWidget().currentPage = i / getState().pageLength;
						break;
					}
				}
			}
		} else if (getWidget().currentPage < 0) {
			getWidget().currentPage = 0;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ComboBoxMultiselectConnector.class.getName());

	private class PagedDataChangeHandler implements DataChangeHandler {

		private final DataSource<?> dataSource;

		public PagedDataChangeHandler(DataSource<?> dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public void dataUpdated(int firstRowIndex, int numberOfRows) {
			// NOOP since dataAvailable is always triggered afterwards
		}

		@Override
		public void dataRemoved(int firstRowIndex, int numberOfRows) {
			// NOOP since dataAvailable is always triggered afterwards
		}

		@Override
		public void dataAdded(int firstRowIndex, int numberOfRows) {
			// NOOP since dataAvailable is always triggered afterwards
		}

		@Override
		public void dataAvailable(int firstRowIndex, int numberOfRows) {
			refreshData();
		}

		@Override
		public void resetDataAndSize(int estimatedNewDataSize) {
			if (getState().pageLength == 0) {
				if (getWidget().suggestionPopup.isShowing()) {
					this.dataSource.ensureAvailability(0, estimatedNewDataSize);
				}
				// else lets just wait till the popup is opened before
				// everything is fetched to it. this could be optimized later on
				// to fetch everything if in-memory data is used.
			} else {
				this.dataSource.ensureAvailability(0, getState().pageLength);
			}
		}

	}

	public void selectAll(String filter) {
		this.rpc.selectAll(filter);
	}

	public void clear(String filter) {
		this.rpc.clear(filter);
	}
}
