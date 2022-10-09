/*
 * $Id$
 *
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest;

import com.sun.javatest.util.BackupPolicy;
import com.sun.javatest.util.I18NResourceBundle;
import com.sun.javatest.util.TextWriter;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;


class Trace implements Harness.Observer {
    private static final Format TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    //------methods from Harness.Observer----------------------------------------
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Trace.class);
    private TextWriter out;
    private File reportDir;
    private BackupPolicy backupPolicy;
    private boolean useTimestamp;

    Trace(BackupPolicy backupPolicy) {
        this.backupPolicy = backupPolicy;
    }

    @Override
    public synchronized void startingTestRun(Parameters params) {
        openOutput(params);
        if (out != null) {
            TestSuite ts = params.getTestSuite();
            String tsName = ts == null ? "null" : ts.getClass().getName();

            TestFinder tf = params.getWorkDirectory().getTestResultTable().getTestFinder();
            String tfName = tf == null ? "null" : tf.getClass().getName();
            println(i18n, "trace.starting", tsName, tfName);
        }
    }

    //------private data-----------------------------------------------------------

    @Override
    public synchronized void startingTest(TestResult tr) {
        if (out != null) {
            TestDescription td = null;
            try {
                td = tr.getDescription();
            } catch (TestResult.Fault e) {
                e.printStackTrace();
            }

            if (td != null) {
                println(i18n, "trace.testStarting", td.getRootRelativeURL());
            }
        }
    }

    @Override
    public synchronized void finishedTest(TestResult tr) {
        if (out != null) {
            try {
                TestDescription td = tr.getDescription();
                println(i18n, "trace.testFinished",
                        td.getRootRelativeURL(), tr.getStatus());
            } catch (TestResult.Fault e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void stoppingTestRun() {
        if (out != null) {
            println(i18n, "trace.stopping");
        }
    }

    @Override
    public synchronized void finishedTesting() {
        if (out != null) {
            println(i18n, "trace.cleanup");
        }
    }

    @Override
    public synchronized void finishedTestRun(boolean allOK) {
        if (out != null) {
            if (allOK) {
                println(i18n, "trace.doneOK");
            } else {
                println(i18n, "trace.doneNotOK");
            }
            close();
        }
    }

    @Override
    public synchronized void error(String s) {
        if (out != null) {
            println(i18n, "trace.error", s);
        }
    }

    private void openOutput(Parameters params) {
        try {
            WorkDirectory wd = params.getWorkDirectory();
            File traceFile = wd.getSystemFile("harness.trace");
            boolean autoFlush = Boolean.getBoolean("javatest.trace.autoflush");
            String timeStampSysProp = System.getProperty("javatest.trace.timestamp");
            useTimestamp = (timeStampSysProp == null) ? true : Boolean.parseBoolean(timeStampSysProp);
            out = new TextWriter(backupPolicy.backupAndOpenWriter(traceFile), autoFlush);
            // The following output is verified.
            out.println("# Trace file started at " + timeStamp());
            out.println("# " + ProductInfo.getName() + " version " + ProductInfo.getVersion());
            out.println("# class directory: " + Harness.getClassDir());
            out.println("# using java: " + System.getProperty("java.home"));
        } catch (IOException e) {
            System.err.println("Cannot open trace file: trace cancelled");
            System.err.println(e);
            out = null;
        }
    }

    //------private data-----------------------------------------------------------

    private void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println("Exception occurred writing to trace file");
                System.err.println(e);
            }
            out = null;
        }
    }

    private void println(I18NResourceBundle i18n, String key) {
        printLocalizedLn(i18n.getString(key));
    }

    private void println(I18NResourceBundle i18n, String key, Object arg) {
        printLocalizedLn(i18n.getString(key, arg));
    }

    private void println(I18NResourceBundle i18n, String key, Object... args) {
        printLocalizedLn(i18n.getString(key, args));
    }

    private void printLocalizedLn(String msg) {
        try {
            out.println(useTimestamp ? timeStamp() + " " + msg : msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Exception occurred writing to trace file");
            System.err.println(e);
            System.err.println("while trying to write: " + msg);
        }
    }

    private String timeStamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }
}
