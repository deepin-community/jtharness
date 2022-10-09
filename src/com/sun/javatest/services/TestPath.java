/*
 * $Id$
 *
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.services;

import com.sun.javatest.TestDescription;
import com.sun.javatest.TestSuite;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents mapping from test path (represented as java regexp),
 * to the set of services, which need to be started before executing any test,
 * that matches this test path.
 */
public class TestPath {

    private final Set<String> services;
    protected ServiceManager mgr;
    protected Pattern p;
    protected TDMatcher tdm;

    /**
     * Creates new test path with specified pattern, matcher and empty set of
     * services. Both pattern and matcher may be null, test path is always
     * accepted in such case. Otherwise, matching result ic logical AND of
     * regexp and custom matching.
     *
     * @param ts                 test suite
     * @param pathPattern        patter for matching test paths.
     * @param tdMatcherClassName {@link com.sun.javatest.services.TestPath.TDMatcher}
     *                           to customize matching rules.
     */
    public TestPath(TestSuite ts, String pathPattern, String tdMatcherClassName) {
        if (pathPattern != null) {
            p = Pattern.compile(pathPattern);
        }
        if (tdMatcherClassName != null && !tdMatcherClassName.isEmpty()) {
            try {

                Class<? extends TDMatcher> tdMatcherClass = ts.loadClass(tdMatcherClassName);
                tdm = tdMatcherClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                Logger.getLogger(TestPath.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        services = new TreeSet<>();
    }

    /**
     * @param path root relative test path
     * @return true, if test path matches pattern.
     */
    public boolean matches(String path) {
        boolean res = true;
        if (p != null) {
            Matcher m = p.matcher(path);
            res &= m.matches();
        }
        return res;
    }

    public boolean matches(TestDescription td) {
        boolean res = true;

        res &= matches(td.getRootRelativePath());
        if (tdm != null) {
            res &= tdm.matches(td);
        }

        return res;
    }

    /**
     * Gives access to the set of services of this test path.
     *
     * @return set of test path service's IDs.
     */
    public Set<String> getServices() {
        return services;
    }

    public void addService(String sID) {
        services.add(sID);
    }

    void setServiceManager(ServiceManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TestPath  pattern='");
        sb.append(p);
        sb.append("' services={");
        for (String s : services) {
            sb.append(s).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    public interface TDMatcher {
        boolean matches(TestDescription td);
    }
}
