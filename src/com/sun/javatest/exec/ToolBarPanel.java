/*
 * $Id$
 *
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * The Panel which holds standard and custom toolbars
 */
class ToolBarPanel extends JPanel {

    public static final String PB_PROP_NAME = "PAINT_BORDER";
    private ToolBarLayout theLayout = new ToolBarLayout();
    private BevelBorder b = new BevelBorder(BevelBorder.RAISED);

    /**
     * Creates a new panel.
     */
    ToolBarPanel() {
        setLayout(theLayout);
        setName("toolbarPanel");
    }

    /**
     * Paints the panel.
     * Invoked by Swing to draw components.
     *
     * @param g - the Graphics context in which to paint
     **/
    @Override
    public void paint(java.awt.Graphics g) {
        int indent = 0;
        super.paint(g);
        int[] separators = theLayout.getRowPositions();
        Color oldColor = g.getColor();
        for (int separator : separators) {
            g.setColor(b.getShadowInnerColor(this));
            // x1, y1, x2, y2
            g.drawLine(indent, separator, getWidth() - indent, separator);
            g.setColor(b.getHighlightInnerColor(this));
            g.drawLine(indent, separator + 1, getWidth() - indent, separator + 1);
        }

        g.setColor(oldColor);
    }

    /**
     * ToolBarLayout lays out the panel and calculates positions for
     * horisontal separators
     **/
    static class ToolBarLayout extends FlowLayout {

        private int[] separators = new int[0];

        public ToolBarLayout() {
            super(FlowLayout.LEFT, 0, 0);
            setHgap(10);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                int realW = target.getSize().width;
                int commponentsPerRow = 0;
                List<Integer> hs = new ArrayList<>();
                if (realW == 0) {
                    return super.preferredLayoutSize(target);
                }
                Dimension dim = new Dimension(0, 0);
                int nmembers = target.getComponentCount();
                boolean firstVisibleComponent = true;
                Insets insets = target.getInsets();
                int maxWidth = 0;
                int maxHeight = 0;
                int row = 1;
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    Dimension d = m.getPreferredSize();
                    if (m.isVisible()) {
                        dim.height = Math.max(dim.height, d.height);
                        commponentsPerRow++;
                        if (firstVisibleComponent) {
                            firstVisibleComponent = false;
                        } else {
                            dim.width += getHgap();
                        }
                        // width of a current row can be
                        int newFullWidth = dim.width + d.width + insets.left + insets.right + getHgap() * 2;
                        if (newFullWidth < realW || commponentsPerRow == 1) {
                            // add to the row
                            dim.width += d.width;
                            maxHeight = Math.max(maxHeight, d.height);
                        } else {
                            // start new row
                            row++;
                            hs.add(Integer.valueOf(dim.height));
                            dim.height += maxHeight + getVgap();
                            maxHeight = 0;
                            // this is bug
                            // it calculates height of a new row by height of first component
                            maxWidth = Math.max(maxWidth, dim.width);
                            maxHeight = Math.max(maxHeight, d.height);
                            dim.width = d.width;
                            firstVisibleComponent = true;
                            commponentsPerRow = 1;
                        }
                        if (m instanceof JComponent) {
                            ((JComponent) m).putClientProperty(PB_PROP_NAME, Boolean.valueOf(!firstVisibleComponent));
                        }
                    }
                }
                dim.width = Math.max(maxWidth, dim.width);
                dim.width += insets.left + insets.right + getHgap() * 2;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                // fill in separators between rows
                separators = new int[hs.size()];
                for (int i = 0; i < hs.size(); i++) {
                    separators[i] = hs.get(i).intValue();
                }
                return dim;
            }
        }

        synchronized int[] getRowPositions() {
            return separators;
        }
    }

}



