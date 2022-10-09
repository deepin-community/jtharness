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
package com.sun.javatest;

import com.sun.javatest.util.I18NResourceBundle;
import com.sun.javatest.util.PropertyUtils;
import com.sun.javatest.util.StringArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * An implementation of Parameters, using data read from a .jtp file.
 */
public class FileParameters
        extends
        BasicParameters
        implements
        Parameters.LegacyEnvParameters {
    private static final String PARAMFILE_EXTN = ".jtp";
    private static final I18NResourceBundle i18n =
            I18NResourceBundle.getBundleForClass(FileParameters.class);
    private File[] envFiles;
    private File[] cachedAbsEnvFiles;

    //---------------------------------------------------------------------
    private File cachedAbsEnvFiles_base;
    private File[] cachedAbsEnvFiles_envFiles;
    private String envName;
    private TestEnvContext cachedEnvTable;
    private File[] cachedEnvTable_absEnvFiles;
    private String envTableError;
    private TestEnvironment cachedEnv;
    private TestEnvContext cachedEnv_envTable;
    private String cachedEnv_envName;
    private String envError;
    private File reportDir;
    private String legacyTsPath;        // always a directory

    /**
     * Create an empty FileParameters object.
     */
    public FileParameters() {
    }

    /**
     * Create a FileParameters object, based on data read from a parameter file.
     *
     * @param file the file to be read to initialize this object
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException           if there is a problem reading the file
     */
    public FileParameters(File file)
            throws FileNotFoundException, IOException {
        Map<String, String> p;
        try (Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            p = PropertyUtils.load(in);
        }

        setTestSuite(adjustPath(p.get("javasoft.sqe.javatest.selection.testSuite")));
        setWorkDirectory(p.get("javasoft.sqe.javatest.results.workDir"));
        setTests(p.get("javasoft.sqe.javatest.selection.tests"));
        setExcludeFiles(p.get("javasoft.sqe.javatest.selection.excludeList"));
        String keywordOp = p.get("javasoft.sqe.javatest.selection.keywordOp");
        String keywords = p.get("javasoft.sqe.javatest.selection.keywords");
        setKeywords(keywordOp, keywords);
        String statusOp = p.get("javasoft.sqe.javatest.selection.status");
        String[] statusTests = new String[Status.NUM_STATES];
        statusTests[Status.PASSED] = p.get("javasoft.sqe.javatest.selection.prev.passed");
        statusTests[Status.FAILED] = p.get("javasoft.sqe.javatest.selection.prev.failed");
        statusTests[Status.ERROR] = p.get("javasoft.sqe.javatest.selection.prev.error");
        statusTests[Status.NOT_RUN] = p.get("javasoft.sqe.javatest.selection.prev.notRun");
        setPriorStatusValues(statusOp, statusTests);
        setEnvFiles(p.get("javasoft.sqe.javatest.execution.envFiles"));
        setEnvName(p.get("javasoft.sqe.javatest.execution.environment"));
        setConcurrency(p.get("javasoft.sqe.javatest.execution.concurrency"));
        setTimeoutFactor(p.get("javasoft.sqe.javatest.execution.timeFactor"));
        setReportDir(p.get("javasoft.sqe.javatest.results.reportDir"));
    }
    /**
     * Create a FileParameters object, based on command-line-like args.
     * The args that are accepted are:
     * <dl>
     * <dt>{@code -t} <i>testsuite</i><br>{@code -testsuite} <i>testsuite</i>
     * <dd>Specify the test suite
     * <dt>{@code -keywords} <i>expr</i>
     * <dd>Specify a keyword expression, used to filter the tests to be run.
     * <dt>{@code -status} <i>status-list</i>
     * <dd>Specify the status values used to select tests at runtime.
     * <i>status-list</i> should be a comma-separated list of words from
     * the following list:
     * {@code passed},
     * {@code failed},
     * {@code error},
     * {@code notRun}
     * <dt>{@code -exclude} <i>exclude-list-file</i>
     * <dd>Specify an exclude-list file containing a list of tests to
     * be excluded from the test run.
     * The option can be specified more than once, with different files.
     * <dt>{@code -envFile} <i>environment-file</i>
     * <dd>Specify an environment file, containing environment entries
     * providing details on how to run tests.
     * The option can be specified more than once, with different files.
     * <dt>{@code -env} <i>environment-name</i>
     * <dd>Specify the name of the environment to be used from the
     * set of environment files.
     * <dt>{@code -concurrency} <i>number</i>
     * <dd>Specify how many tests JT Harness may run at once. The default
     * is 1.
     * <dt>{@code -timeoutFactor} <i>number</i>
     * <dd>Specify a scale factor to be used to multiply the timeout
     * value for each test, to allow for running on slow CPUs.
     * <dt>{@code -report} <i>report-dir</i><br>{@code -r} <i>report-dir</i>
     * <dd>Specify a directory in which to write reports at the end of the test run.
     * <dt>{@code -workDir} <i>work-dir</i><br>{@code -w} <i>work-dir</i>
     * <dd>Specify a directory in which to write the results of the individual tests.
     * <dt><i>initial-files</i>
     * <dd>Trailing file arguments are treated as initial files, used to select
     * which parts of the test suite should be run.
     * </dl>
     * The test suite, work directory and report directory are evaluated
     * relative to the user's current directory, unless the location specified
     * is an absolute path.  The exclude list and environment files are located
     * relative to the test suite location, unless they are absolute paths.
     *
     * @param args The args used to initialize the FileParameters object.
     * @throws IllegalArgumentException If an unrecognized argument is found.
     */
    public FileParameters(String... args) {
        String testSuiteArg = null;
        String workDirArg = null;
        String testsArgs = null;
        String exclFilesArgs = null;
        String keywordsExprArg = null;
        String priorStatusValuesArg = null;
        String envFilesArgs = null;
        String envNameArg = null;
        String concurrencyArg = null;
        String timeoutFactorArg = null;
        String reportDirArg = null;

        for (int i = 0; i < args.length; i++) {
            if ("-testSuite".equalsIgnoreCase(args[i]) || "-t".equalsIgnoreCase(args[i])) {
                testSuiteArg = args[++i];
            } else if ("-keywords".equalsIgnoreCase(args[i])) {
                keywordsExprArg = args[++i];
            } else if ("-status".equalsIgnoreCase(args[i])) {
                priorStatusValuesArg = args[++i].toLowerCase();
            } else if ("-exclude".equalsIgnoreCase(args[i])) {
                if (exclFilesArgs == null) {
                    exclFilesArgs = args[++i];
                } else {
                    exclFilesArgs += " " + args[++i];
                }
            } else if ("-envFile".equalsIgnoreCase(args[i])) {
                if (envFilesArgs == null) {
                    envFilesArgs = args[++i];
                } else {
                    envFilesArgs += " " + args[++i];
                }
            } else if ("-env".equalsIgnoreCase(args[i])) {
                envNameArg = args[++i];
            } else if ("-concurrency".equalsIgnoreCase(args[i])) {
                concurrencyArg = args[++i];
            } else if ("-timeoutFactor".equalsIgnoreCase(args[i])) {
                timeoutFactorArg = args[++i];
            } else if ("-report".equalsIgnoreCase(args[i]) || "-r".equalsIgnoreCase(args[i])) {
                reportDirArg = args[++i];
            } else if ("-workDir".equalsIgnoreCase(args[i]) || "-w".equalsIgnoreCase(args[i])) {
                workDirArg = args[++i];
            } else if (args[i].startsWith("-")) {
                throw new IllegalArgumentException(args[i]);
            } else {
                String[] tests = new String[args.length - i];
                System.arraycopy(args, i, tests, 0, tests.length);
                testsArgs = StringArray.join(tests);
                i = args.length;
            }
        }

        setTestSuite(adjustPath(testSuiteArg));
        setWorkDirectory(adjustPath(workDirArg));
        setTests(testsArgs);
        setExcludeFiles(exclFilesArgs);
        setKeywords("expr", keywordsExprArg);
        setPriorStatusValues(priorStatusValuesArg);
        setEnvFiles(envFilesArgs);
        setEnvName(envNameArg);
        setConcurrency(concurrencyArg);
        setTimeoutFactor(timeoutFactorArg);
        setReportDir(adjustPath(reportDirArg));
    }

    /**
     * Determine if the specified file is a parameter file,
     * as determined by whether its extension is .jtp or not.
     *
     * @param file the file to be checked
     * @return true if the specified file is a parameter file,
     * and false otherwise
     */
    public static boolean isParameterFile(File file) {
        return file.getPath().endsWith(PARAMFILE_EXTN);
    }

    @Override
    public Parameters.EnvParameters getEnvParameters() {
        return this;
    }

    @Override
    public File[] getEnvFiles() {
        return envFiles;
    }

    @Override
    public void setEnvFiles(File... files) {
        envFiles = files;
    }

    private void setEnvFiles(String files) {
        String[] f = StringArray.split(files);
        File[] ff = new File[f.length];

        // legacy behavior requires that paths be relative to the testSuite
        // location specified in the JTE or on the cmd line
        for (int i = 0; i < ff.length; i++) {
            ff[i] = new File(makeLegacyTsRelative(f[i]));
        }
        setEnvFiles(ff);
    }

    @Override
    public File[] getAbsoluteEnvFiles() {
        updateAbsoluteEnvFiles();
        return cachedAbsEnvFiles;
    }

    @Override
    public String getEnvName() {
        return envName;
    }

    @Override
    public void setEnvName(String name) {
        envName = name;
    }

    /**
     * Get an object containing the environments read from the environment files.
     *
     * @return an object containing all the environments read from the environment files.
     * @see #setEnvFiles
     * @see #setEnvName
     */
    public TestEnvContext getEnvTable() {
        updateEnvTable();
        return cachedEnvTable;
    }

    @Override
    public TestEnvironment getEnv() {
        updateEnv();
        return cachedEnv;
    }

    //---------------------------------------------------------------------

    private void updateAbsoluteEnvFiles() {
        TestSuite ts = getTestSuite();
        File base = ts == null ? null : ts.getRootDir();
        if (cachedAbsEnvFiles == null ||
                cachedAbsEnvFiles_base != base ||
                cachedAbsEnvFiles_envFiles != envFiles) {
            cachedAbsEnvFiles = getAbsoluteFiles(base, envFiles);
        }
    }

    private void updateEnvTable() {
        updateAbsoluteEnvFiles();
        File[] absEnvFiles = cachedAbsEnvFiles;
        if (cachedEnvTable == null
                || !equal(absEnvFiles, cachedEnvTable_absEnvFiles)) {
            try {
                cachedEnvTable = new TestEnvContext(absEnvFiles);
                cachedEnvTable_absEnvFiles = absEnvFiles;
                envTableError = null;
            } catch (TestEnvContext.Fault e) {
                cachedEnvTable = null;
                envTableError = e.getMessage();
            }
        }
    }

    //---------------------------------------------------------------------

    private void updateEnv() {
        TestEnvContext envTable = getEnvTable();
        if (envTable == null) {
            cachedEnv = null;
            envError = i18n.getString("fp.noEnvs", envName);
            return;
        }

        TestEnvironment env;
        try {
            if (envName == null) {
                envName = "";
            }
            env = envTable.getEnv(envName);
            if (env == null) {
                // note envName==null is always a valid environment
                cachedEnv = null;
                envError = i18n.getString("fp.envNotFound", envName);
                return;
            }
            for (TestEnvironment.Element entry : env.elements()) {
                if (entry.value.contains("VALUE_NOT_DEFINED")) {
                    String eText =
                            (entry.definedInEnv == null ? "" : "env." + entry.definedInEnv + ".") +
                                    entry.key + "=" + entry.value;
                    cachedEnv = null;
                    envError = i18n.getString("fp.undefinedEntry",
                            eText, entry.definedInFile);
                    return;
                }
            }
        } catch (TestEnvironment.Fault e) {
            cachedEnv = null;
            envError = i18n.getString("fp.badEnv", envName, e.getMessage());
            return;
        }
        cachedEnv = env;
        envError = null;
    }

    //---------------------------------------------------------------------

    private boolean isEnvOK() {
        updateEnv();
        return envTableError == null && envError == null;
    }

    //---------------------------------------------------------------------

    @Override
    public boolean isValid() {
        return super.isValid() && isEnvOK();
    }

    //---------------------------------------------------------------------

    @Override
    public String getErrorMessage() {
        String basicError = super.getErrorMessage();
        return basicError != null ? basicError : envTableError;
    }

    //---------------------------------------------------------------------

    private void setTestSuite(String path) {
        legacyTsPath = path;
        File p = path == null ? null : new File(path);

        // we assume that the path has already be made non-relative
        // i.e. absolute
        if (p != null && p.isFile()) {
            legacyTsPath = p.getParent();
        }

        // this strange arrangement of checks is here because we
        // are trying to ensure that users playing around with legacy
        // configurations get reasonable results
        if (p == null || TestSuite.isTestSuite(p)) {
            setTestSuite(p);
        } else {
            File parent = p.getParentFile();
            // fallback check, check parent of specified location
            if (parent != null && TestSuite.isTestSuite(parent)) {
                setTestSuite(parent);
            } else {
                // must do this to ensure proper errors occur
                setTestSuite(p);
            }
        }
    }

    //---------------------------------------------------------------------

    private void setWorkDirectory(String path) {
        setWorkDirectory(path == null ? null : new File(path));
    }

    private void setTests(String tests) {
        setTests(StringArray.split(tests));
    }

    //---------------------------------------------------------------------

    private void setExcludeFiles(String files) {
        String[] f = StringArray.split(files);
        File[] ff = new File[f.length];

        // legacy behavior requires that paths be relative to the testSuite
        // location specified in the JTE or on the cmd line
        for (int i = 0; i < ff.length; i++) {
            ff[i] = new File(makeLegacyTsRelative(f[i]));
        }
        setExcludeFiles(ff);
    }

    //---------------------------------------------------------------------

    private void setKeywords(String op, String value) {
        if (op == null || op.equals("ignore")) {
            setKeywordsMode(NO_KEYWORDS);
        } else if (op.equals("expr")) {
            setKeywords(EXPR, value);
        } else if (op.equals("allOf")) {
            setKeywords(ALL_OF, value);
        } else if (op.equals("anyOf")) {
            setKeywords(ANY_OF, value);
        } else {
            setKeywordsMode(NO_KEYWORDS);
        }
    }

    //---------------------------------------------------------------------

    private void setPriorStatusValues(String op, String... values) {
        if (op == null || !op.equals("allOf")) {
            setPriorStatusValues((boolean[]) null);
        } else {
            boolean[] b = new boolean[Status.NUM_STATES];
            for (int i = 0; i < values.length; i++) {
                b[i] = "true".equals(values[i]);
            }
            setPriorStatusValues(b);
        }
    }

    private void setPriorStatusValues(String values) {
        if (values == null || values.isEmpty()) {
            setPriorStatusValues((boolean[]) null);
        } else {
            boolean[] b = new boolean[Status.NUM_STATES];
            b[Status.PASSED] = values.contains("pass");
            b[Status.FAILED] = values.contains("fail");
            b[Status.ERROR] = values.contains("erro");
            b[Status.NOT_RUN] = values.contains("notr");
            setPriorStatusValues(b);
        }
    }

    private void setConcurrency(String conc) {
        if (conc == null) {
            setConcurrency(1);
        } else {
            try {
                setConcurrency(Integer.parseInt(conc));
            } catch (NumberFormatException e) {
                concurrencyError = i18n.getString("fp.badConcurrency", conc);
            }
        }
    }

    private void setTimeoutFactor(String tf) {
        if (tf == null) {
            setTimeoutFactor(1);
        } else {
            try {
                setTimeoutFactor(Float.parseFloat(tf));
            } catch (NumberFormatException e) {
                timeoutFactorError = i18n.getString("fp.badTimeoutFactor", tf);
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Get the report directory given in the parameters.
     *
     * @return the report directory
     * @see #setReportDir
     */
    public File getReportDir() {
        return reportDir;
    }

    /**
     * Set the report directory.
     *
     * @param dir the report directory
     * @see #getReportDir
     */
    public void setReportDir(File dir) {
        // check report dir exists?
        reportDir = dir;
    }

    private void setReportDir(String dir) {
        if (dir == null) {
            setReportDir((File) null);
        } else {
            setReportDir(new File(dir));
        }
    }

    /**
     * Makes the given path relative to the user's CWD if it is a
     * relative value.  So "tests" may be turned into
     * {@code /home/me/mytck/tests}.
     */
    private String adjustPath(String path) {
        if (path == null) {
            return path;
        }

        File p = new File(path);
        if (p.isAbsolute()) {
            return path;
        } else {
            String userDir = System.getProperty("user.dir");
            if (userDir == null) {
                return path;
            } else {
                return userDir + File.separator + path;
            }
        }
    }

    private String makeLegacyTsRelative(String path) {
        if (path == null) {
            return path;
        }

        File p = new File(path);
        if (p.isAbsolute()) {
            return path;
        } else {
            if (legacyTsPath == null) {
                return path;
            } else {
                return legacyTsPath + File.separator + path;
            }
        }
    }
}
