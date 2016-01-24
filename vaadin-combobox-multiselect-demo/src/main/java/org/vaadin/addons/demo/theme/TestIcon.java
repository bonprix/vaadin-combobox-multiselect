package org.vaadin.addons.demo.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;

public class TestIcon {

    int iconCount = 0;

    public TestIcon(int startIndex) {
        iconCount = startIndex;
    }

    public Resource get() {
        return get(false, 32);
    }

    public Resource get(boolean isImage) {
        return get(isImage, 32);
    }

    public Resource get(boolean isImage, int imageSize) {
        if (!isImage) {
            if (++iconCount >= ICONS.size()) {
                iconCount = 0;
            }
            return ICONS.get(iconCount);
        }
        return new ThemeResource("../runo/icons/" + imageSize + "/document.png");
    }

    static List<FontAwesome> ICONS = Collections.unmodifiableList(Arrays.asList(FontAwesome.values()));
}
