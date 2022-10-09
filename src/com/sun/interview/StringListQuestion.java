/*
 * $Id$
 *
 * Copyright (c) 2003, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.interview;

import java.util.Map;
import java.util.Vector;

/**
 * A {@link Question question} to which the response is an array of strings.
 */
public abstract class StringListQuestion extends Question {
    private static final String[] EMPTY = {};
    /**
     * The current response for this question.
     */
    protected String[] value;
    /**
     * The default response for this question.
     */
    private String[] defaultValue;
    private boolean duplicatesAllowed = true;

    /**
     * Create a question with a nominated tag.
     *
     * @param interview The interview containing this question.
     * @param tag       A unique tag to identify this specific question.
     */
    protected StringListQuestion(Interview interview, String tag) {
        super(interview, tag);
        clear();
        setDefaultValue(value);
    }

    /**
     * Compare two string arrays for equality.
     *
     * @param s1 the first array to be compared, or null
     * @param s2 the other array to be compared, or null
     * @return true if both parameters are null, or if both are non-null
     * and are element-wise equal.
     * @see #equal(String, String)
     */
    protected static boolean equal(String[] s1, String... s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }

        if (s1.length != s2.length) {
            return false;
        }

        for (int i = 0; i < s1.length; i++) {
            if (!equal(s1[i], s2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two strings for equality.
     *
     * @param s1 the first string to be compared, or null
     * @param s2 the other string to be compared, or null
     * @return true if both parameters are null, or if both are non-null
     * and equal.
     */
    protected static boolean equal(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }

    /**
     * Split a string into a set of newline-separated strings.
     *
     * @param s The string to be split, or null
     * @return an array of strings containing the newline-separated substrings of
     * the argument.
     */
    protected static String[] split(String s) {
        if (s == null) {
            return EMPTY;
        }

        char sep = '\n';

        Vector<String> v = new Vector<>();
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                if (start != -1) {
                    v.add(s.substring(start, i));
                }
                start = -1;
            } else if (start == -1) {
                start = i;
            }
        }
        if (start != -1) {
            v.add(s.substring(start));
        }
        if (v.isEmpty()) {
            return EMPTY;
        }
        return v.toArray(new String[v.size()]);
    }

    /**
     * Get the default response for this question.
     *
     * @return the default response for this question.
     * @see #setDefaultValue
     * @see #clear
     */
    public String[] getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the default response for this question,
     * used by the clear method.
     *
     * @param v the default response for this question.
     * @see #getDefaultValue
     * @see #clear
     */
    public void setDefaultValue(String... v) {
        defaultValue = v;
    }

    /**
     * Check whether or not duplicates should be allowed in the list.
     *
     * @return true if duplicates should be allowed, and false otherwise
     * @see #setDuplicatesAllowed
     */
    public boolean isDuplicatesAllowed() {
        return duplicatesAllowed;
    }

    /**
     * Specify whether or not duplicates should be allowed in the list.
     * By default, duplicates are allowed.
     *
     * @param b true if duplicates should be allowed, and false otherwise
     * @see #isDuplicatesAllowed
     */
    public void setDuplicatesAllowed(boolean b) {
        duplicatesAllowed = b;
    }

    /**
     * Get the current (default or latest) response to this question.
     *
     * @return The current value.
     * @see #setValue
     */
    public String[] getValue() {
        return value;
    }

    @Override
    public void setValue(String s) {
        setValue(s == null ? null : split(s));
    }

    /**
     * Set the current value.
     *
     * @param newValue The value to be set.
     * @see #getValue
     */
    public void setValue(String... newValue) {
        if (newValue != null) {
            for (String aNewValue : newValue) {
                if (aNewValue == null || (aNewValue.contains("\n"))) {
                    throw new IllegalArgumentException();
                }
            }
        }

        if (!equal(newValue, value)) {
            if (newValue == null) {
                value = null;
            } else {
                value = new String[newValue.length];
                System.arraycopy(newValue, 0, value, 0, newValue.length);
            }
            interview.updatePath(this);
            interview.setEdited(true);
        }
    }

    /**
     * Verify this question is on the current path, and if it is,
     * return the current value.
     *
     * @return the current value of this question
     * @throws Interview.NotOnPathFault if this question is not on the
     *                                  current path
     * @see #getValue
     */
    public String[] getValueOnPath()
            throws Interview.NotOnPathFault {
        interview.verifyPathContains(this);
        return getValue();
    }

    /**
     * Get the response to this question as a string.
     *
     * @return a string representing the current response to this question, or null.
     * @see #setValue(String)
     */
    @Override
    public String getStringValue() {
        if (value == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String aValue : value) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            if (aValue != null) {
                sb.append(aValue);
            }
        }

        return sb.toString();
    }

    @Override
    public boolean isValueValid() {
        return true;
    }

    @Override
    public boolean isValueAlwaysValid() {
        return false;
    }

    /**
     * Clear any response to this question, resetting the value
     * back to its initial state.
     */
    @Override
    public void clear() {
        setValue(defaultValue);
    }

    /**
     * Save the value for this question in a dictionary, using
     * the tag as the key.
     *
     * @param data The map in which to save the value for this question.
     */
    @Override
    protected void save(Map<String, String> data) {
        if (value != null) {
            data.put(tag, getStringValue());
        }
    }
}
