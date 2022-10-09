/*
 * $Id$
 *
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javatest.Harness;
import com.sun.javatest.InterviewParameters;
import com.sun.javatest.JavaTestError;
import com.sun.javatest.JavaTestSecurityManager;
import com.sun.javatest.ProductInfo;
import com.sun.javatest.Status;
import com.sun.javatest.services.ServiceManager;
import com.sun.javatest.util.Debug;
import com.sun.javatest.util.ExitCount;
import com.sun.javatest.util.I18NResourceBundle;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The main program class for JT Harness.
 */
public class Main {

    private static final String CMDMGRLIST = "META-INF/services/com.sun.javatest.tool.CommandManager.lst";
    /**
     * If this system property is defined then its value is expected to contain comma-separated list
     * of CommandManager subclasses to load, which would be put in use instead of those contained in the dedicated file.
     */
    public static final String COMMAND_MANAGERS_TO_LOAD = "services.com.sun.javatest.tool.CommandManager";
    private static final int RC_GUI_ACTIVE = -1;
    private static final int RC_OK = 0;
    private static final int RC_BATCH_TESTS_FAILED = 1;
    private static final int RC_BATCH_TESTS_ERROR = 2;
    private static final int RC_USER_ERROR = 3;
    private static final int RC_INTERNAL_ERROR = 4;
    private static boolean tracing;
    private static long traceStartTime;
    private static PrintWriter traceOut;
    private static boolean initialized = false;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Main.class);
    private CommandManager[] commandManagers;
    private HelpManager helpManager;
    private DesktopManager desktopManager;
    private ServiceManager.ServiceCommandManager serviceManager;

    /**
     * Run JT Harness with command-line args.
     *
     * @param args Arguments, per the command-line spec
     */
    public static void main(String... args) {
        tracing = Boolean.getBoolean("javatest.trace.startup");

        if (tracing) {
            traceStartTime = System.currentTimeMillis();
        }

        if (Boolean.getBoolean("javatest.trace.printargs")) {
            StringBuilder fullCmd = new StringBuilder();
            StringBuilder incrementalCmd = new StringBuilder();

            for (String a : args) {
                fullCmd.append(a);
                fullCmd.append(" ");

                incrementalCmd.append("// ");
                incrementalCmd.append(a);
                incrementalCmd.append("\n");
            }   // for

            System.out.println(fullCmd.toString().trim());
            System.out.println(incrementalCmd.toString());
        }

        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            String[] oldVersions = {"1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6"};
            for (String oldVersion : oldVersions) {
                if (javaVersion.startsWith(oldVersion)) {
                    // I18N?
                    System.err.println("Please use Java(TM) Standard Edition, Version 7.0 or better to run the JT Harness(TM) harness.");
                    System.exit(1);
                }
            }
        }

        main0(args);
    }

    private static void main0(String... args) {
        Debug.setProperties(System.getProperties());

        PrintWriter out = new PrintWriter(System.err) {
            @Override
            public void close() {
                flush();
            }
        };

        if (tracing) {
            traceOut = out;
        }

        try {
            ExitCount.inc();
            Main m = new Main();

            CommandContext ctx = new CommandContext(out);
            m.run(args, ctx);

            if (tracing) {
                trace("Main.run complete");
            }

            out.flush(); // flush now in case ExitCount exits

            int[] stats = ctx.getTestStats();
            int rc = stats[Status.ERROR] > 0 ? RC_BATCH_TESTS_ERROR
                    : stats[Status.FAILED] > 0 ? RC_BATCH_TESTS_FAILED
                    : RC_OK;

            ExitCount.dec(true, rc);

            // all initialization is done; this thread has nothing left to do, so...
            boolean preload =
                    System.getProperty("javatest.preload.classes", "false").equals("true");

            if (preload) {
                if (tracing) {
                    trace("preloading classes...");
                }

                preloadUsefulClasses();

                if (tracing) {
                    trace("preloaded classes");
                }
            }

            out.flush();
        } catch (CommandContext.Fault e) {
            // occurs when executing commands on command line or in command file
            Throwable t = e.getCause();
            if (t instanceof Command.Fault) {
                CommandContext ctx = e.getContext();
                boolean verboseCommands =
                        ctx.getVerboseOptionValue(CommandContext.VERBOSE_COMMANDS);
                if (!verboseCommands) {
                    Command.Fault ce = (Command.Fault) t;
                    Command c = ce.getCommand();
                    out.println(CommandContext.TRACE_PREFIX + c.toString());
                }
            }
            out.println(e.getMessage());
            out.flush();
            exit(RC_USER_ERROR);
        } catch (CommandParser.Fault e) {
            // occurs when parsing commands on command line or in command file
            Throwable t = e.getCause();
            if (t instanceof Command.Fault) {
                Command.Fault ce = (Command.Fault) t;
                Command c = ce.getCommand();
                out.println(CommandParser.TRACE_PREFIX + c.toString());
            }
            out.println(e.getMessage());
            out.flush();
            exit(RC_USER_ERROR);
        } catch (Fault e) {
            out.println(e.getMessage());
            out.flush();
            exit(RC_INTERNAL_ERROR);
        } catch (Error | RuntimeException e) {
            e.printStackTrace(out);
            out.flush();
            exit(RC_INTERNAL_ERROR);
        }
    }

    private static void preloadUsefulClasses() {
        //System.err.println("\n\n\n>>>>> preloading classes\n\n\n");
        new javax.swing.text.html.HTMLEditorKit().createDefaultDocument();
        com.sun.interview.Interview i = new com.sun.interview.Interview("dummy") {
            com.sun.interview.Question qEnd = new com.sun.interview.FinalQuestion(this);

            {
                setFirstQuestion(qEnd);
            }
        };
        new com.sun.interview.wizard.WizPane(i);
        //System.err.println("\n\n\n>>>>> preloading classes done\n\n\n");
    }

    private static void trace(String msg) {
        long now = System.currentTimeMillis();
        traceOut.println(MessageFormat.format("{0,number,[##0.0]} {1}",
                Float.valueOf((now - traceStartTime) / 1000f), msg));
        traceOut.flush();
    }

    /**
     * Call System.exit, taking care to get permission from the
     * JavaTestSecurityManager, if it is installed.
     *
     * @param exitCode an exit code to be passed to System.exit
     */
    private static void exit(int exitCode) {
        // If our security manager is installed, it won't allow a call of
        // System.exit unless we ask it nicely, pretty please, thank you.
        SecurityManager sc = System.getSecurityManager();
        if (sc instanceof JavaTestSecurityManager) {
            JavaTestSecurityManager.setAllowExit(true);
        }
        System.exit(exitCode);
        throw new JavaTestError(i18n, "main.cannotExit.err");
    }

    /**
     * The main routine to run JT Harness.
     *
     * @param args Arguments for JT Harness, per the command-line spec.
     * @param out  A stream to which to write standard messages, such as
     *             command-line help, version info etc. Some error messages will
     *             still be sent to System.err.
     * @throws Main.Fault           if there is a problem initializing the harness
     * @throws CommandContext.Fault if there is a problem executing a command
     * @throws CommandParser.Fault  if there is a problem parsing the args
     */
    public final void run(String[] args, PrintWriter out)
            throws Fault, CommandContext.Fault, CommandParser.Fault {
        run(args, new CommandContext(out));
    }

    /**
     * A routine to run JT Harness.
     *
     * @param args Arguments for JT Harness, per the command-line spec.
     * @param ctx  A context to use to execute the commands in the args
     * @throws CommandContext.Fault if there is a problem executing a command
     * @throws CommandParser.Fault  if there is a problem parsing the args
     */
    public final void run(String[] args, final CommandContext ctx)
            throws Fault, CommandContext.Fault, CommandParser.Fault {
        if (commandManagers == null) {
            desktopManager = new DesktopManager();
            helpManager = new HelpManager();
            serviceManager = new ServiceManager.ServiceCommandManager();
            try {
                Set<Object> mgrs;
                String listOfManagersToLoad = System.getProperty(COMMAND_MANAGERS_TO_LOAD);
                if (listOfManagersToLoad == null) {
                    ManagerLoader ml = new ManagerLoader(CommandManager.class, System.err);
                    mgrs = ml.loadManagers(CMDMGRLIST);
                } else {
                    mgrs = new HashSet<>();
                    for (String name : listOfManagersToLoad.split(",")) {
                        mgrs.add(Class.forName(name).getDeclaredConstructor().newInstance());
                    }
                }
                mgrs.add(desktopManager);
                mgrs.add(helpManager);
                mgrs.add(serviceManager);
                commandManagers = mgrs.toArray(new CommandManager[mgrs.size()]);
                helpManager.setCommandManagers(commandManagers);
            } catch (IOException e) {
                throw new Fault(i18n, "main.cantAccessResource", CMDMGRLIST, e);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        CommandParser p = new CommandParser(commandManagers);
        boolean urlEncoded = Boolean.getBoolean("javatest.command.urlEncoded");
        p.parse(args, urlEncoded, ctx);

        if (!initialized) {
            File classDir = ProductInfo.getJavaTestClassDir();
            Harness.setClassDir(classDir);

            // Install our own security manager. This is primarily for self-defense
            // against sameJVM tests, and not to prevent access outside the sandbox.
            // Moan to stderr if it can't be installed.
            JavaTestSecurityManager.install();

            // mark initialization done
            initialized = true;
        }

        Command[] cmds = ctx.getCommands();

        // special case, when user requested info
        boolean helpInfoRequired = helpManager.isInfoRequired();
        if (helpInfoRequired) {
            helpManager.showRequiredInfo(ctx.getLogWriter(), ctx);
            if (cmds.length == 0) {
                return;
            }
        }

        final Desktop desktop;
        boolean needDesktop = ctx.isDesktopRequired();

        if (needDesktop) {
            if (tracing) {
                trace("creating desktop...");
            }

            // should really be on event thread
            desktop = desktopManager.createDesktop(ctx);

            /*
            // show splash screen, using event thread
            Runnable task = new Runnable() {
                public void run() {
                    if (tracing)
                        trace("creating splash screen...");
                    Startup su = null;
                    // lots of GUI construction caused by the few lines below
                    for (int i = 0; i < cmds.length; i++) {
                        URL ss = cmds[i].getCustomSplash();
                        if (ss != null) {
                            su = new Startup(ss);
                            break;
                        }
                    }   // for

                    if (su == null)
                        su = new Startup();

                    final Startup splashScreen = su;

                    Thread splashTimer = new Thread() {
                            public void run() {
                                // pause for 15 seconds before displaying desktop and
                                // hiding splash screen; if the desktop is made ready sooner,
                                // it will be displayed sooner
                                int splashSecs =
                                    Integer.getInteger("javatest.splashScreen.time", 15).intValue();
                                try {
                                    Thread.currentThread().sleep(splashSecs*1000);
                                }
                                catch (InterruptedException e) {
                                }
                                if (tracing)
                                    trace("splash timer done: showing desktop...");

                                Runnable task2 = new Runnable() {
                                    public void run() {
                                        desktop.setVisible(true);
                                        splashScreen.disposeLater();
                                    }
                                };

                                try {
                                    EventQueue.invokeAndWait(task2);
                                } catch (InterruptedException e) {
                                    if (tracing)
                                        e.printStackTrace();
                                } catch (java.lang.reflect.InvocationTargetException e) {
                                    if (tracing)
                                        e.printStackTrace();
                                }

                            }
                        };
                    splashTimer.setDaemon(true);
                    splashTimer.start();
                }   // run()
            };  // runnable

            try {
                EventQueue.invokeAndWait(task);
            } catch (InterruptedException e) {
                if (tracing)
                    e.printStackTrace();
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (tracing)
                    e.printStackTrace();
            }
            */

            desktop.setVisible(true);
            ctx.setDesktop(desktop);
            // set context log to display in a log tool
        } else {
            desktop = null;
        }

        // execute the commands on the command line
        if (tracing) {
            trace("executing command line...");
        }

        if (desktop != null) {
            desktop.setVisible(true);
        }

        ctx.runCommands();

        if (tracing) {
            trace("command line done");
        }

        if (desktop != null) {
            if (ctx.isCloseDesktopWhenDoneEnabled()
                    && desktop.isOKToAutoExit()) {
                Runnable task = () -> {
                    desktop.setVisible(false);
                    desktop.dispose();
                };
                try {
                    EventQueue.invokeAndWait(task);
                } catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
                    if (tracing) {
                        e.printStackTrace();
                    }
                }
            } else {
                InterviewParameters ip_tmp = null;
                if (desktop.isEmpty() && ctx.hasConfig()) {
                    try {
                        ip_tmp = ctx.getConfig();
                    } catch (CommandContext.Fault e) {
                        System.err.println(i18n.getString("main.cantGetConfig", e.getMessage()));
                    }
                }
                final InterviewParameters ip = ip_tmp;

                // run()
                Runnable task = () -> {
                    // if a desktop has been started, make sure it is not empty
                    if (desktop.isEmpty()) {
                        if (ctx.hasConfig() && ip != null) {
                            if (tracing) {
                                trace("show specified test suite");
                            }
                            desktop.restoreHistory();
                            Tool tool = desktop.addDefaultTool(ip);
                            Map<String, String> data = ctx.getDesktopData();
                            if (data != null) {
                                tool.restore(data);
                            }
                        } else if (desktop.isFirstTime()) {
                            if (tracing) {
                                trace("show default");
                            }
                            desktop.addDefaultTool();
                        } else {
                            if (tracing) {
                                trace("restore desktop");
                            }
                            desktop.restore();
                        }
                    }
                    if (tracing) {
                        trace("set desktop visible");
                    }
                    desktop.setVisible(true);
                };   // Runnable

                try {
                    EventQueue.invokeAndWait(task);
                } catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
                    if (tracing) {
                        e.printStackTrace();
                    }
                }

            }
        }
        ctx.dispose();
    }

    /**
     * Thrown when a bad command line argument is encountered.
     */
    public static class Fault extends Exception {
        /**
         * Create a BadArgs exception.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param key  The key for the detail message.
         */
        public Fault(I18NResourceBundle i18n, String key) {
            super(i18n.getString(key));
        }

        /**
         * Create a BadArgs exception.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param key  The key for the detail message.
         * @param arg  An argument to be formatted with the detail message by
         *             {@link java.text.MessageFormat#format}
         */
        public Fault(I18NResourceBundle i18n, String key, Object arg) {
            super(i18n.getString(key, arg));
        }

        /**
         * Create a BadArgs exception.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param key  The key for the detail message.
         * @param args An array of arguments to be formatted with the detail message by
         *             {@link java.text.MessageFormat#format}
         */
        public Fault(I18NResourceBundle i18n, String key, Object... args) {
            super(i18n.getString(key, args));
        }
    }
}
