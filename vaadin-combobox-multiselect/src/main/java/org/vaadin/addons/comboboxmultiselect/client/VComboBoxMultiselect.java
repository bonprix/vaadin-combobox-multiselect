package org.vaadin.addons.comboboxmultiselect.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.aria.client.CheckedValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Property;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.aria.client.State;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
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
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
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
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ComputedStyle;
import com.vaadin.client.ConnectorMap;
import com.vaadin.client.DeferredWorker;
import com.vaadin.client.Focusable;
import com.vaadin.client.UIDL;
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
import com.vaadin.shared.EventId;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.shared.util.SharedUtil;

/**
 * Client side implementation of the ComboBoxMultiselect component. Basis is the
 * {@link VFilterSelect}
 * 
 * @author Thorben von Hacht (bonprix Handelsgesellschaft mbH)
 * 
 */
public class VComboBoxMultiselect extends Composite
		implements Field, KeyDownHandler, KeyUpHandler, ClickHandler, FocusHandler, BlurHandler, Focusable,
		SubPartAware, HandlesAriaCaption, HandlesAriaInvalid, HandlesAriaRequired, DeferredWorker {

	/**
	 * Represents a suggestion in the suggestion popup box
	 */
	public class FilterSelectSuggestion implements Suggestion, Command {

		private final String key;
		private final String caption;
		private String untranslatedIconUri;
		private String style;

		private final VCheckBox checkBox;

		/**
		 * Constructor
		 * 
		 * @param uidl
		 *            The UIDL recieved from the server
		 */
		public FilterSelectSuggestion(UIDL uidl) {
			this.key = uidl.getStringAttribute("key");
			this.caption = uidl.getStringAttribute("caption");
			this.style = uidl.getStringAttribute("style");

			if (uidl.hasAttribute("icon")) {
				this.untranslatedIconUri = uidl.getStringAttribute("icon");
			}

			this.checkBox = new VCheckBox();
			boolean checkboxEnabled = uidl.hasAttribute("checkboxEnabled") && uidl.getBooleanAttribute("checkboxEnabled");
			this.checkBox.setEnabled(checkboxEnabled);
			State.HIDDEN.set(getCheckBoxElement(), true);
		}

		/**
		 * Gets the visible row in the popup as a HTML string. The string
		 * contains an image tag with the rows icon (if an icon has been
		 * specified) and the caption of the item
		 */
		@Override
		public String getDisplayString() {
			final StringBuffer sb = new StringBuffer();
			final Icon icon = VComboBoxMultiselect.this.client.getIcon(VComboBoxMultiselect.this.client.translateVaadinUri(this.untranslatedIconUri));

			if (icon != null) {
				sb.append(icon	.getElement()
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

		public String getAriaLabel() {
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
		 * @return
		 */
		public String getIconUri() {
			return VComboBoxMultiselect.this.client.translateVaadinUri(this.untranslatedIconUri);
		}

		/**
		 * Gets the style set for this suggestion item. Styles are typically set
		 * by a server-side {@link com.vaadin.ui.ComboBox.ItemStyleGenerator}.
		 * The returned style is prefixed by <code>v-filterselect-item-</code>.
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
			if (!(obj instanceof FilterSelectSuggestion)) {
				return false;
			}
			FilterSelectSuggestion other = (FilterSelectSuggestion) obj;
			if ((this.key == null && other.key != null) || (this.key != null && !this.key.equals(other.key))) {
				return false;
			}
			if ((this.caption == null && other.caption != null)
					|| (this.caption != null && !this.caption.equals(other.caption))) {
				return false;
			}
			if (!SharedUtil.equals(this.untranslatedIconUri, other.untranslatedIconUri)) {
				return false;
			}
			if (!SharedUtil.equals(this.style, other.style)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			try {
				return Integer.parseInt(this.key);
			} catch (NumberFormatException e) {
				return 0;
			}
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

		/**
		 * Default constructor
		 */
		SuggestionPopup() {
			super(true, false, true);
			debug("VFS.SP: constructor()");
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

			Roles	.getListboxRole()
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

		/**
		 * Shows the popup where the user can see the filtered options
		 * 
		 * @param currentSuggestions
		 *            The filtered suggestions
		 * @param currentPage
		 *            The current page number
		 * @param totalSuggestions
		 *            The total amount of suggestions
		 */
		public void showSuggestions(final Collection<FilterSelectSuggestion> currentSuggestions, final int currentPage,
				final int totalSuggestions) {

			debug("VFS.SP: showSuggestions(" + currentSuggestions + ", " + currentPage + ", " + totalSuggestions + ")");

			/*
			 * We need to defer the opening of the popup so that the parent DOM
			 * has stabilized so we can calculate an absolute top and left
			 * correctly. This issue manifests when a Combobox is placed in
			 * another popupView which also needs to calculate the absoluteTop()
			 * to position itself. #9768
			 * 
			 * After deferring the showSuggestions method, a problem with
			 * navigating in the combo box occurs. Because of that the method
			 * navigateItemAfterPageChange in ComboBoxConnector class, which
			 * navigates to the exact item after page was changed also was
			 * marked as deferred. #11333
			 */
			final SuggestionPopup popup = this;
			Scheduler	.get()
						.scheduleDeferred(new ScheduledCommand() {
							@Override
							public void execute() {
								// Add TT anchor point
								getElement().setId("VAADIN_COMBOBOX_OPTIONLIST");

								int nullOffset = (VComboBoxMultiselect.this.nullSelectionAllowed
										&& "".equals(VComboBoxMultiselect.this.lastFilter) ? 1 : 0);
								boolean firstPage = (currentPage == 0);
								final int first = currentPage * VComboBoxMultiselect.this.pageLength + 1
										- (firstPage ? 0 : nullOffset);
								final int last = first + currentSuggestions.size() - 1
										- (firstPage && "".equals(VComboBoxMultiselect.this.lastFilter) ? nullOffset
												: 0);
								final int matches = totalSuggestions - nullOffset;
								if (last > 0) {
									// nullsel not counted, as requested by user
									SuggestionPopup.this.status.setInnerText((matches == 0 ? 0 : first) + "-" + last
											+ "/" + matches);
								} else {
									SuggestionPopup.this.status.setInnerText("");
								}

								SuggestionPopup.this.menu.setSuggestions(currentSuggestions, first, matches);
								final int x = VComboBoxMultiselect.this.getAbsoluteLeft();

								SuggestionPopup.this.topPosition = VComboBoxMultiselect.this.tb.getAbsoluteTop();
								SuggestionPopup.this.topPosition += VComboBoxMultiselect.this.tb.getOffsetHeight();

								setPopupPosition(x, SuggestionPopup.this.topPosition);

								// We don't need to show arrows or statusbar if
								// there is
								// only one page
								if (totalSuggestions <= VComboBoxMultiselect.this.pageLength
										|| VComboBoxMultiselect.this.pageLength == 0) {
									setPagingEnabled(false);
								} else {
									setPagingEnabled(true);
								}
								setPrevButtonActive(first > 1);
								setNextButtonActive(last < matches);

								// clear previously fixed width
								SuggestionPopup.this.menu.setWidth("");
								SuggestionPopup.this.menu	.getElement()
															.getFirstChildElement()
															.getStyle()
															.clearWidth();

								setPopupPositionAndShow(popup);
								// Fix for #14173
								// IE9 and IE10 have a bug, when resize an a
								// element with
								// box-shadow.
								// IE9 and IE10 need explicit update to remove
								// extra
								// box-shadows
								if (BrowserInfo	.get()
												.isIE9()
										|| BrowserInfo	.get()
														.isIE10()) {
									forceReflow();
								}
							}
						});
		}

		/**
		 * Should the next page button be visible to the user?
		 * 
		 * @param active
		 */
		private void setNextButtonActive(boolean active) {
			if (VComboBoxMultiselect.this.enableDebug) {
				debug("VFS.SP: setNextButtonActive(" + active + ")");
			}
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
			if (VComboBoxMultiselect.this.enableDebug) {
				debug("VFS.SP: setPrevButtonActive(" + active + ")");
			}

			if (active) {
				DOM.sinkEvents(this.up, Event.ONCLICK);
				this.up.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-prevpage");
			} else {
				DOM.sinkEvents(this.up, 0);
				this.up.setClassName(VComboBoxMultiselect.this.getStylePrimaryName() + "-prevpage-off");
			}

		}

		/**
		 * Selects the next item in the filtered selections
		 */
		public void selectNextItem() {
			debug("VFS.SP: selectNextItem()");

			final int index = this.menu.getSelectedIndex() + 1;
			if (this.menu	.getItems()
							.size() > index) {
				selectItem(this.menu.getItems()
									.get(index));

			} else {
				selectNextPage();
			}
		}

		/**
		 * Selects the previous item in the filtered selections
		 */
		public void selectPrevItem() {
			debug("VFS.SP: selectPrevItem()");

			final int index = this.menu.getSelectedIndex() - 1;
			if (index > -1) {
				selectItem(this.menu.getItems()
									.get(index));

			} else if (index == -1) {
				selectPrevPage();

			} else {
				if (!this.menu	.getItems()
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
			if (!VComboBoxMultiselect.this.multiselect) {
				selectItem(this.menu.getFirstItem());
			} else {
				MenuItem mi = null;
				if (this.menu.getItems() != null && this.menu	.getItems()
																.size() > 0) {
					if (this.menu	.getItems()
									.size() > 1) {
						if (VComboBoxMultiselect.this.showClearButton
								&& VComboBoxMultiselect.this.showSelectAllButton) {
							mi = this.menu	.getItems()
											.get(2);
						} else if (VComboBoxMultiselect.this.showClearButton
								|| VComboBoxMultiselect.this.showSelectAllButton) {
							mi = this.menu	.getItems()
											.get(1);
						} else {
							mi = this.menu	.getItems()
											.get(0);
						}

					} else {
						mi = this.menu	.getItems()
										.get(0);
					}
				}
				selectItem(mi);
			}
		}

		/**
		 * Select the last item of the suggestions list popup.
		 * 
		 * @since 7.2.6
		 */
		public void selectLastItem() {
			debug("VFS.SP: selectLastItem()");
			selectItem(this.menu.getLastItem());
		}

		/*
		 * Sets the selected item in the popup menu.
		 */
		private void selectItem(final MenuItem newSelectedItem) {
			if (VComboBoxMultiselect.this.multiselect) {
				Property.ACTIVEDESCENDANT.set(	VComboBoxMultiselect.this.tb.getElement(),
												Id.of(newSelectedItem.getElement()));
			}

			this.menu.selectItem(newSelectedItem);

			if (!VComboBoxMultiselect.this.multiselect) {
				// Set the icon.
				FilterSelectSuggestion suggestion = (FilterSelectSuggestion) newSelectedItem.getCommand();
				setSelectedItemIcon(suggestion.getIconUri());

				// Set the text.
				setText(suggestion.getReplacementString());
			}
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
				debug("VFS.SP.LPS: run()");
				if (this.pagesToScroll != 0) {
					if (!VComboBoxMultiselect.this.waitingForFilteringResponse) {
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
						filterOptions(	VComboBoxMultiselect.this.currentPage + this.pagesToScroll,
										VComboBoxMultiselect.this.lastFilter, false);
					}
					this.pagesToScroll = 0;
				}
			}

			public void scrollUp() {
				debug("VFS.SP.LPS: scrollUp()");
				if (VComboBoxMultiselect.this.pageLength > 0
						&& VComboBoxMultiselect.this.currentPage + this.pagesToScroll > 0) {
					this.pagesToScroll--;
					cancel();
					schedule(200);
				}
			}

			public void scrollDown() {
				debug("VFS.SP.LPS: scrollDown()");
				if (VComboBoxMultiselect.this.pageLength > 0
						&& VComboBoxMultiselect.this.totalMatches > (VComboBoxMultiselect.this.currentPage
								+ this.pagesToScroll + 1) * VComboBoxMultiselect.this.pageLength) {
					this.pagesToScroll++;
					cancel();
					schedule(200);
				}
			}
		}

		@Override
		public void onBrowserEvent(Event event) {
			debug("VFS.SP: onBrowserEvent()");

			if (event.getTypeInt() == Event.ONCLICK) {
				final Element target = DOM.eventGetTarget(event);

				if (target == this.up || target == DOM.getChild(this.up, 0)) {
					this.lazyPageScroller.scrollUp();
				} else if (target == this.down || target == DOM.getChild(this.down, 0)) {
					this.lazyPageScroller.scrollDown();
				}

			} else if (event.getTypeInt() == Event.ONMOUSEWHEEL) {

				boolean scrollNotActive = !this.menu.isScrollActive();

				debug("VFS.SP: onBrowserEvent() scrollNotActive: " + scrollNotActive);

				if (scrollNotActive) {
					int velocity = event.getMouseWheelVelocityY();

					debug("VFS.SP: onBrowserEvent() velocity: " + velocity);

					if (velocity > 0) {
						this.lazyPageScroller.scrollDown();
					} else {
						this.lazyPageScroller.scrollUp();
					}
				}
			}

			/*
			 * Prevent the keyboard focus from leaving the textfield by
			 * preventing the default behaviour of the browser. Fixes #4285.
			 */
			handleMouseDownEvent(event);
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
			debug("VFS.SP: setPagingEnabled(" + paging + ")");
			if (this.isPagingEnabled == paging) {
				return;
			}
			if (paging) {
				this.down	.getStyle()
							.clearDisplay();
				this.up	.getStyle()
						.clearDisplay();
				this.status	.getStyle()
							.clearDisplay();
			} else {
				this.down	.getStyle()
							.setDisplay(Display.NONE);
				this.up	.getStyle()
						.setDisplay(Display.NONE);
				this.status	.getStyle()
							.setDisplay(Display.NONE);
			}
			this.isPagingEnabled = paging;
		}

		@Override
		public void setPosition(int offsetWidth, int offsetHeight) {
			debug("VFS.SP: setPosition(" + offsetWidth + ", " + offsetHeight + ")");

			int top = this.topPosition;
			int left = getPopupLeft();

			// reset menu size and retrieve its "natural" size
			this.menu.setHeight("");
			if (VComboBoxMultiselect.this.currentPage > 0 && !hasNextPage()) {
				// fix height to avoid height change when getting to last page
				this.menu.fixHeightTo(VComboBoxMultiselect.this.pageLength);
			}

			final int desiredHeight = offsetHeight = getOffsetHeight();
			final int desiredWidth = getMainWidth();

			debug("VFS.SP:     desired[" + desiredWidth + ", " + desiredHeight + "]");

			Element menuFirstChild = this.menu	.getElement()
												.getFirstChildElement();
			final int naturalMenuWidth = WidgetUtil.getRequiredWidth(menuFirstChild);

			if (this.popupOuterPadding == -1) {
				this.popupOuterPadding = WidgetUtil.measureHorizontalPaddingAndBorder(getElement(), 2);
			}

			if (naturalMenuWidth < desiredWidth) {
				this.menu.setWidth((desiredWidth - this.popupOuterPadding) + "px");
				menuFirstChild	.getStyle()
								.setWidth(100, Unit.PCT);
			}

			if (BrowserInfo	.get()
							.isIE()
					&& BrowserInfo	.get()
									.getBrowserMajorVersion() < 11) {
				// Must take margin,border,padding manually into account for
				// menu element as we measure the element child and set width to
				// the element parent
				double naturalMenuOuterWidth = WidgetUtil.getRequiredWidthDouble(menuFirstChild)
						+ getMarginBorderPaddingWidth(this.menu.getElement());

				/*
				 * IE requires us to specify the width for the container
				 * element. Otherwise it will be 100% wide
				 */
				double rootWidth = Math.max(desiredWidth - this.popupOuterPadding, naturalMenuOuterWidth);
				getContainerElement()	.getStyle()
										.setWidth(rootWidth, Unit.PX);
			}

			final int vfsHeight = VComboBoxMultiselect.this.getOffsetHeight();
			final int spaceAvailableAbove = top - vfsHeight;
			final int spaceAvailableBelow = Window.getClientHeight() - top;
			if (spaceAvailableBelow < offsetHeight && spaceAvailableBelow < spaceAvailableAbove) {
				// popup on top of input instead
				top -= offsetHeight + vfsHeight;
				if (top < 0) {
					offsetHeight += top;
					top = 0;
				}
			} else {
				offsetHeight = Math.min(offsetHeight, spaceAvailableBelow);
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

				final int naturalMenuWidthPlusScrollBar = naturalMenuWidth + WidgetUtil.getNativeScrollbarSize();
				if (offsetWidth < naturalMenuWidthPlusScrollBar) {
					this.menu.setWidth(naturalMenuWidthPlusScrollBar + "px");
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
		 * Was the popup just closed?
		 * 
		 * @return true if popup was just closed
		 */
		public boolean isJustClosed() {
			debug("VFS.SP: justClosed()");
			final long now = (new Date()).getTime();
			return (this.lastAutoClosed > 0 && (now - this.lastAutoClosed) < 200);
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
			if (VComboBoxMultiselect.this.enableDebug) {
				debug("VFS.SP: onClose(" + event.isAutoClosed() + ")");
			}
			if (event.isAutoClosed()) {
				this.lastAutoClosed = (new Date()).getTime();
			}
		}

		/**
		 * Updates style names in suggestion popup to help theme building.
		 * 
		 * @param uidl
		 *            UIDL for the whole combo box
		 * @param componentState
		 *            shared state of the combo box
		 */
		public void updateStyleNames(UIDL uidl, AbstractComponentState componentState) {
			debug("VFS.SP: updateStyleNames()");
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
				debug("VFS.SM: delayedImageLoadExecutioner()");
				if (VComboBoxMultiselect.this.suggestionPopup.isVisible()
						&& VComboBoxMultiselect.this.suggestionPopup.isAttached()) {
					setWidth("");
					getElement().getFirstChildElement()
								.getStyle()
								.clearWidth();
					VComboBoxMultiselect.this.suggestionPopup.setPopupPositionAndShow(VComboBoxMultiselect.this.suggestionPopup);
				}

			}
		});

		/**
		 * Default constructor
		 */
		SuggestionMenu() {
			super(true);
			debug("VFS.SM: constructor()");
			addDomHandler(this, LoadEvent.getType());

			setScrollEnabled(true);
		}

		/**
		 * Fixes menus height to use same space as full page would use. Needed
		 * to avoid height changes when quickly "scrolling" to last page.
		 */
		public void fixHeightTo(int pageItemsCount) {
			setHeight(getPreferredHeight(pageItemsCount));
		}

		/*
		 * Gets the preferred height of the menu including pageItemsCount items.
		 */
		String getPreferredHeight(int pageItemsCount) {
			if (VComboBoxMultiselect.this.currentSuggestions.size() > 0) {
				debug("getPreferredHeight(): " + getPreferredHeight());
				debug("currentSuggestions.size(): " + VComboBoxMultiselect.this.currentSuggestions.size());
				debug("pageItemsCount: " + pageItemsCount);
				int cntButtons = 0;
				if (VComboBoxMultiselect.this.showClearButton) {
					cntButtons++;
				}
				if (VComboBoxMultiselect.this.showSelectAllButton) {
					cntButtons++;
				}
				final int pixels = (getPreferredHeight()
						/ (VComboBoxMultiselect.this.currentSuggestions.size() + cntButtons)
						* (pageItemsCount + cntButtons));
				debug("pixels: " + pixels + "px");
				return pixels + "px";
			} else {
				return "";
			}
		}

		/**
		 * Sets the suggestions rendered in the menu
		 * 
		 * @param suggestions
		 *            The suggestions to be rendered in the menu
		 * @param firstSuggestionIndex
		 *            For accessibility: the index of the first rendered
		 *            suggestion
		 * @param numberOfSuggestions
		 *            For accessibility: the number of available suggestions
		 */
		public void setSuggestions(Collection<FilterSelectSuggestion> suggestions, int firstSuggestionIndex,
				int numberOfSuggestions) {
			if (VComboBoxMultiselect.this.enableDebug) {
				debug("VFS.SM: setSuggestions(" + suggestions + ")");
			}

			clearItems();

			if (VComboBoxMultiselect.this.showClearButton) {
				MenuItem clearMenuItem = new MenuItem(VComboBoxMultiselect.this.clearButtonCaption, false,
						VComboBoxMultiselect.this.clearCmd);
				clearMenuItem	.getElement()
								.setId(DOM.createUniqueId());
				clearMenuItem.addStyleName("align-center");
				Property.LABEL.set(clearMenuItem.getElement(), VComboBoxMultiselect.this.clearButtonCaption);
				this.addItem(clearMenuItem);
			}

			if (VComboBoxMultiselect.this.showSelectAllButton) {
				MenuItem selectAllMenuItem = new MenuItem(VComboBoxMultiselect.this.selectAllButtonCaption, false,
						VComboBoxMultiselect.this.selectAllCmd);
				selectAllMenuItem	.getElement()
									.setId(DOM.createUniqueId());
				selectAllMenuItem.addStyleName("align-center");
				Property.LABEL.set(selectAllMenuItem.getElement(), VComboBoxMultiselect.this.selectAllButtonCaption);
				this.addItem(selectAllMenuItem);
			}

			Iterator<FilterSelectSuggestion> it = suggestions.iterator();
			boolean isFirstIteration = true;
			boolean wasPreviousSelected = true;
			int currentSuggestionIndex = firstSuggestionIndex;
			while (it.hasNext()) {
				final FilterSelectSuggestion s = it.next();

				final MenuItem mi = new MenuItem(s.getDisplayString(), true, s);
				mi	.getElement()
					.setId(DOM.createUniqueId());

				String style = s.getStyle();
				if (style != null) {
					mi.addStyleName("v-filterselect-item-" + style);
				}
				Roles	.getOptionRole()
						.set(mi.getElement());

				WidgetUtil.sinkOnloadForImages(mi.getElement());

				boolean isSelected = VComboBoxMultiselect.this.selectedOptionKeys.contains(s);
				s.setChecked(isSelected);
				mi	.getElement()
					.insertFirst(s	.getCheckBox()
									.getElement());
				Property.LABEL.set(mi.getElement(), s.getAriaLabel());
				Property.SETSIZE.set(mi.getElement(), numberOfSuggestions);
				Property.POSINSET.set(mi.getElement(), currentSuggestionIndex);
				State.CHECKED.set(mi.getElement(), CheckedValue.of(isSelected));

				this.addItem(mi);

				// By default, first item on the list is always highlighted
				// or its the first not selected item on the list,
				// unless adding new items is allowed.
				if ((isFirstIteration || (wasPreviousSelected && !isSelected))
						&& !VComboBoxMultiselect.this.allowNewItem) {
					selectItem(mi);
				}
				wasPreviousSelected = isSelected;

				// If the filter matches the current selection, highlight that
				// instead of the first item.
				if (VComboBoxMultiselect.this.tb.getText()
												.equals(s.getReplacementString())
						&& s == VComboBoxMultiselect.this.currentSuggestion) {
					selectItem(mi);
				}

				currentSuggestionIndex++;
				isFirstIteration = false;
			}

			menuItemsAreOwnedByTextBox();
		}

		private void menuItemsAreOwnedByTextBox() {
			List<Id> ids = new ArrayList<>();
			for (MenuItem menuItem : getItems()) {
				ids.add(Id.of(menuItem.getElement()));
			}
			Property.OWNS.set(VComboBoxMultiselect.this.tb.getElement(), ids.toArray(new Id[0]));
		}

		@Override
		public void selectItem(MenuItem item) {
			MenuItem selectedItem = getSelectedItem();
			if (selectedItem != null) {
				State.SELECTED.set(selectedItem.getElement(), SelectedValue.FALSE);
			}

			State.SELECTED.set(item.getElement(), SelectedValue.TRUE);

			super.selectItem(item);
		}

		/**
		 * Send the current selection to the server. Triggered when a selection
		 * is made or on a blur event.
		 */
		public void doSelectedItemAction() {
			debug("VFS.SM: doSelectedItemAction()");
			// do not send a value change event if null was and stays selected
			final String enteredItemValue = VComboBoxMultiselect.this.tb.getText();
			if (VComboBoxMultiselect.this.nullSelectionAllowed && "".equals(enteredItemValue)
					&& !VComboBoxMultiselect.this.selectedOptionKeys.isEmpty()
					&& !VComboBoxMultiselect.this.selectedOptionKeys.contains("")) {
				if (VComboBoxMultiselect.this.nullSelectItem) {
					reset();
					return;
				}
				// null is not visible on pages != 0, and not visible when
				// filtering: handle separately
				if (!VComboBoxMultiselect.this.multiselect) {
					VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "filter", "",
																	false);
					VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "page", 0,
																	false);
				}
				VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "selected",
																new String[] {}, VComboBoxMultiselect.this.immediate);
				afterUpdateClientVariables();

				// suggestionPopup.hide();
				return;
			}

			VComboBoxMultiselect.this.updateSelectionWhenReponseIsReceived = VComboBoxMultiselect.this.waitingForFilteringResponse;
			if (!VComboBoxMultiselect.this.waitingForFilteringResponse) {
				doPostFilterSelectedItemAction();
			}
		}

		/**
		 * Triggered after a selection has been made
		 */
		public void doPostFilterSelectedItemAction() {
			debug("VFS.SM: doPostFilterSelectedItemAction()");
			final MenuItem item = getSelectedItem();
			final String enteredItemValue = VComboBoxMultiselect.this.tb.getText();

			VComboBoxMultiselect.this.updateSelectionWhenReponseIsReceived = false;

			// check for exact match in menu
			if (!VComboBoxMultiselect.this.multiselect) {
				int p = getItems().size();
				if (p > 0) {
					for (int i = 0; i < p; i++) {
						final MenuItem potentialExactMatch = getItems().get(i);
						if (potentialExactMatch	.getText()
												.equals(enteredItemValue)) {
							selectItem(potentialExactMatch);
							// do not send a value change event if null was and
							// stays selected
							if (!"".equals(enteredItemValue) || (!VComboBoxMultiselect.this.selectedOptionKeys.isEmpty()
									&& !"".equals(VComboBoxMultiselect.this.selectedOptionKeys	.iterator()
																								.next()))) {
								doItemAction(potentialExactMatch, true);
							}
							VComboBoxMultiselect.this.suggestionPopup.hide();
							return;
						}
					}
				}

				if (VComboBoxMultiselect.this.allowNewItem) {

					if (!VComboBoxMultiselect.this.prompting
							&& !enteredItemValue.equals(VComboBoxMultiselect.this.lastNewItemString)) {
						/*
						 * Store last sent new item string to avoid double sends
						 */
						VComboBoxMultiselect.this.lastNewItemString = enteredItemValue;
						VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId,
																		"newitem", enteredItemValue,
																		VComboBoxMultiselect.this.immediate);
						afterUpdateClientVariables();
					}
				} else if (item != null && !"".equals(VComboBoxMultiselect.this.lastFilter)
						&& (VComboBoxMultiselect.this.filteringmode == FilteringMode.CONTAINS ? item.getText()
																									.toLowerCase()
																									.contains(VComboBoxMultiselect.this.lastFilter.toLowerCase())
								: item	.getText()
										.toLowerCase()
										.startsWith(VComboBoxMultiselect.this.lastFilter.toLowerCase()))) {
					doItemAction(item, true);
				} else {
					// currentSuggestion has key="" for nullselection
					VComboBoxMultiselect.this.selectedOptionKeys.clear();
					if (VComboBoxMultiselect.this.currentSuggestion != null
							&& !VComboBoxMultiselect.this.currentSuggestion.key.equals("")) {
						// An item (not null) selected
						String text = VComboBoxMultiselect.this.currentSuggestion.getReplacementString();
						setText(text);
						VComboBoxMultiselect.this.selectedOptionKeys.add(VComboBoxMultiselect.this.currentSuggestion);
					} else {
						// Null selected
						setText("");
					}
				}
				VComboBoxMultiselect.this.suggestionPopup.hide();
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
				menuItemRoot = menuItemRoot	.getParentElement()
											.cast();
			}
			// "menuItemRoot" is now the root of the menu item

			final int itemCount = getItems().size();
			for (int i = 0; i < itemCount; i++) {
				if (getItems()	.get(i)
								.getElement() == menuItemRoot) {
					String name = SUBPART_PREFIX + i;
					return name;
				}
			}
			return null;
		}

		@Override
		public void onLoad(LoadEvent event) {
			debug("VFS.SM: onLoad()");
			// Handle icon onload events to ensure shadow is resized
			// correctly
			this.delayedImageLoadExecutioner.trigger();

		}

		/**
		 * @deprecated use {@link SuggestionPopup#selectFirstItem()} instead.
		 */
		@Deprecated
		public void selectFirstItem() {
			debug("VFS.SM: selectFirstItem()");
			MenuItem firstItem = getItems().get(0);
			selectItem(firstItem);
		}

		/**
		 * @deprecated use {@link SuggestionPopup#selectLastItem()} instead.
		 */
		@Deprecated
		public void selectLastItem() {
			debug("VFS.SM: selectLastItem()");
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
															.getOffsetHeight()
					: 0;
		}

		/*
		 * Gets the width of one menu item.
		 */
		int getItemOffsetWidth() {
			List<MenuItem> items = getItems();
			return items != null && items.size() > 0 ? items.get(0)
															.getOffsetWidth()
					: 0;
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

		@Override
		public void onBrowserEvent(Event event) {
			// stop double propagation from vcheckbox click
			if (VComboBoxMultiselect.this.multiselect && event.getTypeInt() == 1
					&& event.getEventTarget() == event.getCurrentEventTarget()) {
				return;
			}

			super.onBrowserEvent(event);
		}

	}

	/**
	 * TextBox variant used as input element for filter selects, which prevents
	 * selecting text when disabled.
	 * 
	 * @since 7.1.5
	 */
	public class FilterSelectTextBox extends TextBox {

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

	@Deprecated
	public static final FilteringMode FILTERINGMODE_OFF = FilteringMode.OFF;
	@Deprecated
	public static final FilteringMode FILTERINGMODE_STARTSWITH = FilteringMode.STARTSWITH;
	@Deprecated
	public static final FilteringMode FILTERINGMODE_CONTAINS = FilteringMode.CONTAINS;

	public static final String CLASSNAME = "v-filterselect";
	private static final String STYLE_NO_INPUT = "no-input";

	/** For internal use only. May be removed or replaced in the future. */
	public int pageLength = 10;

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
	private final HTML popupOpener = new HTML("") {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.google.gwt.user.client.ui.Widget#onBrowserEvent(com.google.gwt
		 * .user.client.Event)
		 */

		@Override
		public void onBrowserEvent(Event event) {
			super.onBrowserEvent(event);

			/*
			 * Prevent the keyboard focus from leaving the textfield by
			 * preventing the default behaviour of the browser. Fixes #4285.
			 */
			handleMouseDownEvent(event);
		}
	};

	private class IconWidget extends Widget {
		IconWidget(Icon icon) {
			setElement(icon.getElement());
			addDomHandler(VComboBoxMultiselect.this, ClickEvent.getType());
		}
	}

	private IconWidget selectedItemIcon;

	/** For internal use only. May be removed or replaced in the future. */
	public ApplicationConnection client;

	/** For internal use only. May be removed or replaced in the future. */
	public String paintableId;

	/** For internal use only. May be removed or replaced in the future. */
	public int currentPage;

	/**
	 * A collection of available suggestions (options) as received from the
	 * server.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public final List<FilterSelectSuggestion> currentSuggestions = new ArrayList<FilterSelectSuggestion>();

	/** For internal use only. May be removed or replaced in the future. */
	public boolean immediate;

	/** For internal use only. May be removed or replaced in the future. */
	public Set<FilterSelectSuggestion> selectedOptionKeys = new HashSet<FilterSelectSuggestion>();

	/** For internal use only. May be removed or replaced in the future. */
	Command clearCmd = new Command() {

		@Override
		public void execute() {
			debug("VFS: clearCmd()");

			VComboBoxMultiselect.this.updateSelectionWhenReponseIsReceived = false;

			int page = 0;
			String filter = "";
			VComboBoxMultiselect.this.tb.setText(filter);

			VComboBoxMultiselect.this.waitingForFilteringResponse = true;
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "sortingneeded",
															true, false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "filter", filter,
															false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "page", page, false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "clear", true,
															VComboBoxMultiselect.this.immediate);
			afterUpdateClientVariables();

			VComboBoxMultiselect.this.selectPopupItemWhenResponseIsReceived = Select.FIRST;
			VComboBoxMultiselect.this.lastFilter = filter;
			VComboBoxMultiselect.this.currentPage = page;
		}
	};

	/** For internal use only. May be removed or replaced in the future. */
	Command selectAllCmd = new Command() {

		@Override
		public void execute() {
			debug("VFS: selectAllCmd()");

			int page = 0;
			String filter = "";
			VComboBoxMultiselect.this.tb.setText(filter);

			VComboBoxMultiselect.this.updateSelectionWhenReponseIsReceived = false;

			VComboBoxMultiselect.this.waitingForFilteringResponse = true;
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "sortingneeded",
															true, false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "filter", filter,
															false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "page", page, false);
			VComboBoxMultiselect.this.client.updateVariable(VComboBoxMultiselect.this.paintableId, "selectedAll", true,
															VComboBoxMultiselect.this.immediate);
			afterUpdateClientVariables();

			VComboBoxMultiselect.this.selectPopupItemWhenResponseIsReceived = Select.FIRST;
			VComboBoxMultiselect.this.lastFilter = filter;
			VComboBoxMultiselect.this.currentPage = page;
		}
	};

	/** For internal use only. May be removed or replaced in the future. */
	public boolean waitingForFilteringResponse = false;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean updateSelectionWhenReponseIsReceived = false;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean initDone = false;

	/** For internal use only. May be removed or replaced in the future. */
	public String lastFilter = "";

	/** For internal use only. May be removed or replaced in the future. */
	public enum Select {
		NONE, FIRST, LAST
	}

	/** For internal use only. May be removed or replaced in the future. */
	public Select selectPopupItemWhenResponseIsReceived = Select.NONE;

	/**
	 * The current suggestion selected from the dropdown. This is one of the
	 * values in currentSuggestions except when filtering, in this case
	 * currentSuggestion might not be in currentSuggestions.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public FilterSelectSuggestion currentSuggestion;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean allowNewItem;

	/** For internal use only. May be removed or replaced in the future. */
	public int totalMatches;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean nullSelectionAllowed;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean nullSelectItem;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean multiselect;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean enabled;

	/** For internal use only. May be removed or replaced in the future. */
	public boolean readonly;

	/** For internal use only. May be removed or replaced in the future. */
	public FilteringMode filteringmode = FilteringMode.OFF;

	// shown in unfocused empty field, disappears on focus (e.g "Search here")
	private static final String CLASSNAME_PROMPT = "prompt";

	/** For internal use only. May be removed or replaced in the future. */
	public String inputPrompt = "";

	/** For internal use only. May be removed or replaced in the future. */
	public boolean prompting = false;

	/**
	 * Set true when popupopened has been clicked. Cleared on each UIDL-update.
	 * This handles the special case where are not filtering yet and the
	 * selected value has changed on the server-side. See #2119
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public boolean popupOpenerClicked;

	/** For internal use only. May be removed or replaced in the future. */
	public int suggestionPopupMinWidth = 0;

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

	/**
	 * Default constructor.
	 */
	public VComboBoxMultiselect() {
		this.tb = createTextBox();
		Roles	.getComboboxRole()
				.set(this.tb.getElement());

		this.suggestionPopup = createSuggestionPopup();

		this.popupOpener.sinkEvents(Event.ONMOUSEDOWN);
		Roles	.getButtonRole()
				.setAriaHiddenState(this.popupOpener.getElement(), true);
		Roles	.getButtonRole()
				.set(this.popupOpener.getElement());

		this.panel.add(this.tb);
		this.panel.add(this.popupOpener);
		initWidget(this.panel);

		this.tb.addKeyDownHandler(this);
		this.tb.addKeyUpHandler(this);

		this.tb.addFocusHandler(this);
		this.tb.addBlurHandler(this);
		this.tb.addClickHandler(this);

		this.popupOpener.addClickHandler(this);

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
				Scheduler	.get()
							.scheduleDeferred(new ScheduledCommand() {

								@Override
								public void execute() {
									filterOptions(VComboBoxMultiselect.this.currentPage);
								}
							});
			}
		}
	}

	/**
	 * This method will create the TextBox used by the MyComponentWidget
	 * instance. It is invoked during the Constructor and should only be
	 * overridden if a custom TextBox shall be used. The overriding method
	 * cannot use any instance variables.
	 * 
	 * @since 7.1.5
	 * @return TextBox instance used by this MyComponentWidget
	 */
	protected TextBox createTextBox() {
		return new FilterSelectTextBox();
	}

	/**
	 * This method will create the SuggestionPopup used by the MyComponentWidget
	 * instance. It is invoked during the Constructor and should only be
	 * overridden if a custom SuggestionPopup shall be used. The overriding
	 * method cannot use any instance variables.
	 * 
	 * @since 7.1.5
	 * @return SuggestionPopup instance used by this MyComponentWidget
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
		if (this.pageLength > 0 && this.totalMatches > (this.currentPage + 1) * this.pageLength) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Filters the options at a certain page. Uses the text box input as a
	 * filter
	 * 
	 * @param page
	 *            The page which items are to be filtered
	 */
	public void filterOptions(int page) {
		filterOptions(page, this.tb.getText(), false);
	}

	/**
	 * Filters the options at certain page using the given filter
	 * 
	 * @param page
	 *            The page to filter
	 * @param filter
	 *            The filter to apply to the components
	 */
	public void filterOptions(int page, String filter, boolean sortingNeeded) {
		filterOptions(page, filter, sortingNeeded, true);
	}

	/**
	 * Filters the options at certain page using the given filter
	 * 
	 * @param page
	 *            The page to filter
	 * @param filter
	 *            The filter to apply to the options
	 * @param immediate
	 *            Whether to send the options request immediately
	 */
	private void filterOptions(int page, String filter, boolean sortingNeeded, boolean immediate) {
		debug("VFS: filterOptions(" + page + ", " + filter + ", " + immediate + ")");

		if (filter.equals(this.lastFilter) && this.currentPage == page) {
			if (!this.suggestionPopup.isAttached()) {
				this.suggestionPopup.showSuggestions(this.currentSuggestions, this.currentPage, this.totalMatches);
			}
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

		this.waitingForFilteringResponse = true;
		if (sortingNeeded) {
			this.client.updateVariable(this.paintableId, "sortingneeded", sortingNeeded, false);
		}
		this.client.updateVariable(this.paintableId, "filter", filter, false);
		this.client.updateVariable(this.paintableId, "page", page, immediate);
		afterUpdateClientVariables();

		this.lastFilter = filter;
		this.currentPage = page;
	}

	/** For internal use only. May be removed or replaced in the future. */
	public void updateReadOnly() {
		debug("VFS: updateReadOnly()");
		this.tb.setReadOnly(this.readonly || !this.textInputEnabled);
	}

	public void setTextInputEnabled(boolean textInputEnabled) {
		debug("VFS: setTextInputEnabled()");
		// Always update styles as they might have been overwritten
		if (textInputEnabled) {
			removeStyleDependentName(STYLE_NO_INPUT);
			Roles	.getTextboxRole()
					.removeAriaReadonlyProperty(this.tb.getElement());
		} else {
			addStyleDependentName(STYLE_NO_INPUT);
			Roles	.getTextboxRole()
					.setAriaReadonlyProperty(this.tb.getElement(), true);
		}

		if (this.textInputEnabled == textInputEnabled) {
			return;
		}

		this.textInputEnabled = textInputEnabled;
		updateReadOnly();
	}

	/**
	 * Sets the text in the text box.
	 * 
	 * @param text
	 *            the text to set in the text box
	 */
	public void setTextboxText(final String text) {
		if (this.enableDebug) {
			debug("VFS: setTextboxText(" + text + ")");
		}
		setText(text);
	}

	private void setText(final String text) {
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
	 * Turns prompting on. When prompting is turned on a command prompt is shown
	 * in the text box if nothing has been entered.
	 */
	public void setPromptingOn() {
		debug("VFS: setPromptingOn()");
		if (!this.prompting) {
			this.prompting = true;
			addStyleDependentName(CLASSNAME_PROMPT);
		}
		setTextboxText(this.inputPrompt);
	}

	/**
	 * Turns prompting off. When prompting is turned on a command prompt is
	 * shown in the text box if nothing has been entered.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 * 
	 * @param text
	 *            The text the text box should contain.
	 */
	public void setPromptingOff(String text) {
		debug("VFS: setPromptingOff()");
		setTextboxText(text);
		if (this.prompting) {
			this.prompting = false;
			removeStyleDependentName(CLASSNAME_PROMPT);
		}
	}

	/**
	 * Triggered when a suggestion is selected
	 * 
	 * @param suggestion
	 *            The suggestion that just got selected.
	 */
	public void onSuggestionSelected(FilterSelectSuggestion suggestion) {
		if (this.enableDebug) {
			debug("VFS: onSuggestionSelected(" + suggestion.caption + ": " + suggestion.key + ")");
		}
		this.updateSelectionWhenReponseIsReceived = false;

		this.currentSuggestion = suggestion;
		String newKey;
		if (suggestion.key.equals("")) {
			// "nullselection"
			newKey = "";
		} else {
			// normal selection
			newKey = suggestion.getOptionKey();
		}

		if (!this.multiselect) {
			String text = suggestion.getReplacementString();

			if ("".equals(newKey) && !this.focused) {
				setPromptingOn();
			} else {
				setPromptingOff(text);
			}

			setSelectedItemIcon(suggestion.getIconUri());
		} else {
			if (!"".equals(newKey)) {
				if (this.selectedOptionKeys.contains(suggestion)) {
					suggestion.setChecked(false);
					this.selectedOptionKeys.remove(suggestion);
				} else {
					suggestion.setChecked(true);
					this.selectedOptionKeys.add(suggestion);
				}
			}

		}

		String[] values = new String[this.selectedOptionKeys.size()];
		int i = 0;
		for (FilterSelectSuggestion item : this.selectedOptionKeys) {
			values[i++] = item.key;
		}
		if (this.multiselect) {
			this.client.updateVariable(this.paintableId, "filter", this.tb.getText(), false);
		}
		this.client.updateVariable(this.paintableId, "page", this.currentPage, false);
		this.client.updateVariable(this.paintableId, "selected", values, this.immediate);
		afterUpdateClientVariables();

		// currentPage = -1; // forget the page

		// suggestionPopup.hide();
	}

	/**
	 * Sets the icon URI of the selected item. The icon is shown on the left
	 * side of the item caption text. Set the URI to null to remove the icon.
	 * 
	 * @param iconUri
	 *            The URI of the icon
	 */
	public void setSelectedItemIcon(String iconUri) {
		if (!this.multiselect) {
			if (iconUri == null || iconUri.length() == 0) {
				if (this.selectedItemIcon != null) {
					this.panel.remove(this.selectedItemIcon);
					this.selectedItemIcon = null;
					afterSelectedItemIconChange();
				}
			} else {
				if (this.selectedItemIcon != null) {
					this.panel.remove(this.selectedItemIcon);
				}
				this.selectedItemIcon = new IconWidget(this.client.getIcon(iconUri));
				// Older IE versions don't scale icon correctly if DOM
				// contains height and width attributes.
				this.selectedItemIcon	.getElement()
										.removeAttribute("height");
				this.selectedItemIcon	.getElement()
										.removeAttribute("width");
				this.selectedItemIcon.addDomHandler(new LoadHandler() {
					@Override
					public void onLoad(LoadEvent event) {
						afterSelectedItemIconChange();
					}
				}, LoadEvent.getType());
				this.panel.insert(this.selectedItemIcon, 0);
				afterSelectedItemIconChange();
			}
		}
	}

	private void afterSelectedItemIconChange() {
		if (BrowserInfo	.get()
						.isWebkit()
				|| BrowserInfo	.get()
								.isIE8()) {
			// Some browsers need a nudge to reposition the text field
			forceReflow();
		}
		updateRootWidth();
		if (this.selectedItemIcon != null) {
			updateSelectedIconPosition();
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
		this.selectedItemIcon	.getElement()
								.getStyle()
								.setMarginTop(marginTop, Unit.PX);
	}

	private static Set<Integer> navigationKeyCodes = new HashSet<Integer>();

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

			if (this.enableDebug) {
				debug("VFS: key down: " + keyCode);
			}
			if (this.waitingForFilteringResponse && navigationKeyCodes.contains(keyCode)) {
				/*
				 * Keyboard navigation events should not be handled while we are
				 * waiting for a response. This avoids flickering, disappearing
				 * items, wrongly interpreted responses and more.
				 */
				if (this.enableDebug) {
					debug("Ignoring " + keyCode + " because we are waiting for a filtering response");
				}
				DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
				event.stopPropagation();
				return;
			}

			if (this.suggestionPopup.isAttached()) {
				if (this.enableDebug) {
					debug("Keycode " + keyCode + " target is popup");
				}
				popupKeyDown(event);
			} else {
				if (this.enableDebug) {
					debug("Keycode " + keyCode + " target is text field");
				}
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
		if (this.enableDebug) {
			debug("VFS: inputFieldKeyDown(" + event.getNativeKeyCode() + ")");
		}
		switch (event.getNativeKeyCode()) {
		case KeyCodes.KEY_DOWN:
		case KeyCodes.KEY_UP:
		case KeyCodes.KEY_PAGEDOWN:
		case KeyCodes.KEY_PAGEUP:
			// open popup as from gadget
			filterOptions(-1, "", true);
			this.lastFilter = "";
			this.tb.selectAll();
			break;
		case KeyCodes.KEY_ENTER:
			/*
			 * This only handles the case when new items is allowed, a text is
			 * entered, the popup opener button is clicked to close the popup
			 * and enter is then pressed (see #7560).
			 */
			if (!this.allowNewItem) {
				return;
			}

			if (this.currentSuggestion != null && this.tb	.getText()
															.equals(this.currentSuggestion.getReplacementString())) {
				// Retain behavior from #6686 by returning without stopping
				// propagation if there's nothing to do
				return;
			}
			this.suggestionPopup.menu.doSelectedItemAction();

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
		if (this.enableDebug) {
			debug("VFS: popupKeyDown(" + event.getNativeKeyCode() + ")");
		}
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
			if (this.multiselect) {
				this.suggestionPopup.hide();
				break;
			}
		case KeyCodes.KEY_ENTER:

			if (!this.allowNewItem) {
				int index = this.suggestionPopup.menu.getSelectedIndex();
				debug("index before: " + index);
				if (this.showClearButton && this.showSelectAllButton) {
					index = index - 2;
				} else if (this.showClearButton || this.showSelectAllButton) {
					index = index - 1;
				}

				debug("index after: " + index);
				if (index == -2) {
					this.clearCmd.execute();
				} else if (index == -1) {
					if (this.showSelectAllButton) {
						this.selectAllCmd.execute();
					} else {
						this.clearCmd.execute();
					}
				} else {
					debug("entered suggestion: " + this.currentSuggestions.get(index).caption);
					onSuggestionSelected(this.currentSuggestions.get(index));
				}
			} else {
				// Handle addition of new items.
				this.suggestionPopup.menu.doSelectedItemAction();
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
			filterOptions(this.currentPage - 1, this.lastFilter, false);
			this.selectPopupItemWhenResponseIsReceived = Select.LAST;
		}
	}

	/*
	 * Show the next page.
	 */
	private void selectNextPage() {
		if (hasNextPage()) {
			filterOptions(this.currentPage + 1, this.lastFilter, false);
			this.selectPopupItemWhenResponseIsReceived = Select.FIRST;
		}
	}

	/**
	 * Triggered when a key was depressed
	 * 
	 * @param event
	 *            The KeyUpEvent of the key depressed
	 */

	@Override
	public void onKeyUp(KeyUpEvent event) {
		if (this.enableDebug) {
			debug("VFS: onKeyUp(" + event.getNativeKeyCode() + ")");
		}
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
	 * Resets the Select to its initial state
	 */
	private void reset() {
		debug("VFS: reset()");
		if (!this.multiselect) {
			this.selectedOptionKeys.clear();
			if (this.currentSuggestion != null) {
				String text = this.currentSuggestion.getReplacementString();
				setPromptingOff(text);
				setSelectedItemIcon(this.currentSuggestion.getIconUri());

				this.selectedOptionKeys.add(this.currentSuggestion);

			} else {
				if (this.focused || this.readonly || !this.enabled) {
					setPromptingOff("");
				} else {
					setPromptingOn();
				}
				setSelectedItemIcon(null);
			}
		}

		this.lastFilter = "";
		this.suggestionPopup.hide();
	}

	/**
	 * Listener for popupopener
	 */

	@Override
	public void onClick(ClickEvent event) {
		debug("VFS: onClick()");
		if (this.textInputEnabled && event	.getNativeEvent()
											.getEventTarget()
											.cast() == this.tb.getElement()) {
			// Don't process clicks on the text field if text input is enabled
			return;
		}
		if (this.enabled && !this.readonly) {
			// ask suggestionPopup if it was just closed, we are using GWT
			// Popup's auto close feature
			if (!this.suggestionPopup.isJustClosed()) {
				// If a focus event is not going to be sent, send the options
				// request immediately; otherwise queue in the same burst as the
				// focus event. Fixes #8321.
				boolean immediate = this.focused || !this.client.hasEventListeners(this, EventId.FOCUS);
				filterOptions(-1, "", immediate);
				this.popupOpenerClicked = true;
				this.lastFilter = "";
			}
			DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
			focus();
			this.tb.selectAll();
		}
	}

	/**
	 * Update minimum width for FilterSelect textarea based on input prompt and
	 * suggestions.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public void updateSuggestionPopupMinWidth() {
		// used only to calculate minimum width
		String captions = WidgetUtil.escapeHTML(this.inputPrompt);

		for (FilterSelectSuggestion suggestion : this.currentSuggestions) {
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
	 * A flag which prevents a focus event from taking place
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
		debug("VFS: onFocus()");
		debug("selectedOptionKeys: " + this.selectedOptionKeys.size() + " - " + this.selectedOptionKeys.toString());
		/*
		 * When we disable a blur event in ie we need to refocus the textfield.
		 * This will cause a focus event we do not want to process, so in that
		 * case we just ignore it.
		 */
		if (BrowserInfo	.get()
						.isIE()
				&& this.iePreventNextFocus) {
			this.iePreventNextFocus = false;
			return;
		}

		this.focused = true;
		if ((this.prompting || this.multiselect) && !this.readonly) {
			setPromptingOff("");
		}
		addStyleDependentName("focus");

		if (this.client.hasEventListeners(this, EventId.FOCUS)) {
			this.client.updateVariable(this.paintableId, EventId.FOCUS, "", true);
			afterUpdateClientVariables();
		}

		ComponentConnector connector = ConnectorMap	.get(this.client)
													.getConnector(this);
		this.client	.getVTooltip()
					.showAssistive(connector.getTooltipInfo(getElement()));
	}

	/**
	 * A flag which cancels the blur event and sets the focus back to the
	 * textfield if the Browser is IE
	 */
	boolean preventNextBlurEventInIE = false;

	public boolean showClearButton;
	public String clearButtonCaption;

	public boolean showSelectAllButton;
	public String selectAllButtonCaption;

	public String singleSelectionCaption = null;
	public String multiSelectionCaption = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.event.dom.client.BlurHandler#onBlur(com.google.gwt.event
	 * .dom.client.BlurEvent)
	 */

	@Override
	public void onBlur(BlurEvent event) {
		debug("VFS: onBlur()");

		if (BrowserInfo	.get()
						.isIE()
				&& this.preventNextBlurEventInIE) {
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
			if (getElement().isOrHasChild(focusedElement) || this.suggestionPopup	.getElement()
																					.isOrHasChild(focusedElement)) {

				// IF the suggestion popup or another part of the
				// MyComponentWidget
				// was focused, move the focus back to the textfield and prevent
				// the triggered focus event (in onFocus).
				this.iePreventNextFocus = true;
				this.tb.setFocus(true);
				return;
			}
		}

		this.focused = false;
		if (!this.readonly) {
			if (this.selectedOptionKeys.isEmpty()) {
				setPromptingOn();
			} else if (!this.multiselect) {
				if (this.currentSuggestion != null) {
					setPromptingOff(this.currentSuggestion.caption);
				}
			} else {
				List<FilterSelectSuggestion> sortedList = new ArrayList<>(this.selectedOptionKeys);
				Collections.sort(sortedList, new Comparator<FilterSelectSuggestion>() {

					@Override
					public int compare(FilterSelectSuggestion o1, FilterSelectSuggestion o2) {
						return o1	.getReplacementString()
									.compareTo(o2.getReplacementString());
					}
				});

				String selectedCaption = "";
				if (this.singleSelectionCaption != null && this.selectedOptionKeys.size() == 1) {
					debug("this.singleSelectionCaption: " + this.singleSelectionCaption);
					selectedCaption = this.singleSelectionCaption;
				} else if (this.multiSelectionCaption != null && this.selectedOptionKeys.size() > 1) {
					debug("this.multiSelectionCaption: " + this.multiSelectionCaption);
					selectedCaption = this.multiSelectionCaption;
				} else {
					debug("default selectedCaption");
					StringBuffer sb = new StringBuffer();
					sb.append("(" + this.selectedOptionKeys.size() + ") ");
					for (Iterator<FilterSelectSuggestion> iterator = sortedList.iterator(); iterator.hasNext();) {
						FilterSelectSuggestion selectedOptionKey = iterator.next();
						sb.append(selectedOptionKey.getReplacementString());
						if (iterator.hasNext()) {
							sb.append("; ");
						}
					}
					selectedCaption = sb.toString();
				}

				debug("selectedCaption: " + selectedCaption);
				setPromptingOff(selectedCaption);
			}
		}
		removeStyleDependentName("focus");

		if (this.client.hasEventListeners(this, EventId.BLUR)) {
			this.client.updateVariable(this.paintableId, EventId.BLUR, "", true);
			afterUpdateClientVariables();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.client.Focusable#focus()
	 */

	@Override
	public void focus() {
		debug("VFS: focus()");
		this.focused = true;
		if (this.prompting && !this.readonly) {
			setPromptingOff("");
		}
		this.tb.setFocus(true);
	}

	/**
	 * Calculates the width of the select if the select has undefined width.
	 * Should be called when the width changes or when the icon changes.
	 * <p>
	 * For internal use only. May be removed or replaced in the future.
	 */
	public void updateRootWidth() {
		ComponentConnector paintable = ConnectorMap	.get(this.client)
													.getConnector(this);

		if (paintable.isUndefinedWidth()) {

			/*
			 * When the select has a undefined with we need to check that we are
			 * only setting the text box width relative to the first page width
			 * of the items. If this is not done the text box width will change
			 * when the popup is used to view longer items than the text box is
			 * wide.
			 */
			int w = WidgetUtil.getRequiredWidth(this);

			if ((!this.initDone || this.currentPage + 1 < 0) && this.suggestionPopupMinWidth > w) {
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

				this.tb.setWidth((this.suggestionPopupMinWidth - iconWidth - buttonWidth) + "px");

			}

			/*
			 * Lock the textbox width to its current value if it's not already
			 * locked
			 */
			if (!this.tb.getElement()
						.getStyle()
						.getWidth()
						.endsWith("px")) {
				int iconWidth = this.selectedItemIcon == null ? 0 : this.selectedItemIcon.getOffsetWidth();
				this.tb.setWidth((this.tb.getOffsetWidth() - iconWidth) + "px");
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
	 * Handles special behavior of the mouse down event
	 * 
	 * @param event
	 */
	private void handleMouseDownEvent(Event event) {
		/*
		 * Prevent the keyboard focus from leaving the textfield by preventing
		 * the default behaviour of the browser. Fixes #4285.
		 */
		if (event.getTypeInt() == Event.ONMOUSEDOWN) {
			event.preventDefault();
			event.stopPropagation();

			/*
			 * In IE the above wont work, the blur event will still trigger. So,
			 * we set a flag here to prevent the next blur event from happening.
			 * This is not needed if do not already have focus, in that case
			 * there will not be any blur event and we should not cancel the
			 * next blur.
			 */
			if (BrowserInfo	.get()
							.isIE()
					&& this.focused) {
				this.preventNextBlurEventInIE = true;
				debug("VFS: Going to prevent next blur event on IE");
			}
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
		if (this.tb	.getElement()
					.isOrHasChild(subElement)) {
			return "textbox";
		} else if (this.popupOpener	.getElement()
									.isOrHasChild(subElement)) {
			return "button";
		} else if (this.suggestionPopup	.getElement()
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

	/*
	 * Anything that should be set after the client updates the server.
	 */
	private void afterUpdateClientVariables() {
		// We need this here to be consistent with the all the calls.
		// Then set your specific selection type only after
		// client.updateVariable() method call.
		this.selectPopupItemWhenResponseIsReceived = Select.NONE;
	}

	@Override
	public boolean isWorkPending() {
		return this.waitingForFilteringResponse || this.suggestionPopup.lazyPageScroller.isRunning();
	}

}
