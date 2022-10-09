/*
 * $Id$
 *
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.demoapi.lists;

import java.util.Objects;

/**
 * Sorted linked lists of objects.
 * A list is represented by a series of Entry objects, each containing
 * an item of client data, and a link to the next entry in the list.
 * The entries are sorted according to a Comparator used to compare the
 * items of client data.
 */

// Note: this class is purely provided to be the basis of some
// examples for writing a testsuite. The code has been written with
// simplicity in mind, rather than efficiency, and may contain
// deliberate coding errors. For proper support for linked lists,
// see the classes in java.util.

public class SortedList
{
    /**
     * An entry in a LinkedList, containing client data and a link to the next entry.
     */
    public class Entry {
        /**
         * Create an entry to be put in a LinkedList.
         * Entries are not created directly by the client:
         * they are created automatically by the methods that
         * insert data into the list as a whole.
         * @param data Client data to be stored in this entry
         * @param next The next entry to appear after this one.
         * @see #insert
         * @see ##append
         * @see #insertAfter
         */
        Entry(Object data, Entry next) {
            this.data = data;
            this.next = next;
        }

        /**
         * Get the client data in this entry in the list
         * @return the client data in this entry in the list
         */
        public Object getData() {
            return data;
        }

        /**
         * Get the next entry in the list, if any.
         * @return the next entry in the list,
         * or null if this is the last entry.
         */
        public Entry getNext() {
            return next;
        }

        /**
         * Insert a new entry in the list, after this one.
         * @param data the client data to be stored in this entry
         */
        public void insertAfter(Object data) {
            next = new Entry(data, next);
        }

        /**
         * Remove this entry from the list.
         * @return the next entry in the list, or null if none
         * @throws IllegalStateException if this entry is not in the list
         * in which it was created: for example, if it has already been removed.
         */
        public Entry remove() {
            for (Entry e = first, prev = null; e != null; prev = e, e = e.next) {
                if (e == this) {
                    // update the link to this entry
                    if (prev == null)
                        first = e.next;
                    else
                        prev.next = e.next;

                    return next;
                }
            }

            // not found
            throw new IllegalStateException();
        }

        Object data;
        Entry next;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return Objects.equals(data, entry.data) &&
                    Objects.equals(next, entry.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, next);
        }
    };

    public static interface Comparator {
        int compare(Object a, Object b);
    };

    /**
     * Create an empty list.
     */
    public SortedList(Comparator comp, boolean ignoreDuplicates) {
        this.comp = comp;
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * Determine if a linked list is empty.
     * @return true if the list has no entries, and false otherwise.
     */
    public boolean isEmpty() {
        return (first == null);
    }

    /**
     * Determine if the list contains an entry with a specific item of
     * client data.
     * @return true if the list contains an entry that matches the
     * given client data, and false otherwise.
     */
    public boolean contains(Object data) {
        for (Entry e = first; e != null; e = e.next) {
            if (comp.compare(e.data, data) == 0)
                return true;
        }
        return false;
    }

    /**
     * Get the first entry in the list.
     * @return the first entry in the list
     * @see Entry#getNext
     */
    public Entry getFirst() {
        return first;
    }

    /**
     * Insert a new entry containing the specified client data
     * at the beginning of the list.
     * @param data the client data for the new entry
     */
    public boolean insert(Object data) {
        if (first == null) {
            first = new Entry(data, null);
            return true;
        }

        Entry prev = null;
        for (Entry e = first; e != null; prev = e, e = e.next) {
            int c = comp.compare(data, e.data);
            if (c > 0)
                continue;
            else if (c == 0 && ignoreDuplicates)
                return false;
            else
                break;
        }

        // found where to insert
        if (prev == null)
            first = new Entry(data, first);
        else
            prev.next = new Entry(data, prev.next);

        return true;
    }

    /**
     * Remove the first entry from the list that contains the
     * specified client data.
     * @param data The client data indicating the entry to be removed
     * @return true if an entry was found and removed that contained
     * the specified client data, and false otherwise.
     */
    public boolean remove(Object data) {
        for (Entry e = first, prev = null; e != null; prev = e, e = e.next) {
            if (comp.compare(e.data, data) == 0) {
                // update the pointer to this cell
                if (prev == null)
                    first = e.next;
                else
                    prev.next = e.next;

                return true;
            }
        }

        // not found
        return false;
    }

    /**
     * Check if the contents of this list match another.
     * @return true if the other object is a linked list, and corresponding
     * entries in the two lists are either both null, or are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SortedList that = (SortedList) o;
        return ignoreDuplicates == that.ignoreDuplicates &&
                Objects.equals(first, that.first) &&
                Objects.equals(comp, that.comp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, comp, ignoreDuplicates);
    }

    /**
     * Return a string representation of the list.
     * @return a string representation of the list
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SortedList[");
        for (Entry p = first; p != null; p = p.next ) {
            if (p != first)
                sb.append(",");
            sb.append(String.valueOf(p.data));
        }
        sb.append("]");
        return sb.toString();
    }

    private Entry first;
    private Comparator comp;
    private boolean ignoreDuplicates;
}
