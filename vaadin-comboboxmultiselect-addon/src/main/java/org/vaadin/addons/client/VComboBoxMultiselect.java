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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.aria.client.CheckedValue;
import com.google.gwt.aria.client.Property;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.State;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ComputedStyle;
import com.vaadin.client.DeferredWorker;
import com.vaadin.client.Focusable;
import com.vaadin.client.VConsole;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.ui.Field;
import com.vaadin.client.ui.Icon;
import com.vaadin.client.ui.SubPartAware;
import com.vaadin.client.ui.VCheckBox;
import com.vaadin.client.ui.VLazyExecutor;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.client.ui.aria.AriaHelper;
import com.vaadin.client.ui.aria.HandlesAriaCaption;
import com.vaadin.client.ui.aria.HandlesAriaInvalid;
import com.vaadin.client.ui.aria.HandlesAriaRequired;
import com.vaadin.client.ui.menubar.MenuBar;
import com.vaadin.client.ui.menubar.MenuItem;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.shared.util.SharedUtil;

/**
 * Client side implementation of the ComboBoxMultiselect component.
 *
 * TODO needs major refactoring (to be extensible etc)
 *
 * @since 8.0
 */
@SuppressWarnings("deprecation")
public class VComboBoxMultiselect extends Composite
		implements Field, KeyDownHandler, KeyUpHandler, ClickHandler, FocusHandler, BlurHandler, Focusable,
		SubPartAware, HandlesAriaCaption, HandlesAriaInvalid, HandlesAriaRequired, DeferredWorker, MouseDownHandler {

	/**
	 * Represents a suggestion in the suggestion popup box.
	 */
	public class ComboBoxMultiselectSuggestion implements Suggestion, Command {

		private final String key;
		private final String caption;
		private String untranslatedIconUri;
		private String style;
		private final VCheckBox checkBox;

		/**
		 * Constructor for a single suggestion.
		 *
		 * @param key
		 *            item key, empty string for a special null item not in
		 *            container
		 * @param caption
		 *            item caption
		 * @param style
		 *            item style name, can be empty string
		 * @param untranslatedIconUri
		 *            icon URI or null
		 */
		public ComboBoxMultiselectSuggestion(String key, String caption, String style, String untranslatedIconUri) {
			this.key = key;
			this.caption = caption;
			this.style = style;
			this.untranslatedIconUri = untranslatedIconUri;

			this.checkBox = new VCheckBox();
			this.checkBox.setEnabled(false);
			State.HIDDEN.set(getCheckBoxElement(), true);
		}

		/**
		 * Gets the visible row in the popup as a HTML string. The string
		 * contains an image tag with the rows icon (if an icon has been
		 * specified) and the caption of the item
		 */

		@Override
		public String getDisplayString() {
			final StringBuilder sb = new StringBuilder();
			ApplicationConnection client = VComboBoxMultiselect.this.connector.getConnection();
			final Icon icon = client.getIcon(client.translateVaadinUri(this.untranslatedIconUri));
			if (icon != null) {
				sb.append(icon.getElement()
					.getString());
			}
			String content;
			if ("".equals(this.caption)) {
				// Ensure that empty options use the same height as other
				// options and are not collapsed (#7506)
				content = "&nbsp;";
			} else {
				content = WidgetUtil.escapeHTML(this.caption);
			}
			sb.append("<span>" + content + "</span>");
			return sb.toString();
		}

		/**
		 * Get a string that represents this item. This is used in the text box.
		 */

		@Override
		public String getReplacementString() {
			return this.caption;
		}

		/**
		 * Get the option key which represents the item on the server side.
		 *
		 * @return The key of the item
		 */
		public String getOptionKey() {
			return this.key;
		}

		/**
		 * Get the URI of the icon. Used when constructing the displayed option.
		 *
		 * @return real (translated) icon URI or null if none
		 */
		public String getIconUri() {
			ApplicationConnection client = VComboBoxMultiselect.this.connector.getConnection();
			return client.translateVaadinUri(this.untranslatedIconUri);
		}

		/**
		 * Gets the style set for this suggestion item. Styles are typically set
		 * by a server-side. The returned style is prefixed by
		 * <code>v-filterselect-item-</code>.
		 *
		 * @since 7.5.6
		 * @return the style name to use, or <code>null</code> to not apply any
		 *         custom style.
		 */
		public String getStyle() {
			return this.style;
		}

		/**
		 * Executes a selection of this item.
		 */

		@Override
		public void execute() {
			onSuggestionSelected(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ComboBoxMultiselectSuggestion)) {
				return false;
			}
			ComboBoxMultiselectSuggestion other = (ComboBoxMultiselectSuggestion) obj;
			if (this.key == null && other.key != null || this.key != null && !this.key.equals(other.key)) {
				return false;
			}
			if (this.caption == null && other.caption != null
					|| this.caption != null && !this.caption.equals(other.caption)) {
				return false;
			}
			if (!SharedUtil.equals(this.untranslatedIconUri, other.untranslatedIconUri)) {
				return false;
			}

			return SharedUtil.equals(this.style, other.style);
		}

		public VCheckBox getCheckBox() {
			return this.checkBox;
		}

		Element getCheckBoxElement() {
			return this.checkBox.getElement()
				.getFirstChildElement();
		}

		public boolean isChecked() {
			return getCheckBox().getValue();
		}

		public void setChecked(boolean checked) {
			MenuItem menuItem = VComboBoxMultiselect.this.suggestionPopup.getMenuItem(this);
			if (menuItem != null) {
				State.CHECKED.set(menuItem.getElement(), CheckedValue.of(checked));
			}

			getCheckBox().setValue(checked);
		}
	}

	/** An inner class that handles all logic related to mouse wheel. */
	private class MouseWheeler {

		/**
		 * A JavaScript function that handles the mousewheel DOM event, and
		 * passes it on to Java code.
		 *
		 * @see #createMousewheelListenerFunction(Widget)
		 */
		protected final JavaScriptObject mousewheelListenerFunction;

		protected MouseWheeler() {
			this.mousewheelListenerFunction = createMousewheelListenerFunction(VComboBoxMultiselect.this);
		}

		protected native JavaScriptObject createMousewheelListenerFunction(Widget widget)
		/*-{
		    return $entry(function(e) {
		        var deltaX = e.deltaX ? e.deltaX : -0.5*e.wheelDeltaX;
		        var deltaY = e.deltaY ? e.deltaY : -0.5*e.wheelDeltaY;
		
		        // IE8 has only delta y
		        if (isNaN(deltaY)) {
		            deltaY = -0.5*e.wheelDelta;
		        }
		
		        @org.vaadin.addons.client.VComboBoxMultiselect.JsniUtil::moveScrollFromEvent(*)(widget, deltaX, deltaY, e, e.deltaMode);
		    });
		}-*/;

		public void attachMousewheelListener(Element element) {
			attachMousewheelListenerNative(element, this.mousewheelListenerFunction);
		}

		public native void attachMousewheelListenerNative(Element element, JavaScriptObject mousewheelListenerFunction)
		/*-{
		    if (element.addEventListener) {
		        // FireFox likes "wheel", while others use "mousewheel"
		        var eventName = 'onmousewheel' in element ? 'mousewheel' : 'wheel';
		        element.addEventListener(eventName, mousewheelListenerFunction);
		    }
		}-*/;

		public void detachMousewheelListener(Element element) {
			detachMousewheelListenerNative(element, this.mousewheelListenerFunction);
		}

		public native void detachMousewheelListenerNative(Element element, JavaScriptObject mousewheelListenerFunction)
		/*-{
		    if (element.addEventListener) {
		        // FireFox likes "wheel", while others use "mousewheel"
		        var eventName = element.onwheel===undefined?"mousewheel":"wheel";
		        element.removeEventListener(eventName, mousewheelListenerFunction);
		    }
		}-*/;

	}

	/**
	 * A utility class that contains utility methods that are usually called
	 * from JSNI.
	 * <p>
	 * The methods are moved in this class to minimize the amount of JSNI code
	 * as much as feasible.
	 */
	static class JsniUtil {
		private JsniUtil() {
		}

		private static final int DOM_DELTA_PIXEL = 0;
		private static final int DOM_DELTA_LINE = 1;
		private static final int DOM_DELTA_PAGE = 2;

		// Rough estimation of item height
		private static final int SCROLL_UNIT_PX = 25;

		private static double deltaSum = 0;

		public static void moveScrollFromEvent(final Widget widget, final double deltaX, final double deltaY,
				final NativeEvent event, final int deltaMode) {
			if (!Double.isNaN(deltaY)) {
				VComboBoxMultiselect filterSelect = (VComboBoxMultiselect) widget;

				switch (deltaMode) {
				case DOM_DELTA_LINE:
					if (deltaY >= 0) {
						filterSelect.suggestionPopup.selectNextItem();
					} else {
						filterSelect.suggestionPopup.selectPrevItem();
					}
					break;
				case DOM_DELTA_PAGE:
					if (deltaY >= 0) {
						filterSelect.selectNextPage();
					} else {
						filterSelect.selectPrevPage();
					}
					break;
				case DOM_DELTA_PIXEL:
				default:
					// Accumulate dampened deltas
					deltaSum += Math.pow(Math.abs(deltaY), 0.7) * Math.signum(deltaY);

					// "Scroll" if change exceeds item height
					while (Math.abs(deltaSum) >= SCROLL_UNIT_PX) {
						if (!filterSelect.dataReceivedHandler.isWaitingForFilteringResponse()) {
							// Move selection if page flip is not in progress
							if (deltaSum < 0) {
								filterSelect.suggestionPopup.selectPrevItem();
							} else {
								filterSelect.suggestionPopup.selectNextItem();
							}
						}
						deltaSum -= SCROLL_UNIT_PX * Math.signum(deltaSum);
					}
					break;
				}
			}
		}
	}

	/**
	 * Represents the popup box with the selection options. Wraps a suggestion
	 * menu.
	 */
	public class SuggestionPopup extends VOverlay implements PositionCallback, CloseHandler<PopupPanel> {

		private static final int Z_INDEX = 30000;

		/** For internal use only. May be removed or replaced in the future. */
		public final SuggestionMenu menu;

		private final Element up = DOM.createDiv();
		private final Element down = DOM.createDiv();
		private final Element status = DOM.createDiv();

		private boolean isPagingEnabled = true;

		private long lastAutoClosed;

		private int popupOuterPadding = -1;

		private int topPosition;
		private int leftPosition;

		private final MouseWheeler mouseWheeler = new MouseWheeler();

		private boolean scrollPending = false;

		/**
		 * Default constructor
		 */
		SuggestionPopup() {
			super(true, false);
			debug("VComboBoxMultiselect.SP: constructor()");
			setOwner(VComboBoxMultiselect.this);
			this.menu = new SuggestionMenu();
			setWidget(this.menu);

			getElement().getStyle()
				.setZIndex(Z_INDEX);

			final Element root = getContainerElement();

			this.up.setInnerHTML("<span>Prev</span>");
			DOM.sinkEvents(this.up, Event.ONCLICK);

			this.down.setInnerHTML("<span>Next</span>");
			DOM.sinkEvents(this.down, Event.ONCLICK);

			root.insertFirst(this.up);
			root.appendChild(this.down);
			root.appendChild(this.status);

			DOM.sinkEvents(root, Event.ONMOUSEDOWN | Event.ONMOUSEWHEEL);
			addCloseHandler(this);

			Roles.getListRole()
				.set(getElement());

			setPreviewingAllNativeEvents(true);
		}

		public MenuItem getMenuItem(Command command) {
			for (MenuItem menuItem : this.menu.getItems()) {
				if (command.equals(menuItem.getCommand())) {
					return menuItem;
				}
			}
			return null;
		}

		@Override
		protected void onLoad() {
			super.onLoad();

			// Register mousewheel listener on paged select
			if (VComboBoxMultiselect.this.pageLength > 0) {
				this.mouseWheeler.attachMousewheelListener(getElement());
			}
		}

		@Override
		protected void onUnload() {
			this.mouseWheeler.detachMousewheelListener(getElement());
			super.onUnload();
		}

		/**
		 * Shows the popup where the user can see the filtered options that have
		 * been set with a call to
		 * {@link SuggestionMenu#setSuggestions(Collection)}.
		 *
		 * @param currentPage
		 *            The current page number
		 */
		public void showSuggestions(final int currentPage) {

			debug("VComboBoxMultiselect.SP: showSuggestions(" + currentPage + ", " + getTotalSuggestions() + ")");

			final SuggestionPopup popup = this;
			// Add TT anchor point
			getElement().setId("VAADIN_COMBOBOX_OPTIONLIST");

			this.leftPosition = getDesiredLeftPosition();
			this.topPosition = getDesiredTopPosition();

			setPopupPosition(this.leftPosition, this.topPosition);

			final int first = currentPage * VComboBoxMultiselect.this.pageLength + 1;
			final int last = first + VComboBoxMultiselect.this.currentSuggestions.size() - 1;
			final int matches = getTotalSuggestions();
			if (last > 0) {
				// nullsel not counted, as requested by user
				this.status.setInnerText((matches == 0 ? 0 : first) + "-" + last + "/" + matches);
			} else {
				this.status.setInnerText("");
			}
			// We don't need to show arrows or statusbar if there is
			// only one page
			if (matches <= VComboBoxMultiselect.this.pageLength || VComboBoxMultiselect.this.pageLength == 0) {
				setPagingEnabled(false);
			} else {
				setPagingEnabled(true);
			}
			setPrevButtonActive(first > 1);
			setNextButtonActive(last < matches);

			// clear previously fixed width
			this.menu.setWidth("");
			this.menu.getElement()
				.getFirstChildElement()
				.getStyle()
				.clearWidth();

			setPopupPositionAndShow(popup);
		}

		private int getDesiredTopPosition() {
			return toInt32(WidgetUtil.getBoundingClientRect(VComboBoxMultiselect.this.tb.getElement())
				.getBottom()) + Window.getScrollTop();
		}

		private int getDesiredLeftPosition() {
			return toInt32(WidgetUtil.getBoundingClientRect(VComboBoxMultiselect.this.getElement())
				.getLeft());
		}

		private native int toInt32(double val)
		/*-{
		    return val | 0;
		}-*/;

		/**
		 * Should the next page button be visible to the user?
		 *
		 * @param active
		 */
		private void setNextButtonActive(boolean active) {
			debug("VComboBoxMultiselect.SP: setNextButtonActive(" + active + ")");

			if (active) {
				DOM.sinkEvents(this.down, Event.ONCLICK);
				this.down.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-nextpage");
			} else {
				DOM.sinkEvents(this.down, 0);
				this.down.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-nextpage-off");
			}
		}

		/**
		 * Should the previous page button be visible to the user
		 *
		 * @param active
		 */
		private void setPrevButtonActive(boolean active) {
			debug("VComboBoxMultiselect.SP: setPrevButtonActive(" + active + ")");

			if (active) {
				DOM.sinkEvents(this.up, Event.ONCLICK);
				this.up.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-prevpage");
			} else {
				DOM.sinkEvents(this.up, 0);
				this.up.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-prevpage-off");
			}

		}

		/**
		 * Selects the next item in the filtered selections.
		 */
		public void selectNextItem() {
			debug("VComboBoxMultiselect.SP: selectNextItem()");

			final int index = this.menu.getSelectedIndex() + 1;
			if (this.menu.getItems()
				.size() > index) {
				selectItem(this.menu.getItems()
					.get(index));

			} else {
				selectNextPage();
			}
		}

		/**
		 * Selects the previous item in the filtered selections.
		 */
		public void selectPrevItem() {
			debug("VComboBoxMultiselect.SP: selectPrevItem()");

			final int index = this.menu.getSelectedIndex() - 1;
			if (index > -1) {
				selectItem(this.menu.getItems()
					.get(index));

			} else if (index == -1) {
				selectPrevPage();

			} else {
				if (!this.menu.getItems()
					.isEmpty()) {
					selectLastItem();
				}
			}
		}

		/**
		 * Select the first item of the suggestions list popup.
		 *
		 * @since 7.2.6
		 */
		public void selectFirstItem() {
			debug("VFS.SP: selectFirstItem()");
			int index = 0;
			if (this.menu.getItems() != null && !this.menu.getItems()
				.isEmpty()
					&& this.menu.getItems()
						.size() > 1) {
				if (VComboBoxMultiselect.this.showClearButton && VComboBoxMultiselect.this.showSelectAllButton) {
					index = 2;
				} else if (VComboBoxMultiselect.this.showClearButton || VComboBoxMultiselect.this.showSelectAllButton) {
					index = 1;
				}
			}
			selectItem(getFirstNotSelectedItem(index));
		}

		/**
		 * returns first not checked item, if all are checked first item will be
		 * returned
		 * 
		 * @param mi
		 */
		private MenuItem getFirstNotSelectedItem(int index) {
			MenuItem found = getFirstNotSelectedItemRecursive(index);
			return found == null ? this.menu.getItems()
				.get(index) : found;
		}

		private MenuItem getFirstNotSelectedItemRecursive(int index) {
			if (index >= this.menu.getItems()
				.size()) {
				return null;
			}

			MenuItem mi = this.menu.getItems()
				.get(index);

			if (mi == null) {
				return null;
			}

			ComboBoxMultiselectSuggestion suggestion = (ComboBoxMultiselectSuggestion) mi.getCommand();

			if (suggestion.isChecked()) {
				return getFirstNotSelectedItemRecursive(index + 1);
			}
			return mi;
		}

		/**
		 * Select the last item of the suggestions list popup.
		 *
		 * @since 7.2.6
		 */
		public void selectLastItem() {
			debug("VComboBoxMultiselect.SP: selectLastItem()");
			selectItem(this.menu.getLastItem());
		}

		/*
		 * Sets the selected item in the popup menu.
		 */
		private void selectItem(final MenuItem newSelectedItem) {
			this.menu.selectItem(newSelectedItem);
		}

		/**
		 * Selects the item at the given index
		 * 
		 * @param index
		 *            item at index to select
		 */
		public void selectItemAtIndex(int index) {
			if (index == -1) {
				return;
			}
			if (VComboBoxMultiselect.this.showSelectAllButton) {
				index++;
			}
			if (VComboBoxMultiselect.this.showClearButton) {
				index++;
			}
			selectItem(this.menu.getItems()
				.get(index));
		}

		/*
		 * Using a timer to scroll up or down the pages so when we receive lots
		 * of consecutive mouse wheel events the pages does not flicker.
		 */
		private LazyPageScroller lazyPageScroller = new LazyPageScroller();

		private class LazyPageScroller extends Timer {
			private int pagesToScroll = 0;

			@Override
			public void run() {
				debug("VComboBoxMultiselect.SP.LPS: run()");
				if (this.pagesToScroll != 0) {
					if (!VComboBoxMultiselect.this.dataReceivedHandler.isWaitingForFilteringResponse()) {
						/*
						 * Avoid scrolling while we are waiting for a response
						 * because otherwise the waiting flag will be reset in
						 * the first response and the second response will be
						 * ignored, causing an empty popup...
						 *
						 * As long as the scrolling delay is suitable
						 * double/triple clicks will work by scrolling two or
						 * three pages at a time and this should not be a
						 * problem.
						 */
						// this makes sure that we don't close the popup
						VComboBoxMultiselect.this.dataReceivedHandler.setNavigationCallback(() -> {
						});
						filterOptions(	VComboBoxMultiselect.this.currentPage + this.pagesToScroll,
										VComboBoxMultiselect.this.lastFilter);
					}
					this.pagesToScroll = 0;
				}
			}

			public void scrollUp() {
				debug("VComboBoxMultiselect.SP.LPS: scrollUp()");
				if (VComboBoxMultiselect.this.pageLength > 0
						&& VComboBoxMultiselect.this.currentPage + this.pagesToScroll > 0) {
					this.pagesToScroll--;
					cancel();
					schedule(200);
				}
			}

			public void scrollDown() {
				debug("VComboBoxMultiselect.SP.LPS: scrollDown()");
				if (VComboBoxMultiselect.this.pageLength > 0
						&& getTotalSuggestions() > (VComboBoxMultiselect.this.currentPage + this.pagesToScroll + 1)
								* VComboBoxMultiselect.this.pageLength) {
					this.pagesToScroll++;
					cancel();
					schedule(200);
				}
			}
		}

		private void scroll(double deltaY) {
			boolean scrollActive = this.menu.isScrollActive();

			debug("VComboBoxMultiselect.SP: scroll() scrollActive: " + scrollActive);

			if (!scrollActive) {
				if (deltaY > 0d) {
					this.lazyPageScroller.scrollDown();
				} else {
					this.lazyPageScroller.scrollUp();
				}
			}
		}

		@Override
		public void onBrowserEvent(Event event) {
			debug("VComboBoxMultiselect.SP: onBrowserEvent()");

			if (event.getTypeInt() == Event.ONCLICK) {
				final Element target = DOM.eventGetTarget(event);
				if (target == this.up || target == DOM.getChild(this.up, 0)) {
					this.lazyPageScroller.scrollUp();
				} else if (target == this.down || target == DOM.getChild(this.down, 0)) {
					this.lazyPageScroller.scrollDown();
				}

			}

			/*
			 * Prevent the keyboard focus from leaving the textfield by
			 * preventing the default behaviour of the browser. Fixes #4285.
			 */
			handleMouseDownEvent(event);
		}

		@Override
		protected void onPreviewNativeEvent(NativePreviewEvent event) {
			// Check all events outside the combobox to see if they scroll the
			// page. We cannot use e.g. Window.addScrollListener() because the
			// scrolled element can be at any level on the page.

			// Normally this is only called when the popup is showing, but make
			// sure we don't accidentally process all events when not showing.
			if (!this.scrollPending && isShowing()
					&& !DOM.isOrHasChild(SuggestionPopup.this.getElement(), Element.as(event.getNativeEvent()
						.getEventTarget()))) {
				if (getDesiredLeftPosition() != this.leftPosition || getDesiredTopPosition() != this.topPosition) {
					updatePopupPositionOnScroll();
				}
			}

			super.onPreviewNativeEvent(event);
		}

		/**
		 * Make the popup follow the position of the ComboBoxMultiselect when
		 * the page is scrolled.
		 */
		private void updatePopupPositionOnScroll() {
			if (!this.scrollPending) {
				AnimationScheduler.get()
					.requestAnimationFrame(timestamp -> {
						if (isShowing()) {
							this.leftPosition = getDesiredLeftPosition();
							this.topPosition = getDesiredTopPosition();
							setPopupPosition(this.leftPosition, this.topPosition);
						}
						this.scrollPending = false;
					});
				this.scrollPending = true;
			}
		}

		/**
		 * Should paging be enabled. If paging is enabled then only a certain
		 * amount of items are visible at a time and a scrollbar or buttons are
		 * visible to change page. If paging is turned of then all options are
		 * rendered into the popup menu.
		 *
		 * @param paging
		 *            Should the paging be turned on?
		 */
		public void setPagingEnabled(boolean paging) {
			debug("VComboBoxMultiselect.SP: setPagingEnabled(" + paging + ")");
			if (this.isPagingEnabled == paging) {
				return;
			}
			if (paging) {
				this.down.getStyle()
					.clearDisplay();
				this.up.getStyle()
					.clearDisplay();
				this.status.getStyle()
					.clearDisplay();
			} else {
				this.down.getStyle()
					.setDisplay(Display.NONE);
				this.up.getStyle()
					.setDisplay(Display.NONE);
				this.status.getStyle()
					.setDisplay(Display.NONE);
			}
			this.isPagingEnabled = paging;
		}

		@Override
		public void setPosition(int offsetWidth, int offsetHeight) {
			debug("VComboBoxMultiselect.SP: setPosition(" + offsetWidth + ", " + offsetHeight + ")");

			int top = this.topPosition;
			int left = getPopupLeft();

			// reset menu size and retrieve its "natural" size
			this.menu.setHeight("");
			if (VComboBoxMultiselect.this.currentPage > 0 && !hasNextPage()) {
				// fix height to avoid height change when getting to last page
				this.menu.fixHeightTo(VComboBoxMultiselect.this.pageLength);
			}

			// ignoring the parameter as in V7
			offsetHeight = getOffsetHeight();
			final int desiredHeight = offsetHeight;
			final int desiredWidth = getMainWidth();

			debug("VComboBoxMultiselect.SP:     desired[" + desiredWidth + ", " + desiredHeight + "]");

			Element menuFirstChild = this.menu.getElement()
				.getFirstChildElement();
			int naturalMenuWidth;
			if (BrowserInfo.get()
				.isIE()
					&& BrowserInfo.get()
						.getBrowserMajorVersion() < 10) {
				// On IE 8 & 9 visibility is set to hidden and measuring
				// elements while they are hidden yields incorrect results
				String before = this.menu.getElement()
					.getParentElement()
					.getStyle()
					.getVisibility();
				this.menu.getElement()
					.getParentElement()
					.getStyle()
					.setVisibility(Visibility.VISIBLE);
				naturalMenuWidth = WidgetUtil.getRequiredWidth(menuFirstChild);
				this.menu.getElement()
					.getParentElement()
					.getStyle()
					.setProperty("visibility", before);
			} else {
				naturalMenuWidth = WidgetUtil.getRequiredWidth(menuFirstChild);
			}

			if (this.popupOuterPadding == -1) {
				this.popupOuterPadding = WidgetUtil.measureHorizontalPaddingAndBorder(this.menu.getElement(), 2)
						+ WidgetUtil
							.measureHorizontalPaddingAndBorder(	VComboBoxMultiselect.this.suggestionPopup.getElement(),
																0);
			}

			updateMenuWidth(desiredWidth, naturalMenuWidth);

			if (BrowserInfo.get()
				.isIE()
					&& BrowserInfo.get()
						.getBrowserMajorVersion() < 11) {
				// Must take margin,border,padding manually into account for
				// menu element as we measure the element child and set width to
				// the element parent

				double naturalMenuOuterWidth;
				if (BrowserInfo.get()
					.getBrowserMajorVersion() < 10) {
					// On IE 8 & 9 visibility is set to hidden and measuring
					// elements while they are hidden yields incorrect results
					String before = this.menu.getElement()
						.getParentElement()
						.getStyle()
						.getVisibility();
					this.menu.getElement()
						.getParentElement()
						.getStyle()
						.setVisibility(Visibility.VISIBLE);
					naturalMenuOuterWidth = WidgetUtil.getRequiredWidthDouble(menuFirstChild)
							+ getMarginBorderPaddingWidth(this.menu.getElement());
					this.menu.getElement()
						.getParentElement()
						.getStyle()
						.setProperty("visibility", before);
				} else {
					naturalMenuOuterWidth = WidgetUtil.getRequiredWidthDouble(menuFirstChild)
							+ getMarginBorderPaddingWidth(this.menu.getElement());
				}

				/*
				 * IE requires us to specify the width for the container
				 * element. Otherwise it will be 100% wide
				 */
				double rootWidth = Math.max(desiredWidth - this.popupOuterPadding, naturalMenuOuterWidth);
				getContainerElement().getStyle()
					.setWidth(rootWidth, Unit.PX);
			}

			final int textInputHeight = VComboBoxMultiselect.this.getOffsetHeight();
			final int textInputTopOnPage = VComboBoxMultiselect.this.tb.getAbsoluteTop();
			final int viewportOffset = Document.get()
				.getScrollTop();
			final int textInputTopInViewport = textInputTopOnPage - viewportOffset;
			final int textInputBottomInViewport = textInputTopInViewport + textInputHeight;

			final int spaceAboveInViewport = textInputTopInViewport;
			final int spaceBelowInViewport = Window.getClientHeight() - textInputBottomInViewport;

			if (spaceBelowInViewport < offsetHeight && spaceBelowInViewport < spaceAboveInViewport) {
				// popup on top of input instead
				if (offsetHeight > spaceAboveInViewport) {
					// Shrink popup height to fit above
					offsetHeight = spaceAboveInViewport;
				}
				top = textInputTopOnPage - offsetHeight;
			} else {
				// Show below, position calculated in showSuggestions for some
				// strange reason
				top = this.topPosition;
				offsetHeight = Math.min(offsetHeight, spaceBelowInViewport);
			}

			// fetch real width (mac FF bugs here due GWT popups overflow:auto )
			offsetWidth = menuFirstChild.getOffsetWidth();

			if (offsetHeight < desiredHeight) {
				int menuHeight = offsetHeight;
				if (this.isPagingEnabled) {
					menuHeight -= this.up.getOffsetHeight() + this.down.getOffsetHeight()
							+ this.status.getOffsetHeight();
				} else {
					final ComputedStyle s = new ComputedStyle(this.menu.getElement());
					menuHeight -= s.getIntProperty("marginBottom") + s.getIntProperty("marginTop");
				}

				// If the available page height is really tiny then this will be
				// negative and an exception will be thrown on setHeight.
				int menuElementHeight = this.menu.getItemOffsetHeight();
				if (menuHeight < menuElementHeight) {
					menuHeight = menuElementHeight;
				}

				this.menu.setHeight(menuHeight + "px");

				if (VComboBoxMultiselect.this.suggestionPopupWidth == null) {
					final int naturalMenuWidthPlusScrollBar = naturalMenuWidth + WidgetUtil.getNativeScrollbarSize();
					if (offsetWidth < naturalMenuWidthPlusScrollBar) {
						this.menu.setWidth(naturalMenuWidthPlusScrollBar + "px");
					}
				}
			}

			if (offsetWidth + left > Window.getClientWidth()) {
				left = VComboBoxMultiselect.this.getAbsoluteLeft() + VComboBoxMultiselect.this.getOffsetWidth()
						- offsetWidth;
				if (left < 0) {
					left = 0;
					this.menu.setWidth(Window.getClientWidth() + "px");

				}
			}

			setPopupPosition(left, top);
			this.menu.scrollSelectionIntoView();
		}

		/**
		 * Adds in-line CSS rules to the DOM according to the
		 * suggestionPopupWidth field
		 *
		 * @param desiredWidth
		 * @param naturalMenuWidth
		 */
		private void updateMenuWidth(final int desiredWidth, int naturalMenuWidth) {
			/**
			 * Three different width modes for the suggestion pop-up:
			 *
			 * 1. Legacy "null"-mode: width is determined by the longest item
			 * caption for each page while still maintaining minimum width of
			 * (desiredWidth - popupOuterPadding)
			 *
			 * 2. relative to the component itself
			 *
			 * 3. fixed width
			 */
			String width = "auto";
			if (VComboBoxMultiselect.this.suggestionPopupWidth == null) {
				if (naturalMenuWidth < desiredWidth) {
					naturalMenuWidth = desiredWidth - this.popupOuterPadding;
					width = desiredWidth - this.popupOuterPadding + "px";
				}
			} else if (isrelativeUnits(VComboBoxMultiselect.this.suggestionPopupWidth)) {
				float mainComponentWidth = desiredWidth - this.popupOuterPadding;
				// convert percentage value to fraction
				int widthInPx = Math
					.round(mainComponentWidth * asFraction(VComboBoxMultiselect.this.suggestionPopupWidth));
				width = widthInPx + "px";
			} else {
				// use as fixed width CSS definition
				width = WidgetUtil.escapeAttribute(VComboBoxMultiselect.this.suggestionPopupWidth);
			}
			this.menu.setWidth(width);
		}

		/**
		 * Returns the percentage value as a fraction, e.g. 42% -> 0.42
		 *
		 * @param percentage
		 */
		private float asFraction(String percentage) {
			String trimmed = percentage.trim();
			String withoutPercentSign = trimmed.substring(0, trimmed.length() - 1);
			float asFraction = Float.parseFloat(withoutPercentSign) / 100;
			return asFraction;
		}

		/**
		 * @since 7.7
		 * @param suggestionPopupWidth
		 * @return
		 */
		private boolean isrelativeUnits(String suggestionPopupWidth) {
			return suggestionPopupWidth.trim()
				.endsWith("%");
		}

		/**
		 * Was the popup just closed?
		 *
		 * @return true if popup was just closed
		 */
		public boolean isJustClosed() {
			debug("VComboBoxMultiselect.SP: justClosed()");
			final long now = new Date().getTime();
			return this.lastAutoClosed > 0 && now - this.lastAutoClosed < 200;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * com.google.gwt.event.logical.shared.CloseHandler#onClose(com.google
		 * .gwt.event.logical.shared.CloseEvent)
		 */

		@Override
		public void onClose(CloseEvent<PopupPanel> event) {
			debug("VComboBoxMultiselect.SP: onClose(" + event.isAutoClosed() + ")");

			if (event.isAutoClosed()) {
				this.lastAutoClosed = new Date().getTime();
			}
		}

		/**
		 * Updates style names in suggestion popup to help theme building.
		 *
		 * @param componentState
		 *            shared state of the combo box
		 */
		public void updateStyleNames(AbstractComponentState componentState) {
			debug("VComboBoxMultiselect.SP: updateStyleNames()");
			setStyleName(VComboBoxMultiselect.this.getStylePrimaryName() + "-suggestpopup");
			this.menu.setStyleName(VComboBoxMultiselect.this.getStylePrimaryName() + "-suggestmenu");
			this.status.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-status");
			if (ComponentStateUtil.hasStyles(componentState)) {
				for (String style : componentState.styles) {
					if (!"".equals(style)) {
						addStyleDependentName(style);
					}
				}
			}
		}

	}

	/**
	 * The menu where the suggestions are rendered
	 */
	public class SuggestionMenu extends MenuBar implements SubPartAware, LoadHandler {

		private VLazyExecutor delayedImageLoadExecutioner = new VLazyExecutor(100, new ScheduledCommand() {

			@Override
			public void execute() {
				debug("VComboBoxMultiselect.SM: delayedImageLoadExecutioner()");
				if (VComboBoxMultiselect.this.suggestionPopup.isVisible()
						&& VComboBoxMultiselect.this.suggestionPopup.isAttached()) {
					setWidth("");
					getElement().getFirstChildElement()
						.getStyle()
						.clearWidth();
					VComboBoxMultiselect.this.suggestionPopup
						.setPopupPositionAndShow(VComboBoxMultiselect.this.suggestionPopup);
				}

			}
		});

		/**
		 * Default constructor
		 */
		SuggestionMenu() {
			super(true);
			debug("VComboBoxMultiselect.SM: constructor()");
			addDomHandler(this, LoadEvent.getType());

			setScrollEnabled(true);
		}

		/**
		 * Fixes menus height to use same space as full page would use. Needed
		 * to avoid height changes when quickly "scrolling" to last page.
		 * 
		 * @param pageItemsCount
		 *            height items count
		 */
		public void fixHeightTo(int pageItemsCount) {
			setHeight(getPreferredHeight(pageItemsCount));
		}

		/*
		 * Gets the preferred height of the menu including pageItemsCount items.
		 */
		String getPreferredHeight(int pageItemsCount) {
			if (VComboBoxMultiselect.this.currentSuggestions.size() > 0) {
				final int pixels = getPreferredHeight() / VComboBoxMultiselect.this.currentSuggestions.size()
						* pageItemsCount;
				return pixels + "px";
			} else {
				return "";
			}
		}

		/**
		 * Sets the suggestions rendered in the menu.
		 *
		 * @param suggestions
		 *            The suggestions to be rendered in the menu
		 */
		public void setSuggestions(Collection<ComboBoxMultiselectSuggestion> suggestions) {
			debug("VComboBoxMultiselect.SM: setSuggestions(" + suggestions + ")");

			clearItems();

			if (VComboBoxMultiselect.this.showClearButton) {
				MenuItem clearMenuItem = new MenuItem(VComboBoxMultiselect.this.clearButtonCaption, false,
						VComboBoxMultiselect.this.clearCmd);
				clearMenuItem.getElement()
					.setId(DOM.createUniqueId());
				clearMenuItem.addStyleName("align-center");
				Property.LABEL.set(clearMenuItem.getElement(), VComboBoxMultiselect.this.clearButtonCaption);
				this.addItem(clearMenuItem);
			}

			if (VComboBoxMultiselect.this.showSelectAllButton) {
				MenuItem selectAllMenuItem = new MenuItem(VComboBoxMultiselect.this.selectAllButtonCaption, false,
						VComboBoxMultiselect.this.selectAllCmd);
				selectAllMenuItem.getElement()
					.setId(DOM.createUniqueId());
				selectAllMenuItem.addStyleName("align-center");
				Property.LABEL.set(selectAllMenuItem.getElement(), VComboBoxMultiselect.this.selectAllButtonCaption);
				this.addItem(selectAllMenuItem);
			}

			final Iterator<ComboBoxMultiselectSuggestion> it = suggestions.iterator();
			// TODO thacht remove
			boolean isFirstIteration = true;
			MenuItem firstElement = null;
			while (it.hasNext()) {
				final ComboBoxMultiselectSuggestion suggestion = it.next();
				final MenuItem mi = new MenuItem(suggestion.getDisplayString(), true, suggestion);
				if (isFirstIteration) {
					firstElement = mi;
					isFirstIteration = false;
				}
				String style = suggestion.getStyle();
				if (style != null) {
					mi.addStyleName("v-filterselect-item-" + style);
				}
				Roles.getListitemRole()
					.set(mi.getElement());

				WidgetUtil.sinkOnloadForImages(mi.getElement());

				boolean isSelected = VComboBoxMultiselect.this.selectedOptionKeys != null
						&& VComboBoxMultiselect.this.selectedOptionKeys.contains(suggestion.getOptionKey());
				suggestion.setChecked(isSelected);
				mi.getElement()
					.insertFirst(suggestion.getCheckBox()
						.getElement());
				// TODO thacht
				// Property.LABEL.set(mi.getElement(), s.getAriaLabel());
				// Property.SETSIZE.set(mi.getElement(), numberOfSuggestions);
				// Property.POSINSET.set(mi.getElement(),
				// currentSuggestionIndex);
				// State.CHECKED.set(mi.getElement(),
				// CheckedValue.of(isSelected));

				this.addItem(mi);

				// By default, first item on the list is always highlighted,
				// unless adding new items is allowed.
				// TODO thacht
				// if (isFirstIteration &&
				// !VComboBoxMultiselect.this.allowNewItems) {
				// selectItem(mi);
				// }

				// if (VComboBoxMultiselect.this.currentSuggestion != null &&
				// suggestion.getOptionKey()
				// .equals(VComboBoxMultiselect.this.currentSuggestion.getOptionKey()))
				// {
				// // Refresh also selected caption and icon in case they have
				// // been updated on the server, e.g. just the item has been
				// // updated, but selection (from state) has stayed the same.
				// // FIXME need to update selected item caption separately, if
				// // the selected item is not in "active data range" that is
				// // being sent to the client. Then this can be removed.
				// if
				// (VComboBoxMultiselect.this.currentSuggestion.getReplacementString()
				// .equals(VComboBoxMultiselect.this.tb.getText())) {
				// VComboBoxMultiselect.this.currentSuggestion = suggestion;
				// selectItem(mi);
				// setSelectedCaption(VComboBoxMultiselect.this.currentSuggestion.getReplacementString());
				// }
				// }
			}
			VComboBoxMultiselect.this.suggestionPopup.selectFirstItem();
		}

		/**
		 * Create/select a suggestion based on the used entered string. This
		 * method is called after filtering has completed with the given string.
		 *
		 * @param enteredItemValue
		 *            user entered string
		 */
		public void actOnEnteredValueAfterFiltering(String enteredItemValue) {
			debug("VComboBoxMultiselect.SM: doPostFilterSelectedItemAction()");
			final MenuItem item = getSelectedItem();

			// check for exact match in menu
			int p = getItems().size();
			if (p > 0) {
				for (int i = 0; i < p; i++) {
					final MenuItem potentialExactMatch = getItems().get(i);
					if (potentialExactMatch.getText()
						.equals(enteredItemValue)) {
						selectItem(potentialExactMatch);
						// do not send a value change event if null was and
						// stays selected
						if (!"".equals(enteredItemValue) || VComboBoxMultiselect.this.selectedOptionKeys != null
								&& !VComboBoxMultiselect.this.selectedOptionKeys.isEmpty()) {
							doItemAction(potentialExactMatch, true);
						}
						return;
					}
				}
			}
			if (VComboBoxMultiselect.this.allowNewItems) {
				if (!enteredItemValue.equals(VComboBoxMultiselect.this.lastNewItemString)) {
					// Store last sent new item string to avoid double sends
					VComboBoxMultiselect.this.lastNewItemString = enteredItemValue;
					VComboBoxMultiselect.this.connector.sendNewItem(enteredItemValue);
					// TODO try to select the new value if it matches what was
					// sent for V7 compatibility
				}
			} else if (item != null && !"".equals(VComboBoxMultiselect.this.lastFilter) && item.getText()
				.toLowerCase()
				.contains(VComboBoxMultiselect.this.lastFilter.toLowerCase())) {
				doItemAction(item, true);
			} else {
				// currentSuggestion has key="" for nullselection
				if (VComboBoxMultiselect.this.currentSuggestion != null
						&& !"".equals(VComboBoxMultiselect.this.currentSuggestion.key)) {
					// An item (not null) selected
					String text = VComboBoxMultiselect.this.currentSuggestion.getReplacementString();
					setText(text);
					VComboBoxMultiselect.this.selectedOptionKeys.add(VComboBoxMultiselect.this.currentSuggestion.key);
				}
			}
		}

		private static final String SUBPART_PREFIX = "item";

		@Override
		public com.google.gwt.user.client.Element getSubPartElement(String subPart) {
			int index = Integer.parseInt(subPart.substring(SUBPART_PREFIX.length()));

			MenuItem item = getItems().get(index);

			return item.getElement();
		}

		@Override
		public String getSubPartName(com.google.gwt.user.client.Element subElement) {
			if (!getElement().isOrHasChild(subElement)) {
				return null;
			}

			Element menuItemRoot = subElement;
			while (menuItemRoot != null && !menuItemRoot.getTagName()
				.equalsIgnoreCase("td")) {
				menuItemRoot = menuItemRoot.getParentElement()
					.cast();
			}
			// "menuItemRoot" is now the root of the menu item

			final int itemCount = getItems().size();
			for (int i = 0; i < itemCount; i++) {
				if (getItems().get(i)
					.getElement() == menuItemRoot) {
					String name = SUBPART_PREFIX + i;
					return name;
				}
			}
			return null;
		}

		@Override
		public void onLoad(LoadEvent event) {
			debug("VComboBoxMultiselect.SM: onLoad()");
			// Handle icon onload events to ensure shadow is resized
			// correctly
			this.delayedImageLoadExecutioner.trigger();

		}

		/**
		 * @deprecated use {@link SuggestionPopup#selectFirstItem()} instead.
		 */
		@Deprecated
		public void selectFirstItem() {
			debug("VComboBoxMultiselect.SM: selectFirstItem()");
			MenuItem firstItem = getItems().get(0);
			selectItem(firstItem);
		}

		/**
		 * @deprecated use {@link SuggestionPopup#selectLastItem()} instead.
		 */
		@Deprecated
		public void selectLastItem() {
			debug("VComboBoxMultiselect.SM: selectLastItem()");
			List<MenuItem> items = getItems();
			MenuItem lastItem = items.get(items.size() - 1);
			selectItem(lastItem);
		}

		/*
		 * Gets the height of one menu item.
		 */
		int getItemOffsetHeight() {
			List<MenuItem> items = getItems();
			return items != null && items.size() > 0 ? items.get(0)
				.getOffsetHeight() : 0;
		}

		/*
		 * Gets the width of one menu item.
		 */
		int getItemOffsetWidth() {
			List<MenuItem> items = getItems();
			return items != null && items.size() > 0 ? items.get(0)
				.getOffsetWidth() : 0;
		}

		/**
		 * Returns true if the scroll is active on the menu element or if the
		 * menu currently displays the last page with less items then the
		 * maximum visibility (in which case the scroll is not active, but the
		 * scroll is active for any other page in general).
		 *
		 * @since 7.2.6
		 */
		@Override
		public boolean isScrollActive() {
			String height = getElement().getStyle()
				.getHeight();
			String preferredHeight = getPreferredHeight(VComboBoxMultiselect.this.pageLength);

			return !(height == null || height.length() == 0 || height.equals(preferredHeight));
		}

		/**
		 * Highlight (select) an item matching the current text box content
		 * without triggering its action.
		 */
		public void highlightSelectedItem() {
			int p = getItems().size();
			// first check if there is a key match to handle items with
			// identical captions
			String currentKey = VComboBoxMultiselect.this.currentSuggestion != null
					? VComboBoxMultiselect.this.currentSuggestion.getOptionKey() : "";
			for (int i = 0; i < p; i++) {
				final MenuItem potentialExactMatch = getItems().get(i);
				if (currentKey.equals(getSuggestionKey(potentialExactMatch)) && VComboBoxMultiselect.this.tb.getText()
					.equals(potentialExactMatch.getText())) {
					selectItem(potentialExactMatch);
					VComboBoxMultiselect.this.tb.setSelectionRange(VComboBoxMultiselect.this.tb.getText()
						.length(), 0);
					return;
				}
			}
			// then check for exact string match in menu
			String text = VComboBoxMultiselect.this.tb.getText();
			for (int i = 0; i < p; i++) {
				final MenuItem potentialExactMatch = getItems().get(i);
				if (potentialExactMatch.getText()
					.equals(text)) {
					selectItem(potentialExactMatch);
					VComboBoxMultiselect.this.tb.setSelectionRange(VComboBoxMultiselect.this.tb.getText()
						.length(), 0);
					return;
				}
			}
		}
	}

	private String getSuggestionKey(MenuItem item) {
		if (item != null && item.getCommand() != null && item.getCommand() instanceof ComboBoxMultiselectSuggestion) {
			return ((ComboBoxMultiselectSuggestion) item.getCommand()).getOptionKey();
		}
		return "";
	}

	/**
	 * TextBox variant used as input element for filter selects, which prevents
	 * selecting text when disabled.
	 *
	 * @since 7.1.5
	 */
	public class FilterSelectTextBox extends TextBox {

		/**
		 * Creates a new filter select text box.
		 *
		 * @since 7.6.4
		 */
		public FilterSelectTextBox() {
			/*-
			 * Stop the browser from showing its own suggestion popup.
			 *
			 * Using an invalid value instead of "off" as suggested by
			 * https://developer.mozilla.org/en-US/docs/Web/Security/Securing_your_site/Turning_off_form_autocompletion
			 *
			 * Leaving the non-standard Safari options autocapitalize and
			 * autocorrect untouched since those do not interfere in the same
			 * way, and they might be useful in a combo box where new items are
			 * allowed.
			 */
			getElement().setAttribute("autocomplete", "nope");
		}

		/**
		 * Overridden to avoid selecting text when text input is disabled
		 */
		@Override
		public void setSelectionRange(int pos, int length) {
			if (VComboBoxMultiselect.this.textInputEnabled) {
				/*
				 * set selection range with a backwards direction: anchor at the
				 * back, focus at the front. This means that items that are too
				 * long to display will display from the start and not the end
				 * even on Firefox.
				 *
				 * We need the JSNI function to set selection range so that we
				 * can use the optional direction attribute to set the anchor to
				 * the end and the focus to the start. This makes Firefox work
				 * the same way as other browsers (#13477)
				 */
				WidgetUtil.setSelectionRange(getElement(), pos, length, "backward");

			} else {
				/*
				 * Setting the selectionrange for an uneditable textbox leads to
				 * unwanted behaviour when the width of the textbox is narrower
				 * than the width of the entry: the end of the entry is shown
				 * instead of the beginning. (see #13477)
				 *
				 * To avoid this, we set the caret to the beginning of the line.
				 */

				super.setSelectionRange(0, 0);
			}
		}

	}

	/**
	 * Handler receiving notifications from the connector and updating the
	 * widget state accordingly.
	 *
	 * This class is still subject to change and should not be considered as
	 * public stable API.
	 *
	 * @since 8.0
	 */
	public class DataReceivedHandler {

		private Runnable navigationCallback = null;
		/**
		 * Set true when popupopened has been clicked. Cleared on each
		 * UIDL-update. This handles the special case where are not filtering
		 * yet and the selected value has changed on the server-side. See #2119
		 * <p>
		 * For internal use only. May be removed or replaced in the future.
		 */
		private boolean popupOpenerClicked = false;
		/** For internal use only. May be removed or replaced in the future. */
		private boolean waitingForFilteringResponse = false;
		private boolean initialData = true;
		private String pendingUserInput = null;
		private boolean showPopup = false;
		private boolean blurUpdate = false;

		/**
		 * Called by the connector when new data for the last requested filter
		 * is received from the server.
		 */
		public void dataReceived() {
			if (this.initialData || this.blurUpdate) {
				VComboBoxMultiselect.this.suggestionPopup.menu
					.setSuggestions(VComboBoxMultiselect.this.currentSuggestions);
				performSelection(VComboBoxMultiselect.this.serverSelectedKeys, true, true);
				updateSuggestionPopupMinWidth();
				updateRootWidth();
				this.initialData = false;
				return;
			}

			VComboBoxMultiselect.this.suggestionPopup.menu.setSuggestions(VComboBoxMultiselect.this.currentSuggestions);
			if (!this.waitingForFilteringResponse && VComboBoxMultiselect.this.suggestionPopup.isAttached()) {
				this.showPopup = true;
			}
			if (this.showPopup) {
				VComboBoxMultiselect.this.suggestionPopup.showSuggestions(VComboBoxMultiselect.this.currentPage);
				if (VComboBoxMultiselect.this.currentSuggestion != null
						&& VComboBoxMultiselect.this.currentSuggestions != null) {

					VComboBoxMultiselect.this.suggestionPopup
						.selectItemAtIndex(VComboBoxMultiselect.this.currentSuggestions
							.indexOf(VComboBoxMultiselect.this.currentSuggestion));
					// TODO thacht how does this work?
					// VComboBoxMultiselect.this.suggestionPopup.selectFirstItem();
				}
			}

			this.waitingForFilteringResponse = false;

			if (this.pendingUserInput != null) {
				VComboBoxMultiselect.this.suggestionPopup.menu.actOnEnteredValueAfterFiltering(this.pendingUserInput);
				this.pendingUserInput = null;
			} else if (this.popupOpenerClicked) {
				// make sure the current item is selected in the popup
				VComboBoxMultiselect.this.suggestionPopup.menu.highlightSelectedItem();
			} else {
				navigateItemAfterPageChange();
			}

			this.popupOpenerClicked = false;
		}

		/**
		 * Perform filtering with the user entered string and when the results
		 * are received, perform any action appropriate for the user input
		 * (select an item or create a new one).
		 *
		 * @param value
		 *            user input
		 */
		public void reactOnInputWhenReady(String value) {
			this.pendingUserInput = value;
			this.showPopup = false;
			filterOptions(0, value);
		}

		/*
		 * This method navigates to the proper item in the combobox page. This
		 * should be executed after setSuggestions() method which is called from
		 * VComboBoxMultiselect.showSuggestions(). ShowSuggestions() method
		 * builds the page content. As far as setSuggestions() method is called
		 * as deferred, navigateItemAfterPageChange method should be also be
		 * called as deferred. #11333
		 */
		private void navigateItemAfterPageChange() {
			if (this.navigationCallback != null) {
				// navigationCallback is not reset here but after any server
				// request in case you are in between two requests both changing
				// the page back and forth

				// we're paging w/ arrows
				this.navigationCallback.run();
				this.navigationCallback = null;
			}
		}

		/**
		 * Called by the connector any pending navigation operations should be
		 * cleared.
		 */
		public void clearPendingNavigation() {
			this.navigationCallback = null;
		}

		/**
		 * Set a callback that is invoked when a page change occurs if there
		 * have not been intervening requests to the server. The callback is
		 * reset when any additional request is made to the server.
		 *
		 * @param callback
		 *            method to call after filtering has completed
		 */
		public void setNavigationCallback(Runnable callback) {
			this.showPopup = true;
			this.navigationCallback = callback;
		}

		/**
		 * Record that the popup opener has been clicked and the popup should be
		 * opened on the next request.
		 *
		 * This handles the special case where are not filtering yet and the
		 * selected value has changed on the server-side. See #2119. The flag is
		 * cleared on each server reply.
		 */
		public void popupOpenerClicked() {
			this.popupOpenerClicked = true;
			this.showPopup = true;
		}

		/**
		 * Cancel a pending request to perform post-filtering actions.
		 */
		private void cancelPendingPostFiltering() {
			this.pendingUserInput = null;
		}

		/**
		 * Called by the connector when it has finished handling any reply from
		 * the server, regardless of what was updated.
		 */
		public void serverReplyHandled() {
			this.popupOpenerClicked = false;
			VComboBoxMultiselect.this.lastNewItemString = null;

			// if (!initDone) {
			// debug("VComboBoxMultiselect: init done, updating widths");
			// // Calculate minimum textarea width
			// updateSuggestionPopupMinWidth();
			// updateRootWidth();
			// initDone = true;
			// }
		}

		/**
		 * For internal use only - this method will be removed in the future.
		 *
		 * @return true if the combo box is waiting for a reply from the server
		 *         with a new page of data, false otherwise
		 */
		public boolean isWaitingForFilteringResponse() {
			return this.waitingForFilteringResponse;
		}

		/**
		 * For internal use only - this method will be removed in the future.
		 *
		 * @return true if the combo box is waiting for initial data from the
		 *         server, false otherwise
		 */
		public boolean isWaitingForInitialData() {
			return this.initialData;
		}

		/**
		 * Set a flag that filtering of options is pending a response from the
		 * server.
		 */
		private void startWaitingForFilteringResponse() {
			this.waitingForFilteringResponse = true;
		}

		/**
		 * Perform selection (if appropriate) based on a reply from the server.
		 * When this method is called, the suggestions have been reset if new
		 * ones (different from the previous list) were received from the
		 * server.
		 *
		 * @param selectedKeys
		 *            new selected keys or null if none given by the server
		 * @param selectedCaption
		 *            new selected item caption if sent by the server or null -
		 *            this is used when the selected item is not on the current
		 *            page
		 */
		public void updateSelectionFromServer(Set<String> selectedKeys, String selectedCaption) {
			boolean oldSuggestionTextMatchTheOldSelection = VComboBoxMultiselect.this.currentSuggestion != null
					&& VComboBoxMultiselect.this.currentSuggestion.getReplacementString()
						.equals(VComboBoxMultiselect.this.tb.getText());

			VComboBoxMultiselect.this.serverSelectedKeys = selectedKeys;

			performSelection(	selectedKeys, oldSuggestionTextMatchTheOldSelection,
								!isWaitingForFilteringResponse() || this.popupOpenerClicked);

			cancelPendingPostFiltering();

			if (!VComboBoxMultiselect.this.suggestionPopup.isShowing()) {
				setSelectedCaption(selectedCaption);
			}
		}

		public void setBlurUpdate(boolean blurUpdate) {
			this.blurUpdate = blurUpdate;
		}

	}

	// TODO decide whether this should change - affects themes and v7
	public static final String CLASSNAME = "v-filterselect";
	private static final String STYLE_NO_INPUT = "no-input";

	/** For internal use only. May be removed or replaced in the future. */
	public int pageLength = 10;

	/** For internal use only. May be removed or replaced in the future. */
	public String clearButtonCaption = "clear";

	/** For internal use only. May be removed or replaced in the future. */
	public boolean showClearButton;

	/** For internal use only. May be removed or replaced in the future. */
	public String selectAllButtonCaption = "select all";

	/** For internal use only. May be removed or replaced in the future. */
	public boolean showSelectAllButton;

	/** For internal use only. May be removed or replaced in the future. */
	Command clearCmd = new Command() {

		@Override
		public void execute() {
			debug("VFS: clearCmd()");

			String filter = VComboBoxMultiselect.this.tb.getText();
			VComboBoxMultiselect.this.connector.clear(filter);

			setText("");
			filterOptions(0, "");
		}
	};

	/** For internal use only. May be removed or replaced in the future. */
	Command selectAllCmd = new Command() {

		@Override
		public void execute() {
			debug("VFS: selectAllCmd()");
			String filter = VComboBoxMultiselect.this.tb.getText();
			VComboBoxMultiselect.this.connector.selectAll(filter);

			setText("");
			filterOptions(0, "");
		}
	};

	private boolean enableDebug = false;

	private final FlowPanel panel = new FlowPanel();

	/**
	 * The text box where the filter is written
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public final TextBox tb;

	/** For internal use only. May be removed or replaced in the future. */
	public final SuggestionPopup suggestionPopup;

	/**
	 * Used when measuring the width of the popup
	 */
	private final HTML popupOpener = new HTML("");

	private class IconWidget extends Widget {
		IconWidget(Icon icon) {
			setElement(icon.getElement());
		}
	}

	private IconWidget selectedItemIcon;

	/** For internal use only. May be removed or replaced in the future. */
	public ComboBoxMultiselectConnector connector;

	/** For internal use only. May be removed or replaced in the future. */
	public int currentPage;

	/**
	 * A collection of available suggestions (options) as received from the
	 * server.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public final List<ComboBoxMultiselectSuggestion> currentSuggestions = new ArrayList<>();

	/** For internal use only. May be removed or replaced in the future. */
	public Set<String> serverSelectedKeys;
	/** For internal use only. May be removed or replaced in the future. */
	public Set<String> selectedOptionKeys;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean initDone = false;

	/** For internal use only. May be removed or replaced in the future. */
	public String lastFilter = "";

	/**
	 * The current suggestion selected from the dropdown. This is one of the
	 * values in currentSuggestions except when filtering, in this case
	 * currentSuggestion might not be in currentSuggestions.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public ComboBoxMultiselectSuggestion currentSuggestion;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean allowNewItems;

	/** Total number of suggestions, excluding null selection item. */
	private int totalSuggestions;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean enabled;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean readonly;

	/** For internal use only. May be removed or replaced in the future. */
	public String inputPrompt = "";

	/** For internal use only. May be removed or replaced in the future. */
	public int suggestionPopupMinWidth = 0;

	public String suggestionPopupWidth = null;

	private int popupWidth = -1;
	/**
	 * Stores the last new item string to avoid double submissions. Cleared on
	 * uidl updates.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public String lastNewItemString;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean focused = false;

	/**
	 * If set to false, the component should not allow entering text to the
	 * field even for filtering.
	 */
	private boolean textInputEnabled = true;

	private final DataReceivedHandler dataReceivedHandler = new DataReceivedHandler();

	/**
	 * Default constructor.
	 */
	public VComboBoxMultiselect() {
		this.tb = createTextBox();
		this.suggestionPopup = createSuggestionPopup();

		this.popupOpener.addMouseDownHandler(VComboBoxMultiselect.this);
		Roles.getButtonRole()
			.setAriaHiddenState(this.popupOpener.getElement(), true);
		Roles.getButtonRole()
			.set(this.popupOpener.getElement());

		this.panel.add(this.tb);
		this.panel.add(this.popupOpener);
		initWidget(this.panel);
		Roles.getComboboxRole()
			.set(this.panel.getElement());

		this.tb.addKeyDownHandler(this);
		this.tb.addKeyUpHandler(this);

		this.tb.addFocusHandler(this);
		this.tb.addBlurHandler(this);

		this.panel.addDomHandler(this, ClickEvent.getType());

		setStyleName(CLASSNAME);

		sinkEvents(Event.ONPASTE);
	}

	private static double getMarginBorderPaddingWidth(Element element) {
		final ComputedStyle s = new ComputedStyle(element);
		return s.getMarginWidth() + s.getBorderWidth() + s.getPaddingWidth();

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.google.gwt.user.client.ui.Composite#onBrowserEvent(com.google.gwt
	 * .user.client.Event)
	 */
	@Override
	public void onBrowserEvent(Event event) {
		super.onBrowserEvent(event);

		if (event.getTypeInt() == Event.ONPASTE) {
			if (this.textInputEnabled) {
				filterOptions(this.currentPage);
			}
		}
	}

	/**
	 * This method will create the TextBox used by the VComboBoxMultiselect
	 * instance. It is invoked during the Constructor and should only be
	 * overridden if a custom TextBox shall be used. The overriding method
	 * cannot use any instance variables.
	 *
	 * @since 7.1.5
	 * @return TextBox instance used by this VComboBoxMultiselect
	 */
	protected TextBox createTextBox() {
		return new FilterSelectTextBox();
	}

	/**
	 * This method will create the SuggestionPopup used by the
	 * VComboBoxMultiselect instance. It is invoked during the Constructor and
	 * should only be overridden if a custom SuggestionPopup shall be used. The
	 * overriding method cannot use any instance variables.
	 *
	 * @since 7.1.5
	 * @return SuggestionPopup instance used by this VComboBoxMultiselect
	 */
	protected SuggestionPopup createSuggestionPopup() {
		return new SuggestionPopup();
	}

	@Override
	public void setStyleName(String style) {
		super.setStyleName(style);
		updateStyleNames();
	}

	@Override
	public void setStylePrimaryName(String style) {
		super.setStylePrimaryName(style);
		updateStyleNames();
	}

	protected void updateStyleNames() {
		this.tb.setStyleName(getStylePrimaryName() + "-input");
		this.popupOpener.setStyleName(getStylePrimaryName() + "-button");
		this.suggestionPopup.setStyleName(getStylePrimaryName() + "-suggestpopup");
	}

	/**
	 * Does the Select have more pages?
	 *
	 * @return true if a next page exists, else false if the current page is the
	 *         last page
	 */
	public boolean hasNextPage() {
		return this.pageLength > 0 && getTotalSuggestions() > (this.currentPage + 1) * this.pageLength;
	}

	/**
	 * Filters the options at a certain page. Uses the text box input as a
	 * filter and ensures the popup is opened when filtering results are
	 * available.
	 *
	 * @param page
	 *            The page which items are to be filtered
	 */
	public void filterOptions(int page) {
		this.dataReceivedHandler.popupOpenerClicked();
		filterOptions(page, this.tb.getText());
	}

	/**
	 * Filters the options at certain page using the given filter.
	 *
	 * @param page
	 *            The page to filter
	 * @param filter
	 *            The filter to apply to the components
	 */
	public void filterOptions(int page, String filter) {
		debug("VComboBoxMultiselect: filterOptions(" + page + ", " + filter + ")");

		if (filter.equals(this.lastFilter) && this.currentPage == page && this.suggestionPopup.isAttached()) {
			// already have the page
			this.dataReceivedHandler.dataReceived();
			return;
		}

		if (!filter.equals(this.lastFilter)) {
			// when filtering, let the server decide the page unless we've
			// set the filter to empty and explicitly said that we want to see
			// the results starting from page 0.
			if ("".equals(filter) && page != 0) {
				// let server decide
				page = -1;
			} else {
				page = 0;
			}
		}

		this.dataReceivedHandler.startWaitingForFilteringResponse();
		this.connector.requestPage(page, filter);

		this.lastFilter = filter;

		// If the data was updated from cache, the page has been updated too, if
		// not, update
		if (this.dataReceivedHandler.isWaitingForFilteringResponse()) {
			this.currentPage = page;
		}
	}

	/** For internal use only. May be removed or replaced in the future. */
	public void updateReadOnly() {
		debug("VComboBoxMultiselect: updateReadOnly()");
		this.tb.setReadOnly(this.readonly || !this.textInputEnabled);
	}

	public void setTextInputAllowed(boolean textInputAllowed) {
		debug("VComboBoxMultiselect: setTextInputAllowed()");
		// Always update styles as they might have been overwritten
		if (textInputAllowed) {
			removeStyleDependentName(STYLE_NO_INPUT);
			Roles.getTextboxRole()
				.removeAriaReadonlyProperty(this.tb.getElement());
		} else {
			addStyleDependentName(STYLE_NO_INPUT);
			Roles.getTextboxRole()
				.setAriaReadonlyProperty(this.tb.getElement(), true);
		}

		if (this.textInputEnabled == textInputAllowed) {
			return;
		}

		this.textInputEnabled = textInputAllowed;
		updateReadOnly();
	}

	/**
	 * Sets the text in the text box.
	 *
	 * @param text
	 *            the text to set in the text box
	 */
	public void setText(final String text) {
		/**
		 * To leave caret in the beginning of the line. SetSelectionRange
		 * wouldn't work on IE (see #13477)
		 */
		Direction previousDirection = this.tb.getDirection();
		this.tb.setDirection(Direction.RTL);
		this.tb.setText(text);
		this.tb.setDirection(previousDirection);
	}

	/**
	 * Set or reset the placeholder attribute for the text field.
	 *
	 * @param placeholder
	 *            new placeholder string or null for none
	 */
	public void setPlaceholder(String placeholder) {
		this.inputPrompt = placeholder;
		updatePlaceholder();
	}

	/**
	 * Update placeholder visibility (hidden when read-only or disabled).
	 */
	public void updatePlaceholder() {
		if (this.inputPrompt != null && this.enabled && !this.readonly) {
			this.tb.getElement()
				.setAttribute("placeholder", this.inputPrompt);
		} else {
			this.tb.getElement()
				.removeAttribute("placeholder");
		}
	}

	/**
	 * Triggered when a suggestion is selected.
	 *
	 * @param suggestion
	 *            The suggestion that just got selected.
	 */
	public void onSuggestionSelected(ComboBoxMultiselectSuggestion suggestion) {
		debug("VComboBoxMultiselect: onSuggestionSelected(" + suggestion.caption + ": " + suggestion.key + ")");

		this.dataReceivedHandler.cancelPendingPostFiltering();

		this.currentSuggestion = suggestion;
		String newKey = suggestion.getOptionKey();

		if (!this.selectedOptionKeys.contains(newKey)) {
			this.selectedOptionKeys.add(newKey);
			this.connector.sendSelections(new HashSet<>(Arrays.asList(newKey)), new HashSet<>());
		} else {
			this.selectedOptionKeys.remove(newKey);
			this.connector.sendSelections(new HashSet<>(), new HashSet<>(Arrays.asList(newKey)));
		}
	}

	/**
	 * Perform selection based on a message from the server.
	 *
	 * The special case where the selected item is not on the current page is
	 * handled separately by the caller.
	 *
	 * @param selectedKeys
	 *            non-empty selected item keys
	 * @param forceUpdateText
	 *            true to force the text box value to match the suggestion text
	 * @param updatePromptAndSelectionIfMatchFound
	 */
	private void performSelection(Set<String> selectedKeys, boolean forceUpdateText,
			boolean updatePromptAndSelectionIfMatchFound) {
		// some item selected

		if (this.selectedOptionKeys == null) {
			this.selectedOptionKeys = new LinkedHashSet<>();
		}

		for (ComboBoxMultiselectSuggestion suggestion : this.currentSuggestions) {
			String suggestionKey = suggestion.getOptionKey();
			if (selectedKeys == null || !selectedKeys.contains(suggestionKey)) {
				continue;
			}
			// at this point, suggestion key matches the new selection key
			if (updatePromptAndSelectionIfMatchFound && !this.selectedOptionKeys.contains(suggestionKey)
					|| suggestion.getReplacementString()
						.equals(this.tb.getText())
					|| forceUpdateText) {
				this.selectedOptionKeys.add(suggestionKey);
			}
		}
	}

	private void forceReflow() {
		WidgetUtil.setStyleTemporarily(this.tb.getElement(), "zoom", "1");
	}

	/**
	 * Positions the icon vertically in the middle. Should be called after the
	 * icon has loaded
	 */
	private void updateSelectedIconPosition() {
		// Position icon vertically to middle
		int availableHeight = 0;
		availableHeight = getOffsetHeight();

		int iconHeight = WidgetUtil.getRequiredHeight(this.selectedItemIcon);
		int marginTop = (availableHeight - iconHeight) / 2;
		this.selectedItemIcon.getElement()
			.getStyle()
			.setMarginTop(marginTop, Unit.PX);
	}

	private static Set<Integer> navigationKeyCodes = new HashSet<>();
	static {
		navigationKeyCodes.add(KeyCodes.KEY_DOWN);
		navigationKeyCodes.add(KeyCodes.KEY_UP);
		navigationKeyCodes.add(KeyCodes.KEY_PAGEDOWN);
		navigationKeyCodes.add(KeyCodes.KEY_PAGEUP);
		navigationKeyCodes.add(KeyCodes.KEY_ENTER);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.google.gwt.event.dom.client.KeyDownHandler#onKeyDown(com.google.gwt
	 * .event.dom.client.KeyDownEvent)
	 */

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (this.enabled && !this.readonly) {
			int keyCode = event.getNativeKeyCode();

			debug("VComboBoxMultiselect: key down: " + keyCode);

			if (this.dataReceivedHandler.isWaitingForFilteringResponse() && navigationKeyCodes.contains(keyCode)
					&& (!this.allowNewItems || keyCode != KeyCodes.KEY_ENTER)) {
				/*
				 * Keyboard navigation events should not be handled while we are
				 * waiting for a response. This avoids flickering, disappearing
				 * items, wrongly interpreted responses and more.
				 */
				debug("Ignoring " + keyCode + " because we are waiting for a filtering response");

				DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
				event.stopPropagation();
				return;
			}

			if (this.suggestionPopup.isAttached()) {
				debug("Keycode " + keyCode + " target is popup");
				popupKeyDown(event);
			} else {
				debug("Keycode " + keyCode + " target is text field");
				inputFieldKeyDown(event);
			}
		}
	}

	private void debug(String string) {
		if (this.enableDebug) {
			VConsole.error(string);
		}
	}

	/**
	 * Triggered when a key is pressed in the text box
	 *
	 * @param event
	 *            The KeyDownEvent
	 */
	private void inputFieldKeyDown(KeyDownEvent event) {
		debug("VComboBoxMultiselect: inputFieldKeyDown(" + event.getNativeKeyCode() + ")");

		switch (event.getNativeKeyCode()) {
		case KeyCodes.KEY_DOWN:
		case KeyCodes.KEY_UP:
		case KeyCodes.KEY_PAGEDOWN:
		case KeyCodes.KEY_PAGEUP:
			// open popup as from gadget
			filterOptions(-1, "");
			this.tb.selectAll();
			this.dataReceivedHandler.popupOpenerClicked();
			break;
		case KeyCodes.KEY_ENTER:
			/*
			 * This only handles the case when new items is allowed, a text is
			 * entered, the popup opener button is clicked to close the popup
			 * and enter is then pressed (see #7560).
			 */
			if (!this.allowNewItems) {
				return;
			}

			if (this.currentSuggestion != null && this.tb.getText()
				.equals(this.currentSuggestion.getReplacementString())) {
				// Retain behavior from #6686 by returning without stopping
				// propagation if there's nothing to do
				return;
			}
			this.dataReceivedHandler.reactOnInputWhenReady(this.tb.getText());

			event.stopPropagation();
			break;
		}

	}

	/**
	 * Triggered when a key was pressed in the suggestion popup.
	 *
	 * @param event
	 *            The KeyDownEvent of the key
	 */
	private void popupKeyDown(KeyDownEvent event) {
		debug("VComboBoxMultiselect: popupKeyDown(" + event.getNativeKeyCode() + ")");

		// Propagation of handled events is stopped so other handlers such as
		// shortcut key handlers do not also handle the same events.
		switch (event.getNativeKeyCode()) {
		case KeyCodes.KEY_DOWN:
			this.suggestionPopup.selectNextItem();

			DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
			event.stopPropagation();
			break;
		case KeyCodes.KEY_UP:
			this.suggestionPopup.selectPrevItem();

			DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
			event.stopPropagation();
			break;
		case KeyCodes.KEY_PAGEDOWN:
			selectNextPage();
			event.stopPropagation();
			break;
		case KeyCodes.KEY_PAGEUP:
			selectPrevPage();
			event.stopPropagation();
			break;
		case KeyCodes.KEY_ESCAPE:
			reset();
			DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
			event.stopPropagation();
			break;
		case KeyCodes.KEY_TAB:
		case KeyCodes.KEY_ENTER:

			// queue this, may be cancelled by selection
			int selectedIndex = this.suggestionPopup.menu.getSelectedIndex();
			if (!this.allowNewItems && selectedIndex != -1) {

				debug("index before: " + selectedIndex);
				if (this.showClearButton) {
					selectedIndex = selectedIndex - 1;
				}
				if (this.showSelectAllButton) {
					selectedIndex = selectedIndex - 1;
				}

				debug("index after: " + selectedIndex);
				if (selectedIndex == -2) {
					this.clearCmd.execute();
				} else if (selectedIndex == -1) {
					if (this.showSelectAllButton) {
						this.selectAllCmd.execute();
					} else {
						this.clearCmd.execute();
					}
				}

				debug("entered suggestion: " + this.currentSuggestions.get(selectedIndex).caption);
				onSuggestionSelected(this.currentSuggestions.get(selectedIndex));
			} else {
				this.dataReceivedHandler.reactOnInputWhenReady(this.tb.getText());
			}

			event.stopPropagation();
			break;
		}
	}

	/*
	 * Show the prev page.
	 */
	private void selectPrevPage() {
		if (this.currentPage > 0) {
			this.dataReceivedHandler.setNavigationCallback(() -> this.suggestionPopup.selectLastItem());
			filterOptions(this.currentPage - 1, this.lastFilter);
		}
	}

	/*
	 * Show the next page.
	 */
	private void selectNextPage() {
		if (hasNextPage()) {
			this.dataReceivedHandler.setNavigationCallback(() -> this.suggestionPopup.selectFirstItem());
			filterOptions(this.currentPage + 1, this.lastFilter);
		}
	}

	/**
	 * Triggered when a key was depressed.
	 *
	 * @param event
	 *            The KeyUpEvent of the key depressed
	 */
	@Override
	public void onKeyUp(KeyUpEvent event) {
		debug("VComboBoxMultiselect: onKeyUp(" + event.getNativeKeyCode() + ")");

		if (this.enabled && !this.readonly) {
			switch (event.getNativeKeyCode()) {
			case KeyCodes.KEY_ENTER:
			case KeyCodes.KEY_TAB:
			case KeyCodes.KEY_SHIFT:
			case KeyCodes.KEY_CTRL:
			case KeyCodes.KEY_ALT:
			case KeyCodes.KEY_DOWN:
			case KeyCodes.KEY_UP:
			case KeyCodes.KEY_PAGEDOWN:
			case KeyCodes.KEY_PAGEUP:
			case KeyCodes.KEY_ESCAPE:
				// NOP
				break;
			default:
				if (this.textInputEnabled) {
					// when filtering, we always want to see the results on the
					// first page first.
					filterOptions(0);
				}
				break;
			}
		}
	}

	/**
	 * Resets the ComboBoxMultiselect to its initial state.
	 */
	private void reset() {
		debug("VComboBoxMultiselect: reset()");

		// just fetch selected information from state
		String text = this.connector.getState().selectedItemsCaption;
		setText(text == null ? "" : text);
		this.selectedOptionKeys = this.connector.getState().selectedItemKeys;
		if (this.selectedOptionKeys == null || this.selectedOptionKeys.isEmpty()) {
			this.selectedOptionKeys = null;
			updatePlaceholder();
		}
		this.currentSuggestion = null; // #13217
		// else {
		// this.currentSuggestion = this.currentSuggestions.stream()
		// .filter(suggestion ->
		// this.selectedOptionKeys.contains(suggestion.getOptionKey()))
		// .findAny()
		// .orElse(null);
		// }

		this.suggestionPopup.hide();
	}

	/**
	 * Listener for popupopener.
	 */
	@Override
	public void onClick(ClickEvent event) {
		debug("VComboBoxMultiselect: onClick()");
		if (this.enabled && !this.readonly) {
			getDataReceivedHandler().blurUpdate = false;
			// ask suggestionPopup if it was just closed, we are using GWT
			// Popup's auto close feature
			if (!this.suggestionPopup.isJustClosed()) {
				filterOptions(-1, "");
				this.dataReceivedHandler.popupOpenerClicked();
			}
			DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
			focus();
			setText("");
		}
	}

	/**
	 * Update minimum width for combo box textarea based on input prompt and
	 * suggestions.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public void updateSuggestionPopupMinWidth() {
		debug("VComboBoxMultiselect: updateSuggestionPopupMinWidth()");

		// used only to calculate minimum width
		String captions = WidgetUtil.escapeHTML(this.inputPrompt);

		for (ComboBoxMultiselectSuggestion suggestion : this.currentSuggestions) {
			// Collect captions so we can calculate minimum width for
			// textarea
			if (captions.length() > 0) {
				captions += "|";
			}
			captions += WidgetUtil.escapeHTML(suggestion.getReplacementString());
		}

		// Calculate minimum textarea width
		this.suggestionPopupMinWidth = minWidth(captions);
	}

	/**
	 * Calculate minimum width for FilterSelect textarea.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 *
	 * @param captions
	 *            pipe separated string listing all the captions to measure
	 * @return minimum width in pixels
	 */
	public native int minWidth(String captions)
	/*-{
	    if(!captions || captions.length <= 0)
	            return 0;
	    captions = captions.split("|");
	    var d = $wnd.document.createElement("div");
	    var html = "";
	    for(var i=0; i < captions.length; i++) {
	            html += "<div>" + captions[i] + "</div>";
	            // TODO apply same CSS classname as in suggestionmenu
	    }
	    d.style.position = "absolute";
	    d.style.top = "0";
	    d.style.left = "0";
	    d.style.visibility = "hidden";
	    d.innerHTML = html;
	    $wnd.document.body.appendChild(d);
	    var w = d.offsetWidth;
	    $wnd.document.body.removeChild(d);
	    return w;
	}-*/;

	/**
	 * A flag which prevents a focus event from taking place.
	 */
	boolean iePreventNextFocus = false;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.google.gwt.event.dom.client.FocusHandler#onFocus(com.google.gwt.event
	 * .dom.client.FocusEvent)
	 */

	@Override
	public void onFocus(FocusEvent event) {
		debug("VComboBoxMultiselect: onFocus()");

		/*
		 * When we disable a blur event in ie we need to refocus the textfield.
		 * This will cause a focus event we do not want to process, so in that
		 * case we just ignore it.
		 */
		if (BrowserInfo.get()
			.isIE() && this.iePreventNextFocus) {
			this.iePreventNextFocus = false;
			return;
		}

		this.focused = true;
		updatePlaceholder();
		addStyleDependentName("focus");

		this.connector.sendFocusEvent();

		this.connector.getConnection()
			.getVTooltip()
			.showAssistive(this.connector.getTooltipInfo(getElement()));
	}

	/**
	 * A flag which cancels the blur event and sets the focus back to the
	 * textfield if the Browser is IE.
	 */
	boolean preventNextBlurEventInIE = false;

	private String explicitSelectedCaption;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.google.gwt.event.dom.client.BlurHandler#onBlur(com.google.gwt.event
	 * .dom.client.BlurEvent)
	 */

	@Override
	public void onBlur(BlurEvent event) {
		debug("VComboBoxMultiselect: onBlur()");

		if (BrowserInfo.get()
			.isIE() && this.preventNextBlurEventInIE) {
			/*
			 * Clicking in the suggestion popup or on the popup button in IE
			 * causes a blur event to be sent for the field. In other browsers
			 * this is prevented by canceling/preventing default behavior for
			 * the focus event, in IE we handle it here by refocusing the text
			 * field and ignoring the resulting focus event for the textfield
			 * (in onFocus).
			 */
			this.preventNextBlurEventInIE = false;

			Element focusedElement = WidgetUtil.getFocusedElement();
			if (getElement().isOrHasChild(focusedElement) || this.suggestionPopup.getElement()
				.isOrHasChild(focusedElement)) {

				// IF the suggestion popup or another part of the
				// VComboBoxMultiselect
				// was focused, move the focus back to the textfield and prevent
				// the triggered focus event (in onFocus).
				this.iePreventNextFocus = true;
				this.tb.setFocus(true);
				return;
			}
		}

		this.focused = false;
		updatePlaceholder();
		removeStyleDependentName("focus");

		// Send new items when clicking out with the mouse.
		if (!this.readonly) {
			if (this.textInputEnabled && this.allowNewItems && (this.currentSuggestion == null || this.tb.getText()
				.equals(this.currentSuggestion.getReplacementString()))) {
				this.dataReceivedHandler.reactOnInputWhenReady(this.tb.getText());
			} else {
				reset();
			}
			this.suggestionPopup.hide();
		}

		this.connector.sendBlurEvent();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.vaadin.client.Focusable#focus()
	 */

	@Override
	public void focus() {
		debug("VComboBoxMultiselect: focus()");
		this.focused = true;
		updatePlaceholder();
		this.tb.setFocus(true);
	}

	/**
	 * Calculates the width of the select if the select has undefined width.
	 * Should be called when the width changes or when the icon changes.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public void updateRootWidth() {
		debug("VComboBoxMultiselect: updateRootWidth()");

		if (this.connector.isUndefinedWidth()) {

			/*
			 * When the select has a undefined with we need to check that we are
			 * only setting the text box width relative to the first page width
			 * of the items. If this is not done the text box width will change
			 * when the popup is used to view longer items than the text box is
			 * wide.
			 */
			int w = WidgetUtil.getRequiredWidth(this);

			if (this.dataReceivedHandler.isWaitingForInitialData() && this.suggestionPopupMinWidth > w) {
				/*
				 * We want to compensate for the paddings just to preserve the
				 * exact size as in Vaadin 6.x, but we get here before
				 * MeasuredSize has been initialized.
				 * Util.measureHorizontalPaddingAndBorder does not work with
				 * border-box, so we must do this the hard way.
				 */
				Style style = getElement().getStyle();
				String originalPadding = style.getPadding();
				String originalBorder = style.getBorderWidth();
				style.setPaddingLeft(0, Unit.PX);
				style.setBorderWidth(0, Unit.PX);
				style.setProperty("padding", originalPadding);
				style.setProperty("borderWidth", originalBorder);

				// Use util.getRequiredWidth instead of getOffsetWidth here

				int iconWidth = this.selectedItemIcon == null ? 0 : WidgetUtil.getRequiredWidth(this.selectedItemIcon);
				int buttonWidth = this.popupOpener == null ? 0 : WidgetUtil.getRequiredWidth(this.popupOpener);

				/*
				 * Instead of setting the width of the wrapper, set the width of
				 * the combobox. Subtract the width of the icon and the
				 * popupopener
				 */

				this.tb.setWidth(this.suggestionPopupMinWidth - iconWidth - buttonWidth + "px");
			}

			/*
			 * Lock the textbox width to its current value if it's not already
			 * locked. This can happen after setWidth("") which resets the
			 * textbox width to "100%".
			 */
			if (!this.tb.getElement()
				.getStyle()
				.getWidth()
				.endsWith("px")) {
				int iconWidth = this.selectedItemIcon == null ? 0 : this.selectedItemIcon.getOffsetWidth();
				this.tb.setWidth(this.tb.getOffsetWidth() - iconWidth + "px");
			}
		}
	}

	/**
	 * Get the width of the select in pixels where the text area and icon has
	 * been included.
	 *
	 * @return The width in pixels
	 */
	private int getMainWidth() {
		return getOffsetWidth();
	}

	@Override
	public void setWidth(String width) {
		super.setWidth(width);
		if (width.length() != 0) {
			this.tb.setWidth("100%");
		}
	}

	/**
	 * Handles special behavior of the mouse down event.
	 *
	 * @param event
	 */
	private void handleMouseDownEvent(Event event) {
		/*
		 * Prevent the keyboard focus from leaving the textfield by preventing
		 * the default behaviour of the browser. Fixes #4285.
		 */
		if (event.getTypeInt() == Event.ONMOUSEDOWN) {
			debug("VComboBoxMultiselect: blocking mouseDown event to avoid blur");

			event.preventDefault();
			event.stopPropagation();

			/*
			 * In IE the above wont work, the blur event will still trigger. So,
			 * we set a flag here to prevent the next blur event from happening.
			 * This is not needed if do not already have focus, in that case
			 * there will not be any blur event and we should not cancel the
			 * next blur.
			 */
			if (BrowserInfo.get()
				.isIE() && this.focused) {
				this.preventNextBlurEventInIE = true;
				debug("VComboBoxMultiselect: Going to prevent next blur event on IE");
			}
		}
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		debug("VComboBoxMultiselect.onMouseDown(): blocking mouseDown event to avoid blur");

		event.preventDefault();
		event.stopPropagation();

		/*
		 * In IE the above wont work, the blur event will still trigger. So, we
		 * set a flag here to prevent the next blur event from happening. This
		 * is not needed if do not already have focus, in that case there will
		 * not be any blur event and we should not cancel the next blur.
		 */
		if (BrowserInfo.get()
			.isIE() && this.focused) {
			this.preventNextBlurEventInIE = true;
			debug("VComboBoxMultiselect: Going to prevent next blur event on IE");
		}
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		this.suggestionPopup.hide();
	}

	@Override
	public com.google.gwt.user.client.Element getSubPartElement(String subPart) {
		String[] parts = subPart.split("/");
		if ("textbox".equals(parts[0])) {
			return this.tb.getElement();
		} else if ("button".equals(parts[0])) {
			return this.popupOpener.getElement();
		} else if ("popup".equals(parts[0]) && this.suggestionPopup.isAttached()) {
			if (parts.length == 2) {
				return this.suggestionPopup.menu.getSubPartElement(parts[1]);
			}
			return this.suggestionPopup.getElement();
		}
		return null;
	}

	@Override
	public String getSubPartName(com.google.gwt.user.client.Element subElement) {
		if (this.tb.getElement()
			.isOrHasChild(subElement)) {
			return "textbox";
		} else if (this.popupOpener.getElement()
			.isOrHasChild(subElement)) {
			return "button";
		} else if (this.suggestionPopup.getElement()
			.isOrHasChild(subElement)) {
			return "popup";
		}
		return null;
	}

	@Override
	public void setAriaRequired(boolean required) {
		AriaHelper.handleInputRequired(this.tb, required);
	}

	@Override
	public void setAriaInvalid(boolean invalid) {
		AriaHelper.handleInputInvalid(this.tb, invalid);
	}

	@Override
	public void bindAriaCaption(com.google.gwt.user.client.Element captionElement) {
		AriaHelper.bindCaption(this.tb, captionElement);
	}

	@Override
	public boolean isWorkPending() {
		return this.dataReceivedHandler.isWaitingForFilteringResponse()
				|| this.suggestionPopup.lazyPageScroller.isRunning();
	}

	/**
	 * Sets the caption of selected item, if "scroll to page" is disabled. This
	 * method is meant for internal use and may change in future versions.
	 *
	 * @since 7.7
	 * @param selectedCaption
	 *            the caption of selected item
	 */
	public void setSelectedCaption(String selectedCaption) {
		this.explicitSelectedCaption = selectedCaption;
		if (selectedCaption != null) {
			setText(selectedCaption);
		}
	}

	/**
	 * This method is meant for internal use and may change in future versions.
	 *
	 * @since 7.7
	 * @return the caption of selected item, if "scroll to page" is disabled
	 */
	public String getSelectedCaption() {
		return this.explicitSelectedCaption;
	}

	/**
	 * Returns a handler receiving notifications from the connector about
	 * communications.
	 *
	 * @return the dataReceivedHandler
	 */
	public DataReceivedHandler getDataReceivedHandler() {
		return this.dataReceivedHandler;
	}

	/**
	 * Sets the number of items to show per page, or 0 for showing all items.
	 *
	 * @param pageLength
	 *            new page length or 0 for all items
	 */
	public void setPageLength(int pageLength) {
		this.pageLength = pageLength;
	}

	/**
	 * Sets the caption of the clear button.
	 *
	 * @param clearButtonCaption
	 *            caption of the clear button
	 */
	public void setClearButtonCaption(String clearButtonCaption) {
		this.clearButtonCaption = clearButtonCaption;
	}

	/**
	 * Sets the caption of the selectAll button.
	 *
	 * @param selectAllButtonCaption
	 *            caption of the selectAll button
	 */
	public void setSelectAllButtonCaption(String selectAllButtonCaption) {
		this.selectAllButtonCaption = selectAllButtonCaption;
	}

	/**
	 * Sets the clear button visible.
	 * 
	 * @param showClearButton
	 *            visible
	 */
	public void setShowClearButton(boolean showClearButton) {
		this.showClearButton = showClearButton;
	}

	/**
	 * Sets the select all button visible.
	 * 
	 * @param showSelectAllButton
	 *            visible
	 */
	public void setShowSelectAllButton(boolean showSelectAllButton) {
		this.showSelectAllButton = showSelectAllButton;
	}

	/**
	 * Sets the suggestion pop-up's width as a CSS string. By using relative
	 * units (e.g. "50%") it's possible to set the popup's width relative to the
	 * ComboBoxMultiselect itself.
	 *
	 * @param suggestionPopupWidth
	 *            new popup width as CSS string, null for old default width
	 *            calculation based on items
	 */
	public void setSuggestionPopupWidth(String suggestionPopupWidth) {
		this.suggestionPopupWidth = suggestionPopupWidth;
	}

	/**
	 * Sets whether creation of new items when there is no match is allowed or
	 * not.
	 *
	 * @param allowNewItems
	 *            true to allow creation of new items, false to only allow
	 *            selection of existing items
	 */
	public void setAllowNewItems(boolean allowNewItems) {
		this.allowNewItems = allowNewItems;
	}

	/**
	 * Sets the total number of suggestions.
	 * <p>
	 * NOTE: this excluded the possible null selection item!
	 * <p>
	 * NOTE: this just updates the state, but doesn't update any UI.
	 *
	 * @since 8.0
	 * @param totalSuggestions
	 *            total number of suggestions
	 */
	public void setTotalSuggestions(int totalSuggestions) {
		this.totalSuggestions = totalSuggestions;
	}

	/**
	 * Gets the total number of suggestions, excluding the null selection item.
	 *
	 * @since 8.0
	 * @return total number of suggestions
	 */
	public int getTotalSuggestions() {
		return this.totalSuggestions;
	}

}
