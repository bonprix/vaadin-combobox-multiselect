package org.vaadin.addons.demo.theme;

import com.vaadin.v7.data.Container.Hierarchical;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.data.util.IndexedContainer;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.Resource;
import com.vaadin.ui.Notification;

public class ValoThemeUI {

    @SuppressWarnings("serial")
	static Handler actionHandler = new Handler() {
        private final Action ACTION_ONE = new Action("Action One");
        private final Action ACTION_TWO = new Action("Action Two");
        private final Action ACTION_THREE = new Action("Action Three");
        private final Action[] ACTIONS = new Action[] { ACTION_ONE, ACTION_TWO, ACTION_THREE };

        @Override
        public void handleAction(final Action action, final Object sender, final Object target) {
            Notification.show(action.getCaption());
        }

        @Override
        public Action[] getActions(final Object target, final Object sender) {
            return ACTIONS;
        }
    };

    static Handler getActionHandler() {
        return actionHandler;
    }

    public static final String CAPTION_PROPERTY = "caption";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String ICON_PROPERTY = "icon";
    public static final String INDEX_PROPERTY = "index";

    @SuppressWarnings("unchecked")
	public static IndexedContainer generateContainer(final int size, final boolean hierarchical) {
        final TestIcon testIcon = new TestIcon(90);
        final IndexedContainer container = hierarchical ? new HierarchicalContainer() : new IndexedContainer();
        final StringGenerator sg = new StringGenerator();
        container.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        container.addContainerProperty(ICON_PROPERTY, Resource.class, null);
        container.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        container.addContainerProperty(DESCRIPTION_PROPERTY, String.class, null);
        for (int i = 1; i < size + 1; i++) {
            final Item item = container.addItem(i);
            item.getItemProperty(CAPTION_PROPERTY)
                .setValue(sg.nextString(true) + " " + sg.nextString(false));
            item.getItemProperty(INDEX_PROPERTY)
                .setValue(i);
            item.getItemProperty(DESCRIPTION_PROPERTY)
                .setValue(sg.nextString(true) + " " + sg.nextString(false) + " " + sg.nextString(false));
            item.getItemProperty(ICON_PROPERTY)
                .setValue(testIcon.get());
        }
        container.getItem(container.getIdByIndex(0))
                 .getItemProperty(ICON_PROPERTY)
                 .setValue(testIcon.get());

        if (hierarchical) {
            for (int i = 1; i < size + 1; i++) {
                for (int j = 1; j < 5; j++) {
                    final String id = i + " -> " + j;
                    Item child = container.addItem(id);
                    child.getItemProperty(CAPTION_PROPERTY)
                         .setValue(sg.nextString(true) + " " + sg.nextString(false));
                    child.getItemProperty(ICON_PROPERTY)
                         .setValue(testIcon.get());
                    // ((Hierarchical) container).setChildrenAllowed(id, false);
                    ((Hierarchical) container).setParent(id, i);

                    for (int k = 1; k < 6; k++) {
                        final String id2 = id + " -> " + k;
                        child = container.addItem(id2);
                        child.getItemProperty(CAPTION_PROPERTY)
                             .setValue(sg.nextString(true) + " " + sg.nextString(false));
                        child.getItemProperty(ICON_PROPERTY)
                             .setValue(testIcon.get());
                        // ((Hierarchical) container)
                        // .setChildrenAllowed(id, false);
                        ((Hierarchical) container).setParent(id2, id);

                        for (int l = 1; l < 5; l++) {
                            final String id3 = id2 + " -> " + l;
                            child = container.addItem(id3);
                            child.getItemProperty(CAPTION_PROPERTY)
                                 .setValue(sg.nextString(true) + " " + sg.nextString(false));
                            child.getItemProperty(ICON_PROPERTY)
                                 .setValue(testIcon.get());
                            // ((Hierarchical) container)
                            // .setChildrenAllowed(id, false);
                            ((Hierarchical) container).setParent(id3, id2);
                        }
                    }
                }
            }
        }
        return container;
    }
}
