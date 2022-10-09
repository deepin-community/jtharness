/*
 * $Id$
 *
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.tool;

import com.sun.javatest.util.I18NResourceBundle;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard template for creation of an Action to be used in a Tool.
 */
public abstract class ToolAction implements Action {
    private String name;
    private String desc;
    private Integer mnemonic;
    private Icon icon;
    private Map<String, Object> misc;
    private boolean enabled = true;
    private List<WeakReference<PropertyChangeListener>> listeners = new ArrayList<>();

    /**
     * Construct an action with a specific mnemonic.  This is the
     * non-internationalized version and not recommended.  See
     * {@code Action} for details on the parameters.
     *
     * @param name     Name of this action
     * @param desc     Description of this action
     * @param mnemonic Mnemonic associated with this action
     * @see javax.swing.Action
     */
    public ToolAction(String name, String desc, int mnemonic) {
        this.name = name;
        this.desc = desc;
        this.mnemonic = Integer.valueOf(mnemonic);
        enabled = true;
    }

    /**
     * Construct an internationalized action.
     *
     * @param uif Factory to use for getting strings.
     * @param key Key for retrieving internationalized strings from the
     *            bundle.
     * @see #ToolAction(I18NResourceBundle, String)
     */
    public ToolAction(UIFactory uif, String key) {
        this(uif.getI18NResourceBundle(), key);
    }

    /**
     * Construct an internationalized action.
     *
     * @param uif      Factory to use for getting strings.
     * @param key      Key for retrieving internationalized strings from the
     *                 bundle.
     * @param needIcon True if an icon resource should be associated with
     *                 this action.  Will be retrieved through the uif.  And
     *                 put into the {@code SMALL_ICON} property.
     * @see #ToolAction(I18NResourceBundle, String)
     * @see javax.swing.Action#SMALL_ICON
     */
    public ToolAction(UIFactory uif, String key, boolean needIcon) {
        this(uif, key);
        if (needIcon) {
            putValue(Action.SMALL_ICON, uif.createIcon(key));
        }
    }

    /**
     * Construct an internationalized action.
     * The resources used are:
     * <table><caption></caption>
     * <tr><td><i>uiKey</i>.act  <td>the name for the button
     * <tr><td><i>uiKey</i>.tip  <td>the tool tip for the action
     * <tr><td><i>uiKey</i>.mne  <td>mnemonic for this action
     * </table>
     *
     * @param i18n Resource bundle to use when getting action properties
     * @param key  Key for retrieving internationalized strings from the
     *             bundle.
     */
    public ToolAction(I18NResourceBundle i18n, String key) {
        this(i18n.getString(key + ".act"),
                i18n.getString(key + ".tip"),
                getMnemonic(i18n, key + ".mne"));
    }

    private static int getMnemonic(I18NResourceBundle i18n, String key) {
        String keyString = i18n.getString(key);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyString);
        return keyStroke == null ? 0 : keyStroke.getKeyCode();
    }

    private static boolean equal(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Gets one of this object's properties using the associated key.
     *
     * @param key the key of the property to be returned
     * @return the value of the property with the given key
     * @see #putValue
     */
    @Override
    public Object getValue(String key) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (key.equals(NAME)) {
            return name;
        } else if (key.equals(SHORT_DESCRIPTION)) {
            return desc;
        } else if (key.equals(MNEMONIC_KEY)) {
            return mnemonic;
        } else if (key.equals(SMALL_ICON)) {
            return icon;
        } else {
            return misc == null ? null : misc.get(key);
        }
    }

    /**
     * Sets one of this object's properties using the associated key.
     * If the value has changed, a {@code PropertyChangeEvent} is sent
     * to listeners.
     *
     * @param key    the key of the property to be stored
     * @param newVal the new value for the property
     */
    @Override
    public void putValue(String key, Object newVal) {
        Object oldVal;

        if (key.equals(NAME)) {
            if (equal(newVal, name)) {
                return;
            }
            oldVal = name;
            name = (String) newVal;
        } else if (key.equals(SHORT_DESCRIPTION)) {
            if (equal(newVal, desc)) {
                return;
            }
            oldVal = desc;
            desc = (String) newVal;
        } else if (key.equals(MNEMONIC_KEY)) {
            if (equal(newVal, mnemonic)) {
                return;
            }
            oldVal = mnemonic;
            mnemonic = (Integer) newVal;
        } else if (key.equals(SMALL_ICON)) {
            if (equal(newVal, icon)) {
                return;
            }
            oldVal = icon;
            icon = (Icon) newVal;
        } else {
            if (misc == null) {
                misc = new HashMap<>();
            }
            oldVal = misc.get(key);
            if (equal(newVal, oldVal)) {
                return;
            }
            misc.put(key, newVal);
        }
        firePropertyChangeEvent(key, oldVal, newVal);
    }

    @Override
    public boolean isEnabled() {
        // no need to synchronize just to read a single simple boolean
        return enabled;
    }

    @Override
    public void setEnabled(boolean newVal) {
        if (enabled == newVal) {
            return;
        }

        boolean oldVal = enabled;
        enabled = newVal;

        if (listeners.size() > 0) {
            firePropertyChangeEvent("enabled", Boolean.valueOf(oldVal), Boolean.valueOf(newVal));
        }
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(new WeakReference<>(listener));
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        WeakReference<PropertyChangeListener> found = null;
        for (WeakReference<PropertyChangeListener> reference : listeners) {
            if (reference.get() == listener) {
                found = reference;
                break;
            }
        }
        if (found != null) {
            listeners.remove(found);
        }
    }

    private void firePropertyChangeEvent(String name, Object oldVal, Object newVal) {
        PropertyChangeEvent ev = null; // lazy create event if needed
        if (listeners.size() > 0) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                PropertyChangeListener pcl = listeners.get(i).get();
                if (pcl != null) {
                    if (ev == null) {
                        ev = new PropertyChangeEvent(this, name, oldVal, newVal);
                    }
                    pcl.propertyChange(ev);
                }
            }
        }
    }
}
