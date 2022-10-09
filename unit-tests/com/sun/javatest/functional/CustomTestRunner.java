/*
 * $Id$
 *
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javatest.functional;

import com.sun.javatest.TestUtil;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CustomTestRunner extends TestSuiteRunningTestBase {

    @Test
    public void test() {
        runJavaTest();
        checkSystemErrLineIs(1, "DemoTestRunner.runTests method called");
        checkSystemErrLineIs(7, "Test results: passed: 2; failed: 1");
    }

    @Override
    protected int[] getExpectedTestRunFinalStats() {
        return new int[]{2, 1, 0, 0};
    }


    @Override
    protected String[] getExpectedLinesInTestrunSummary() {
        return new String[]{
                "mytests/ts.html#Fails_01   Failed. exit code 1",
                "mytests/ts.html#Passes_01  Passed. This test has passed",
                "mytests/ts.html#Passes_02  Passed. This test has passed"
        };
    }

    @Override
    protected List<String> getTailArgs() {
        return Arrays.asList("");
    }

    @Override
    protected String getEnvName() {
        return "initurl";
    }

    @Override
    protected String getEnvfileName() {
        return "initurl.jte";
    }

    @Override
    protected String getTestsuiteName() {
        return "testRunner";
    }


}
