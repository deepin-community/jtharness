/*
 * $Id$
 *
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.finder;

import com.sun.javatest.TestDescription;
import com.sun.javatest.TestFinder;
import com.sun.javatest.tool.Preferences;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;

/**
 * A test finder decorator that reads tests from a delegate, and returns the
 * results in the reverse/random order. This is primarily for debugging
 * and testing purposes.
 */
public class TestFinderDecorator extends TestFinder {
    private TestFinder origTestFinder;
    private TestFinder currentTestFinder;

    /**
     * Default constructor
     */
    public TestFinderDecorator(TestFinder testFinder) {
        origTestFinder = testFinder;
        setCurrentTestFinder();

        Preferences.access().addObserver("javatest.executionOrder", (name, newValue) -> setCurrentTestFinder());
    }

    private void setCurrentTestFinder() {
        String value = Preferences.access().getPreference("javatest.executionOrder");

        String sysValue = System.getProperties().getProperty("javatest.executionOrder");
        if (sysValue != null && !sysValue.isEmpty()) {
            value = sysValue;
        }

        if ("reverse".equals(value)) {
            currentTestFinder = new ReverseTestFinder(origTestFinder);
        } else if ("random".equals(value)) {
            currentTestFinder = new RandomTestFinder(origTestFinder);
        } else {
            currentTestFinder = origTestFinder;
        }
    }

    @Override
    public File getRoot() {
        return currentTestFinder.getRoot();
    }

    @Override
    public File getRootDir() {
        return currentTestFinder.getRootDir();
    }

    @Override
    public void read(File file) {
        currentTestFinder.read(file);
    }

    @Override
    public TestDescription[] getTests() {
        return currentTestFinder.getTests();
    }

    @Override
    public File[] getFiles() {
        return currentTestFinder.getFiles();
    }

    @Override
    public Comparator<String> getComparator() {
        return currentTestFinder.getComparator();
    }

    @Override
    public void setComparator(Comparator<String> c) {
        currentTestFinder.setComparator(c);
    }

    public TestFinder getCurrentTestFinder() {
        return currentTestFinder;
    }

    @Override
    public long lastModified(File f) {
        return currentTestFinder.lastModified(f);
    }

    @Override
    public boolean isFolder(File path) {
        return currentTestFinder.isFolder(path);
    }

    @Override
    public Optional<Integer> totalNumberOfTestsInTheSuite() {
        return currentTestFinder.totalNumberOfTestsInTheSuite();
    }

    @Override
    protected void scan(File file) {
        throw new Error("should not be called!");
    }

}

