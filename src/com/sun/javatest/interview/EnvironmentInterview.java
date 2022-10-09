/*
 * $Id$
 *
 * Copyright (c) 2001, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.interview;

import com.sun.interview.ErrorQuestion;
import com.sun.interview.ExtensionFileFilter;
import com.sun.interview.FileListQuestion;
import com.sun.interview.FinalQuestion;
import com.sun.interview.Interview;
import com.sun.interview.Question;
import com.sun.interview.StringQuestion;
import com.sun.javatest.InterviewParameters;
import com.sun.javatest.Parameters;
import com.sun.javatest.TestEnvContext;
import com.sun.javatest.TestEnvironment;
import com.sun.javatest.TestSuite;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;


/**
 * This interview collects the environment parameter, by means of environment (jte) files
 * and an environment name. It is normally used as one of a series of sub-interviews
 * that collect the parameter information for a test run. It is suitable for use with
 * legacy test suites that still rely on environments being provided with .jte files;
 * more sophisticated interviews should create a custom interview that collects the
 * environment data directly.
 */
public class EnvironmentInterview
        extends Interview
        implements Parameters.LegacyEnvParameters {
    private TestEnvContext cachedEnvTable;
    private File[] cachedEnvTable_absFiles;
    private String cachedEnvTableError;
    private ErrorQuestion qNoEnvs = new ErrorQuestion(this, "noEnvs");
    private ErrorQuestion qEnvTableError = new ErrorQuestion(this, "envTableError") {
        @Override
        protected Object[] getTextArgs() {
            return new Object[]{cachedEnvTableError};
        }
    };
    private TestEnvironment cachedEnv;
    private TestEnvContext cachedEnv_envTable;


    //----------------------------------------------------------------------------
    //
    // Env files
    private String cachedEnv_envName;
    private Question cachedEnvError;
    private Object[] cachedEnvErrorArgs;
    private ErrorQuestion qEnvError = new ErrorQuestion(this, "envError") {
        @Override
        protected Object[] getTextArgs() {
            return cachedEnvErrorArgs;
        }
    };
    private ErrorQuestion qEnvNotFound = new ErrorQuestion(this, "envNotFound") {
        @Override
        protected Object[] getTextArgs() {
            return cachedEnvErrorArgs;
        }
    };
    private ErrorQuestion qEnvUndefinedEntry = new ErrorQuestion(this, "envUndefinedEntry") {
        @Override
        protected Object[] getTextArgs() {
            return cachedEnvErrorArgs;
        }
    };


    //----------------------------------------------------------------------------
    //
    // No Env
    private Question qEnd = new FinalQuestion(this);


    //----------------------------------------------------------------------------
    //
    // Env Table Error
    private InterviewParameters parent;


    //----------------------------------------------------------------------------
    //
    // Env
    private StringQuestion qEnv = new StringQuestion(this, "env") {
        private TestEnvContext cachedEnvTable;

        @Override
        public String[] getSuggestions() {
            // ensure the choices are up to date with envTable;
            // note that setting choices may smash the current value
            // if it's not a valid choice in the new set
            TestEnvContext envTable = getEnvTable();
            if (envTable != cachedEnvTable) {
                String[] envNames;
                if (envTable == null) {
                    envNames = new String[0];
                } else {
                    String[] names = envTable.getEnvMenuNames();
                    Arrays.sort(names);
                    envNames = names;
                }
                setSuggestions(envNames);
                cachedEnvTable = envTable;
            }
            return super.getSuggestions();
        }

        @Override
        protected Question getNext() {
            if (value == null) {
                return null;
            } else {
                updateCachedEnv();
                if (cachedEnv == null) {
                    return cachedEnvError;
                } else {
                    return qEnd;
                }
            }
        }
    };
    private FileListQuestion qEnvFiles = new FileListQuestion(this, "envFiles") {
        {
            // I18N...
            setFilter(new ExtensionFileFilter(".jte", "Environment File"));
            setDuplicatesAllowed(false);
        }

        @Override
        public File getBaseDirectory() {
            TestSuite ts = parent.getTestSuite();
            if (ts == null) {
                return null;
            } else {
                File r = ts.getRoot();
                return r.isDirectory() ? r : r.getParentFile();
            }
        }

        @Override
        protected Question getNext() {
            updateCachedEnvTable();
            if (cachedEnvTableError != null) {
                return qEnvTableError;
            } else if (cachedEnvTable == null || cachedEnvTable.getEnvNames().length == 0) {
                return qNoEnvs;
            } else {
                return qEnv;
            }
        }
    };

    /**
     * Create an interview.
     *
     * @param parent The parent interview of which this is a child.
     * @throws Interview.Fault if there is a problem while creating the interview.
     */
    public EnvironmentInterview(InterviewParameters parent)
            throws Interview.Fault {
        super(parent, "environment");
        this.parent = parent;
        setResourceBundle("i18n");
        setHelpSet("/com/sun/javatest/moreInfo/moreInfo.hs");
        setFirstQuestion(qEnvFiles);
    }

    private static File[] getAbsoluteFiles(File baseDir, File... files) {
        if (files == null) {
            return null;
        }

        if (baseDir == null) {
            return files;
        }

        boolean allAbsolute = true;
        for (int i = 0; i < files.length && allAbsolute; i++) {
            allAbsolute = files[i].isAbsolute();
        }

        if (allAbsolute) {
            return files;
        }

        File[] absoluteFiles = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            absoluteFiles[i] = f.isAbsolute() ? f : new File(baseDir, f.getPath());
        }

        return absoluteFiles;
    }

    private static boolean equal(File f1, File f2) {
        return f1 == null ? f2 == null : f1.equals(f2);
    }

    private static boolean equal(File[] f1, File... f2) {
        if (f1 == null || f2 == null) {
            return f1 == f2;
        }

        if (f1.length != f2.length) {
            return false;
        }

        for (int i = 0; i < f1.length; i++) {
            if (!equal(f1[i], f2[i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean equal(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }


    //----------------------------------------------------------------------------
    //
    // Env Error

    /**
     * Get the environment files specified in the interview.
     *
     * @return the list of files specified in the interview
     * @see #setEnvFiles
     */
    @Override
    public File[] getEnvFiles() {
        return qEnvFiles.getValue();
    }

    //----------------------------------------------------------------------------
    //
    // Env Not Found

    /**
     * Set the environment files for the interview.
     *
     * @param files the environment files for the interview
     * @see #getEnvFiles
     */
    @Override
    public void setEnvFiles(File... files) {
        qEnvFiles.setValue(files);
    }

    //----------------------------------------------------------------------------
    //
    // Env Undefined Entry

    @Override
    public File[] getAbsoluteEnvFiles() {
        TestSuite ts = parent.getTestSuite();
        File tsRootDir = ts == null ? null : ts.getRootDir();
        return getAbsoluteFiles(tsRootDir, getEnvFiles());
    }

    //----------------------------------------------------------------------------
    //
    // End

    /**
     * Get the environment name specified in the interview.
     *
     * @return the environment name specified in the interview
     * @see #setEnvName
     */
    @Override
    public String getEnvName() {
        return qEnv.getValue();
    }

    //---------------------------------------------------------------------

    /**
     * Set the environment name for the interview.
     *
     * @param name the environment name for the interview
     * @see #getEnvName
     */
    @Override
    public void setEnvName(String name) {
        qEnv.setValue(name);
    }

    //----------------------------------------------------------------------------

    /**
     * Get the environment specified by the environment files and environment name,
     * or null, if it cannot be determined.
     *
     * @return the environment determined by the interview, or null if it cannot be determined.
     * @see #getEnvFiles
     * @see #getEnvName
     */
    @Override
    public TestEnvironment getEnv() {
        updateCachedEnv();
        return cachedEnv;
    }

    private TestEnvContext getEnvTable() {
        updateCachedEnvTable();
        return cachedEnvTable;
    }

    private void updateCachedEnvTable() {
        File[] absFiles = getAbsoluteEnvFiles();
        if (!equal(cachedEnvTable_absFiles, absFiles)) {
            try {
                cachedEnvTable = new TestEnvContext(absFiles);
                cachedEnvTableError = null;
            } catch (TestEnvContext.Fault e) {
                cachedEnvTable = null;
                cachedEnvTableError = e.getMessage();
            }
            cachedEnvTable_absFiles = absFiles;
        }
    }


    //--------------------------------------------------------

    private void updateCachedEnv() {
        TestEnvContext envTable = getEnvTable();
        String envName = getEnvName();
        if (cachedEnv_envTable != envTable || !equal(cachedEnv_envName, envName)) {
            try {
                if (envTable == null || envName == null || envName.isEmpty()) {
                    cachedEnv = null;
                    cachedEnvError = null;
                } else {
                    cachedEnv = envTable.getEnv(envName);
                    if (cachedEnv == null) {
                        cachedEnvError = qEnvNotFound;
                        cachedEnvErrorArgs = new Object[]{envName};
                    } else {
                        // verify all entries defined
                        cachedEnvError = null;
                        cachedEnvErrorArgs = null;
                        for (Iterator<TestEnvironment.Element> i = cachedEnv.elements().iterator();
                             i.hasNext() && cachedEnvError == null; ) {
                            TestEnvironment.Element entry = i.next();
                            if (entry.getValue().contains("VALUE_NOT_DEFINED")) {
                                cachedEnv = null;
                                String eText =
                                        (entry.getDefinedInEnv() == null ? "" : "env." + entry.getDefinedInEnv() + ".") +
                                                entry.getKey() + "=" + entry.getValue();
                                cachedEnvError = qEnvUndefinedEntry;
                                cachedEnvErrorArgs = new Object[]{eText, entry.getDefinedInFile()};
                            }
                        }
                    }
                }

            } catch (TestEnvironment.Fault e) {
                cachedEnv = null;
                cachedEnvError = qEnvError;
                cachedEnvErrorArgs = new Object[]{e.getMessage()};
            }
            cachedEnv_envTable = envTable;
            cachedEnv_envName = envName;
        }
    }
}
