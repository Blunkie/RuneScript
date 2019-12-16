/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.editor.ui.status;

import lombok.Getter;
import me.waliedyassen.runescript.editor.property.impl.StringProperty;

import javax.swing.*;

/**
 * The RuneScript Editor status bar.
 *
 * @author Walied K. Yassen
 */
public final class StatusBar extends JPanel {

    /**
     * The current status text of the status bar.
     */
    @Getter
    private final StringProperty text = new StringProperty();

    /**
     * Constructs a new {@link StatusBar} type object instance.
     */
    public StatusBar() {
        setupLabel();
        setupMemory();
    }

    /**
     * Sets up the label at teh start of the status bar.
     */
    private void setupLabel() {
        var label = new JLabel();
        text.addListener(label::setText);
        add(label);
    }

    /**
     * Sets-up the memory bar at the ned of the status bar.
     */
    private void setupMemory() {
        // TODO:
    }
}