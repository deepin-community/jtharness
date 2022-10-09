/*
 * $Id$
 *
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.PropertyPermission;

/**
 * This class is set for JT Harness running as an application.  Currently, it imposes
 * almost no security restrictions at all: its existence prevents anyone else
 * (e.g. a test running in this JVM) from setting a more restrictive security manager.
 * <p>
 * Although not required for running under JDK1.0.2, extra definitions for forward
 * compatibility with JDK1.1 are also provided. They will effectively be ignored
 * by JDK1.0.2.
 */

public class JavaTestSecurityManager extends SecurityManager {
    static private boolean allowExit = false; // no overrides on this one; API control only
    static private boolean allowPropertiesAccess = true;   // see initializer
    static private boolean verbose =
            Boolean.getBoolean("javatest.security.verbose");

    {
        // use user specified value if given
        String s = System.getProperty("javatest.security.allowPropertiesAccess");
        if (s != null) {
            allowPropertiesAccess = Boolean.valueOf(s).booleanValue();
        }
    }

    /**
     * Please note that the new default behaviour is NOT installing JavaTestSecurityManager.
     *
     * This method would try to install a copy of this security manager only if "javatest.security.noSecurityManager" property
     * is set to something else than 'true' (ignoring case).
     * If "javatest.security.noSecurityManager" is not defined, security manager still would not be installed.
     * The install can be enabled by setting system property "javatest.security.noSecurityManager" to 'false'.
     * If another security manager is already installed, the install will fail;
     * a warning message will be written to the console if the previously installed security manager is not a subtype of
     * com.sun.javatest.JavaTestSecurityManager.
     */
    public static void install() {
        try {
            String noSecurityMgr = "javatest.security.noSecurityManager";
            if (Boolean.valueOf(System.getProperty(noSecurityMgr, "true"))) {
                // not installing any custom security manager
                // if 'javatest.security.noSecurityManager' property was not set
                // or was set to 'true' (ignoring case)
            } else {
                try {
                    // Trying to install our own Security Manager.
                    // First testing to see if permission API available:
                    // if it's not, we'll get an exception and load
                    // an old-style security manager
                    Class.forName("java.security.Permission");
                    System.setSecurityManager(new NewJavaTestSecurityManager());
                } catch (ClassNotFoundException e) {
                    System.setSecurityManager(new JavaTestSecurityManager());
                }
                System.err.println("JT Harness installed its own Security Manager");
                System.err.println("because system property '" + noSecurityMgr + "' was set to '"
                        + System.getProperty(noSecurityMgr, "undefined") + "'");
                System.err.println();
            }
        } catch (SecurityException e) {
            SecurityManager sm = System.getSecurityManager();
            if (!(sm instanceof JavaTestSecurityManager)) {
                System.err.println();
                System.err.println("     ---- WARNING -----");
                System.err.println();
                System.err.println("JT Harness could not install its own Security Manager");
                System.err.println("because of the following exception:");
                System.err.println("     " + e);
                System.err.println("This is not a fatal error, but it may affect the");
                System.err.println("execution of sameJVM tests");
                System.err.println();
            }
        }
    }

    // These are the JDK1.0.2 security methods
    @Override
    public void checkAccept(String host, int port) {
    }

    @Override
    public void checkAccess(Thread g) {
    }

    @Override
    public void checkAccess(ThreadGroup g) {
    }

    @Override
    public void checkConnect(String host, int port) {
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
    }

    @Override
    public void checkCreateClassLoader() {
    }

    @Override
    public void checkDelete(String file) {
    }

    @Override
    public void checkExec(String cmd) {
    }

    // tests which call System.exit() should not cause JT Harness to exit
    @Override
    public void checkExit(int status) {
        if (allowExit == false) {
            if (verbose) {
                System.err.println(getClass().getName() + ": System.exit() forbidden");
                new Throwable().printStackTrace();
            }
            throw new SecurityException("System.exit() forbidden by JT Harness");
        }
    }

    @Override
    public void checkLink(String lib) {
    }

    @Override
    public void checkListen(int port) {
    }

    @Override
    public void checkPackageAccess(String pkg) {
    }

    @Override
    public void checkPackageDefinition(String pkg) {
    }

    // allowing tests to get at and manipulate the system properties
    // is too dangerous to permit when multiple tests are running,
    // possibly simultaneously, in the same JVM.
    @Override
    public synchronized void checkPropertiesAccess() {
        if (allowPropertiesAccess == false) {
            if (verbose) {
                System.err.println(getClass().getName() + ": properties access forbidden");
                new Throwable().printStackTrace();
            }
            throw new SecurityException("Action forbidden by JT Harness: checkPropertiesAccess");
        }
    }

    @Override
    public void checkPropertyAccess(String key) {
    }

    @Override
    public void checkRead(FileDescriptor fd) {
    }

    @Override
    public void checkRead(String file) {
    }

    @Override
    public void checkRead(String file, Object context) {
    }

    @Override
    public void checkSetFactory() {
    }

    /**
     * Still temporarily kept for compatibility with JDK7,
     * the overridden methods are deprecated in SE8 and removed in SE11
     */
    @java.lang.Deprecated
    public boolean checkTopLevelWindow(Object window) {
        return true;
    }

    @Override
    public void checkWrite(FileDescriptor fd) {
    }

    @Override
    public void checkWrite(String file) {
    }

    /**
     * Still temporarily kept for compatibility with JDK7,
     * the overridden methods are deprecated in SE8 and removed in SE11
     */
    // These methods are added for forward-compatibility with JDK1.1
    @java.lang.Deprecated
    public void checkAwtEventQueueAccess() {
    }

    /**
     * Still temporarily kept for compatibility with JDK7,
     * the overridden methods are deprecated in SE8 and removed in SE11
     */
    @java.lang.Deprecated
    public void checkMemberAccess(Class<?> clazz, int which) {
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
    }

    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    @Override
    public void checkPrintJobAccess() {
    }

    @Override
    public void checkSecurityAccess(String provider) {
    }

    /**
     * Still temporarily kept for compatibility with JDK7,
     * the overridden methods are deprecated in SE8 and removed in SE11
     */
    @java.lang.Deprecated
    public void checkSystemClipboardAccess() {
    }

    /**
     * Set whether or not the JVM may be exited. The default value is "false".
     *
     * @param bool true if the JVM may be exited, and false otherwise
     * @return the previous value of this setting
     */
    public static boolean setAllowExit(boolean bool) {
        boolean prev = allowExit;
        allowExit = bool;
        return prev;
    }

    /**
     * Set whether or not the set of system properties may be accessed.
     * The default value is determined by the system property
     * "javatest.security.allowPropertiesAccess".
     *
     * @param bool true if the system properties may be accessed, and false otherwise
     * @return the previous value of this setting
     */
    public boolean setAllowPropertiesAccess(boolean bool) {
        boolean prev = allowPropertiesAccess;
        allowPropertiesAccess = bool;
        return prev;
    }
}

class NewJavaTestSecurityManager extends JavaTestSecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        // allow most stuff, but limit as appropriate
        if (perm instanceof RuntimePermission) {
            if (perm.getName().equals("exitVM")) {
                checkExit(0);
            }
            if (perm.getName().equals("createSecurityManager")) {
                super.checkPermission(new java.lang.RuntimePermission("createSecurityManager"));
            }
        } else if (perm instanceof PropertyPermission) {
            if (perm.getActions().equals("read,write")) {
                checkPropertiesAccess();
            }
        }
    }
}
