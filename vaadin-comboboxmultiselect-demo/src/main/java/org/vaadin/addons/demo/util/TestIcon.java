/*
 * Copyright 2000-2013 Vaadin Ltd.
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
package org.vaadin.addons.demo.util;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;

/**
 *
 * @since
 * @author Vaadin Ltd
 */
public class TestIcon {

	int iconCount = 0;

	public TestIcon(int startIndex) {
		this.iconCount = startIndex;
	}

	public Resource get() {
		return get(false, 32);
	}

	public Resource get(boolean isImage) {
		return get(isImage, 32);
	}

	public Resource get(boolean isImage, int imageSize) {
		if (!isImage) {
			if (++this.iconCount >= ICONS.size()) {
				this.iconCount = 0;
			}
			return ICONS.get(this.iconCount);
		}
		return new ThemeResource("../runo/icons/" + imageSize + "/document.png");
	}

	static List<VaadinIcons> ICONS = new ArrayList<>();
	static {
		for (VaadinIcons vaadinIcon : VaadinIcons.values()) {
			ICONS.add(vaadinIcon);
		}
	}
}