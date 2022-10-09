/*
 * $Id$
 *
 * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A {@link Question question} to which the response is a string.
 */
public abstract class StringQuestion extends Question {
    /**
     * The current response for this question.
     */
    protected String value;
    /**
     * Suggested values for this question.
     */
    protected String[] suggestions;
    /**
     * The nominal maximum length for the string.
     */
    protected int nominalMaxLength;
    /**
     * The default response for this question.
     */
    private String defaultValue;

    /**
     * Create a question with a nominated tag.
     *
     * @param interview The interview containing this question.
     * @param tag       A unique tag to identify this specific question.
     */
    protected StringQuestion(Interview interview, String tag) {
        super(interview, tag);
        clear();
        setDefaultValue(value);
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
     * Get the default response for this question.
     *
     * @return the default response for this question.
     * @see #setDefaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the default response for this question,
     * used by the clear method.
     *
     * @param v the default response for this question.
     * @see #getDefaultValue
     */
    public void setDefaultValue(String v) {
        defaultValue = v;
    }

    /**
     * Get the current (default or latest) response to this question.
     *
     * @return The current value.
     * @see #setValue
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the current value.
     *
     * @param newValue The value to be set.
     * @see #getValue
     */
    @Override
    public void setValue(String newValue) {
        String oldValue = value;
        value = newValue == null ? null : newValue.trim();
        if (!equal(value, oldValue)) {
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
    public String getValueOnPath()
            throws Interview.NotOnPathFault {
        interview.verifyPathContains(this);
        return getValue();
    }

    @Override
    public String getStringValue() {
        return value;
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
     * Get the nominal maximum length for the string.
     *
     * @return the nominal maximum length for the string.
     * @see #setNominalMaxLength
     */
    public int getNominalMaxLength() {
        return nominalMaxLength;
    }

    /**
     * Set the expected maximum length for the string.
     *
     * @param nominalMaxLength the nominal maximum length for the string.
     * @see #getNominalMaxLength
     */
    public void setNominalMaxLength(int nominalMaxLength) {
        this.nominalMaxLength = nominalMaxLength;
    }

    /**
     * Get the suggested responses to this question, or null if none.
     *
     * @return The suggestions.
     * @see #setSuggestions
     */
    public String[] getSuggestions() {
        return suggestions;
    }

    /**
     * Set the set of suggested responses.
     *
     * @param newSuggestions The values to be set, or null if none
     * @throws IllegalArgumentException if any of the values in the array
     *                                  are null
     * @see #getSuggestions
     */
    public void setSuggestions(String... newSuggestions) {
        if (newSuggestions != null) {
            for (String newSuggestion : newSuggestions) {
                if (newSuggestion == null) {
                    throw new IllegalArgumentException();
                }
            }
        }

        suggestions = newSuggestions;
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
            data.put(tag, value);
        }
    }
}
