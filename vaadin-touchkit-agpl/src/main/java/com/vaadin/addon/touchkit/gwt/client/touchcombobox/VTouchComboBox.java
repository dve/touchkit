package com.vaadin.addon.touchkit.gwt.client.touchcombobox;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.addon.touchkit.gwt.client.popover.VPopover;
import com.vaadin.addon.touchkit.gwt.client.touchcombobox.PageEvent.PageEventType;

/**
 * 
 */
public class VTouchComboBox extends Widget implements
        HasValueChangeHandlers<String>, ResizeHandler {

    private static final String CLASSNAME = "v-touchkit-combobox";

    private static final int DEFAULT_ITEM_HEIGHT = 30;
    private static final int SMALL_SCREEN_WIDTH_THRESHOLD = 500;

    private int PAGE_LENGTH = 6;
    private int HEADER_HEIGHT = 35;
    private int SEARCH_FIELD_HEIGHT = 30;

    private SelectionPopup popup;
    private Label comboBoxDropDown;
    private boolean hasNext, hasPrev;

    /**
     * A collection of available suggestions (options) as received from the
     * server.
     */
    protected final List<TouchComboBoxOptionState> currentSuggestions = new ArrayList<TouchComboBoxOptionState>();

    protected boolean enabled;
    protected boolean readonly;
    protected boolean allowNewItem;
    protected boolean nullSelectionAllowed;
    protected TouchComboBoxOptionState nullSelectionItemId;

    /**
     * Create new TouchKit ComboBox
     */
    public VTouchComboBox() {

        Element wrapper = Document.get().createDivElement();
        setElement(wrapper);

        comboBoxDropDown = new Label();
        comboBoxDropDown.setStyleName(CLASSNAME + "-drop-down");

        DOM.sinkEvents(comboBoxDropDown.getElement(), Event.ONCLICK);
        DOM.setEventListener(comboBoxDropDown.getElement(), dropDownListener);

        wrapper.appendChild(comboBoxDropDown.getElement());

        Window.addResizeHandler(this);

        if (Window.getClientWidth() < SMALL_SCREEN_WIDTH_THRESHOLD) {
            HEADER_HEIGHT = 42;
        }

        setStyleName(CLASSNAME);
    }

    public void setCurrentSuggestions(
            List<TouchComboBoxOptionState> currentSuggestions) {
        this.currentSuggestions.clear();
        this.currentSuggestions.addAll(currentSuggestions);
        populateOptions();
    }

    public void setSelection(String selection) {
        comboBoxDropDown.setText(selection);
    }

    public void setPageLength(int length) {
        PAGE_LENGTH = length;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
        if (popup != null)
            popup.updateNextButton();
    }

    public void setHasPrev(boolean hasPrev) {
        this.hasPrev = hasPrev;
        if (popup != null)
            popup.updatePrevButton();
    }

    public void setWidth(String width) {
        comboBoxDropDown.setWidth(width);

        int leftOffset = comboBoxDropDown.getOffsetWidth() - 30;
        Style style = comboBoxDropDown.getElement().getStyle();
        style.setProperty("background-position", leftOffset + "px -6px ");
    }

    /**
     * Update popup options
     * 
     * Always show buttons for full page length
     */
    private void populateOptions() {
        if (popup == null) {
            return;
        }
        popup.clearItems();

        int itemHeight = getItemHeight();
        for (TouchComboBoxOptionState option : currentSuggestions) {
            Label itemLabel = createItemLabel(option.caption, itemHeight);

            itemLabel.addClickHandler(new SelectionClickListener(option));
            DOM.sinkEvents(itemLabel.getElement(), Event.ONCLICK);

            popup.addItem(itemLabel);
        }
        if (currentSuggestions.size() < PAGE_LENGTH) {
            for (int i = currentSuggestions.size(); i < PAGE_LENGTH; i++) {
                Label itemLabel = createItemLabel("", itemHeight);
                itemLabel.addStyleName("empty");

                popup.addItem(itemLabel);
            }
        }
    }

    private Label createItemLabel(String caption, int itemHeight) {
        Label itemLabel = new Label(caption);
        itemLabel.setStyleName(CLASSNAME + "-select-item");

        itemLabel.setHeight(itemHeight + "px");
        itemLabel.getElement().getStyle()
                .setPropertyPx("lineHeight", itemHeight);
        return itemLabel;
    }

    private int getItemHeight() {
        int clientHeight = Window.getClientHeight();
        int itemHeight;
        if (clientHeight > (PAGE_LENGTH * DEFAULT_ITEM_HEIGHT)
                && Window.getClientWidth() < SMALL_SCREEN_WIDTH_THRESHOLD) {
            itemHeight = (clientHeight - HEADER_HEIGHT - SEARCH_FIELD_HEIGHT)
                    / PAGE_LENGTH;
        } else {
            itemHeight = DEFAULT_ITEM_HEIGHT;
        }
        return itemHeight;
    }

    /**
     * Input element listener //
     */
    private class SelectionClickListener implements ClickHandler {

        private TouchComboBoxOptionState object;

        public SelectionClickListener(TouchComboBoxOptionState object) {
            super();
            this.object = object;
        }

        @Override
        public void onClick(ClickEvent event) {
            ValueChangeEvent.fire(VTouchComboBox.this, object.getKey());
            if (popup != null) {
                popup.hide();
            }
        }
    };

    private EventListener dropDownListener = new EventListener() {

        @Override
        public void onBrowserEvent(Event event) {
            if (popup == null) {
                popup = new SelectionPopup();
                if (Window.getClientWidth() > SMALL_SCREEN_WIDTH_THRESHOLD) {
                    popup.showNextTo(VTouchComboBox.this);
                }

                populateOptions();
            }
        }
    };

    private class ValueChange implements ValueChangeHandler<String> {

        public void onValueChange(ValueChangeEvent<String> event) {
            ValueChangeEvent.fire(VTouchComboBox.this, event.getValue());
        }
    }

    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<String> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    public HandlerRegistration addPageEventHandler(
            PageEventHandler pageEventHandler) {
        return addHandler(pageEventHandler, PageEvent.getType());
    }

    @Override
    public void onResize(ResizeEvent event) {
        if (popup != null) {
            popup.updateHeight();
            populateOptions();
        }
    };

    protected class SelectionPopup extends VPopover {

        final FlowPanel select = new FlowPanel();
        final HorizontalPanel header = new HorizontalPanel();
        final TextBox filter = TextBox.wrap(createInputElement(Document.get(),
                "search"));
        final FlowPanel content = new FlowPanel();

        Button next, prev;

        public native InputElement createInputElement(Document doc, String type)
        /*-{
            var e = doc.createElement("INPUT");
            e.type = type;
            return e;
        }-*/;

        public SelectionPopup() {
            init();
            setStyleName("v-touchkit-popover");
        }

        private void init() {

            int boxWidth = getBoxWidth();

            updateHeight();

            select.setWidth(boxWidth + "px");
            select.getElement().getStyle().setBackgroundColor("white");
            select.setStyleName("v-touchkit-popover");
            if (Window.getClientWidth() > SMALL_SCREEN_WIDTH_THRESHOLD) {
                select.addStyleName(VTouchComboBox.CLASSNAME);
            }

            filter.setWidth(boxWidth + "px");
            filter.setStyleName("v-touchkit-combobox-popup-filter");
            filter.setValue(comboBoxDropDown.getText());
            filter.getElement().getStyle()
                    .setProperty("-webkit-appearance", "textfield");

            content.setWidth(boxWidth + "px");

            header.setHeight(HEADER_HEIGHT + "px");
            header.setWidth("100%");

            next = new Button("next");
            next.setStyleName("v-touchkit-navbar-right v-touchkit-navbutton-forward");
            next.addClickHandler(new NavigationClickListener(PageEventType.NEXT));

            prev = new Button("prev");
            prev.setStyleName("v-touchkit-navbar-left v-touchkit-navbutton-back");
            prev.addClickHandler(new NavigationClickListener(
                    PageEventType.PREVIOUS));

            updatePrevButton();
            updateNextButton();

            header.add(prev);

            if (Window.getClientWidth() < SMALL_SCREEN_WIDTH_THRESHOLD) {
                final Button close = new Button("x");
                close.setStyleName("v-touchkit-combobox-close-button");
                close.getElement().getStyle()
                        .setProperty("left", (boxWidth / 2 - 12) + "px");
                close.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        hide();
                    }
                });

                header.add(close);
            }

            header.add(next);

            select.add(header);

            filter.addValueChangeHandler(new ValueChange());

            select.add(filter);

            select.add(content);
            setWidget(select);

            if (Window.getClientWidth() < SMALL_SCREEN_WIDTH_THRESHOLD) {
                super.slideIn();
                setPopupPosition(0, 0);
            } else {
                setAutoHideEnabled(true);
            }
            show();
        }

        private int getBoxWidth() {
            if (Window.getClientWidth() < SMALL_SCREEN_WIDTH_THRESHOLD) {
                return Window.getClientWidth();
            }
            return comboBoxDropDown.getOffsetWidth();
        }

        public void updateNextButton() {
            next.setVisible(hasNext);
        }

        public void updatePrevButton() {
            prev.setVisible(hasPrev);
        }

        public void addItem(Label item) {
            content.add(item);
        }

        public void clearItems() {
            content.clear();
        }

        public void updateHeight() {
            int itemHeight = getItemHeight();
            if (itemHeight == DEFAULT_ITEM_HEIGHT) {
                // as items have a border-bottom of 1px
                itemHeight++;
            }

            select.setHeight((HEADER_HEIGHT + SEARCH_FIELD_HEIGHT + (itemHeight * PAGE_LENGTH))
                    + "px");
            content.setHeight((itemHeight * PAGE_LENGTH) + "px");
        }

        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
            clearItems();
            hide();
            VTouchComboBox.this.fireEvent(new PageEvent(PageEventType.CLOSE));
            popup = null;
        };

        private class NavigationClickListener implements ClickHandler {

            private PageEventType eventType;

            public NavigationClickListener(PageEventType eventType) {
                super();
                this.eventType = eventType;
            }

            @Override
            public void onClick(ClickEvent event) {
                VTouchComboBox.this.fireEvent(new PageEvent(eventType));
            }
        };
    }
}
