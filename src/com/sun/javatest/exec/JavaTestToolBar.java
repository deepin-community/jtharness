/*
 * $Id$
 *
 * Copyright (c) 2006, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.javatest.exec;

import com.sun.javatest.tool.Preferences;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The custom toolbar.
 */
public class JavaTestToolBar extends JToolBar {

    final static String USER_TOOLBAR_PREF = "JavaTestToolBar.toolbar_";
    private boolean inMenu = false;
    private ResourceBundle theBundle;
    private String id;
    private Map<String, Object> state = new HashMap<>();

    /**
     * Creates using specified ResourceBundle a new tool bar with
     * the specified menu controlled state.
     *
     * @param bundle         a resource bundle used to obtain the resources for the toolbar.
     * @param resourceID     String ID should be unique within the instance of the test suite.
     *                       The ID is never visible to the user. The ID should be a short,
     *                       alphanumeric (Latin) string. It is automatically used to retrieve the toolbar
     *                       name and description from the resource bundle.
     *                       Can't be null and empty string.
     * @param menuControlled - true to make View/Toolbars/menu for the toolbar.
     **/
    public JavaTestToolBar(ResourceBundle bundle, String resourceID, boolean menuControlled) {
        super();
        theBundle = bundle;
        id = resourceID;
        inMenu = menuControlled;
        setUpStyle();
    }

    /**
     * Creates using specified ResourceBundle a new menu controlled tool bar.
     *
     * @param bundle     a resource bundle used to obtain the resources for the toolbar.
     * @param resourceID String ID should be unique within the instance of the test suite.
     *                   The ID is never visible to the user. The ID should be a short,
     *                   alphanumeric (Latin) string. It is automatically used to retrieve the toolbar
     *                   name and description from the resource bundle.
     *                   Can't be null and empty string.
     **/
    public JavaTestToolBar(ResourceBundle bundle, String resourceID) {
        this(bundle, resourceID, true);
    }

    /**
     * Get the identification string for this toolbar.
     *
     * @return the string ID for the tool bar.
     **/
    public String getId() {
        return id;
    }

    /**
     * Get the long description of this toolbar's purpose.
     * May be multiple sentences if desired. This is automatically retrieved from
     * the supplied resource bundle by combining it with the toolbar ID (getId()),
     * e.g. it will try to retrieve getId().tb.desc from the resource bundle.
     *
     * @return the long description for the tool bar.
     **/
    public String getDescription() {
        return theBundle.getString(getId() + ".tb.desc");
    }

    /**
     * Get the short name of this toolbar. Would be used in places such as a toolbar
     * selector drop-down for the user, so it should be kept to one or two words.
     * This is automatically retrieved from the supplied resource bundle by
     * combining it with the toolbar ID (getId()), e.g. it will try to retrieve
     * getId().tb.name from the resource bundle.
     *
     * @return the short for the tool bar.
     **/
    @Override
    public String getName() {
        return theBundle.getString(getId() + ".tb.name");
    }

    /**
     * Determines whether this tool bar should be controlled from view menu.
     *
     * @return true if this tool bar is menu controlled.
     **/
    public boolean isMenuControlled() {
        return inMenu;
    }

    /**
     * Shows or hides this component depending on the value of parameter visible.
     * visible - true to make the component visible; false to make it invisible
     **/
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        saveVisibleState(visible);
    }

    void setVisibleNoStateAffect(boolean visible) {
        super.setVisible(visible);
    }

    /**
     * Reads toolbar visible state from user preferences
     **/
    boolean readVisibleState() {
        Object o = state.get(USER_TOOLBAR_PREF + getId());
        if (o instanceof String) {
            return "true".equals(o);
        }
        return true;
    }

    /**
     * Stores toolbar visible state as user preferences
     **/
    void saveVisibleState(boolean visible) {
        state.put(USER_TOOLBAR_PREF + getId(), Boolean.toString(visible));
    }

    void save(Map<String, Object> map) {
        map.putAll(state);
    }

    /**
     * Accepts Map with parameters from ExecTool
     **/
    void load(Map<String, Object> map) {
        for (String okey : map.keySet()) {
            String tbKey = USER_TOOLBAR_PREF + getId();
            if (okey.contains(tbKey)) {
                state.put(okey, map.get(okey));
            }
        }

        Preferences prefs = Preferences.access();
        String visPref = prefs.getPreference(ExecTool.TOOLBAR_PREF);
        boolean generalVisibleState = "true".equals(visPref);
        setVisibleNoStateAffect(generalVisibleState && readVisibleState());
    }

    /**
     * Sets common style for a toolbar
     **/
    private void setUpStyle() {
        setFloatable(false);
        setRollover(true);
        setBorder(new ToolBarBorder());
    }

    /**
     * Sets common style for a toolbar with left border
     **/
    static class ToolBarBorder extends BevelBorder {

        private int lIn = 5;

        public ToolBarBorder() {
            super(BevelBorder.RAISED);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, lIn);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.right = lIn;
            insets.top = insets.left = insets.bottom = 2;
            return insets;
        }

        @Override
        protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
                                        int width, int height) {
            int gap = 4;
            int hlen = 12;

            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                Boolean paint = (Boolean) jc.getClientProperty(ToolBarPanel.PB_PROP_NAME);
                if (paint != null && paint.booleanValue()) {
                    Color oldColor = g.getColor();
                    int h = height;
                    int w = width;

                    if (h > hlen * 2) {
                        int mid = y + (h / 2);
                        y = mid - hlen;
                        h = hlen * 2;
                    }

                    g.setColor(getShadowInnerColor(c));
                    g.drawLine(x, y + gap, x, y + h - gap - 1);

                    g.setColor(getHighlightInnerColor(c));
                    g.drawLine(x + 1, y + gap, x + 1, y + h - gap - 1);
                }
            }
        }

    }
}
