/*
 * $Id$
 *
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.audit;

import com.sun.javatest.ExcludeList;
import com.sun.javatest.Parameters;
import com.sun.javatest.Status;
import com.sun.javatest.TestDescription;
import com.sun.javatest.TestFilter;
import com.sun.javatest.TestFinder;
import com.sun.javatest.TestFinderQueue;
import com.sun.javatest.TestResult;
import com.sun.javatest.TestSuite;
import com.sun.javatest.WorkDirectory;
import com.sun.javatest.util.I18NResourceBundle;
import com.sun.javatest.util.StringArray;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Analyze a set of test results for validity.
 * Based on the given parameters, a test finder is run over the test suite
 * to determine which tests are supposed to have been run, and the work
 * directory is checked to see that the corresponding test has been run,
 * successfully. Various statistics are collected and can be printed or
 * accessed for external analysis.
 */
public class Audit {
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Audit.class);
    private int testCount;
    private int[] checksumCounts = new int[TestResult.NUM_CHECKSUM_STATES];
    private int[] envCounts = new int[2];
    private int[] statusCounts = new int[5];
    private boolean badDates = false;
    private TestDescription[] badTests;
    private TestResult[] badTestCaseTests;
    private TestResult[] badTestDescriptions;
    private TestResult[] badChecksumTests;
    private Date earliestStart;
    private Date latestStart;
    private DateFormat[] dateFormats;
    private Hashtable<String, Vector<String>> envTable = new Hashtable<>();
    private PrintStream out;

    /**
     * Analyze a set of test results for validity, based on the given parameters.
     *
     * @param params Parameters to define the test finder and work directory
     *               used in the analysis.
     * @throws TestSuite.Fault if there is a problem opening the test suite given in the parameters
     */
    public Audit(Parameters params) {
        this(getTestFinderQueue(params),
                params.getExcludeList(),
                params.getWorkDirectory());
    }

    /**
     * Analyze a set of test results for validity, based on the given parameters.
     *
     * @param tfq         An enumerator for the set of tests to be run
     * @param excludeList The excludeList against which to check excluded test cases
     * @param workDir     The set of results
     */
    public Audit(TestFinderQueue tfq, ExcludeList excludeList, WorkDirectory workDir) {
        Vector<TestResult> badChecksumTestsV = new Vector<>();
        Vector<TestResult> badTestDescriptionsV = new Vector<>();
        Vector<TestResult> badTestCaseTestsV = new Vector<>();
        Vector<TestDescription> badTestsV = new Vector<>();

        workDir.getTestResultTable().waitUntilReady();

        TestDescription td;
        while ((td = tfq.next()) != null) {
            try {
                TestResult tr = new TestResult(workDir, TestResult.getWorkRelativePath(td));

                testCount++;
                statusCounts[tr.getStatus().getType()]++;
                checksumCounts[tr.getChecksumState()]++;

                if (tr.getChecksumState() == TestResult.BAD_CHECKSUM) {
                    badChecksumTestsV.add(tr);
                }

                if (!equal(td, tr.getDescription())) {
                    badTestDescriptionsV.add(tr);
                }

                if (!checkTestCases(tr, excludeList)) {
                    badTestCaseTestsV.add(tr);
                }

                Map<String, String> trEnv = tr.getEnvironment();
                for (Map.Entry<String, String> e : trEnv.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    Vector<String> allValuesForKey = envTable.get(key);
                    if (allValuesForKey == null) {
                        allValuesForKey = new Vector<>();
                        envTable.put(key, allValuesForKey);
                    }
                    if (!allValuesForKey.contains(value)) {
                        allValuesForKey.add(value);
                    }
                }

                String start = tr.getProperty(TestResult.START);
                if (start == null) {
                    badDates = true;
                } else {
                    Date d = parseDate(start);
                    if (d == null) {
                        badDates = true;
                    } else {
                        if (earliestStart == null || d.before(earliestStart)) {
                            earliestStart = d;
                        }
                        if (latestStart == null || d.after(latestStart)) {
                            latestStart = d;
                        }
                    }
                }
            } catch (TestResult.Fault e) {
                //System.err.println(td.getRootRelativeURL() + " " + TestResult.getWorkRelativePath(td) + " " + e);
                badTestsV.add(td);
            }
        }

        for (Enumeration<String> e = envTable.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            Vector<String> allValuesForKey = envTable.get(key);
            envCounts[allValuesForKey.size() == 1 ? 0 : 1]++;
        }

        if (!badChecksumTestsV.isEmpty()) {
            badChecksumTests = badChecksumTestsV.toArray(new TestResult[badChecksumTestsV.size()]);
        }

        if (!badTestDescriptionsV.isEmpty()) {
            badTestDescriptions = badTestDescriptionsV.toArray(new TestResult[badTestDescriptionsV.size()]);
        }

        if (!badTestCaseTestsV.isEmpty()) {
            badTestCaseTests = badTestCaseTestsV.toArray(new TestResult[badTestCaseTestsV.size()]);
        }

        if (!badTestsV.isEmpty()) {
            badTests = badTestsV.toArray(new TestDescription[badTestsV.size()]);
        }
    }

    /**
     * Create a test finder queue based on the info in the parameters
     */
    private static TestFinderQueue getTestFinderQueue(Parameters params) {
        TestSuite ts = params.getTestSuite();
        TestFinder tf = ts.getTestFinder();

        TestFinderQueue tfq = new TestFinderQueue();
        tfq.setTestFinder(tf);

        String[] tests = params.getTests();
        tfq.setTests(tests);

        TestFilter[] filters = params.getFilters();
        tfq.setFilters(filters);

        return tfq;
    }

    private static boolean equal(TestDescription a, TestDescription b) {
        if (a == null || b == null) {
            return a == b;
        }

        //if (!a.rootRelativeFile.equals(b.rootRelativeFile))
        //    return false;

        Iterator<String> eA = a.getParameterKeys();
        Iterator<String> eB = b.getParameterKeys();
        while (eA.hasNext() && eB.hasNext()) {
            String keyA = eA.next();
            String keyB = eB.next();
            if (!keyA.equals(keyB)) {
                //System.err.println("mismatch " + a.getRootRelativePath() + " a:" + keyA + " b:" + keyB);
                return false;
            }

            String valA = a.getParameter(keyA);
            String valB = a.getParameter(keyB);
            if (!(valA.equals(valB) || (keyA.equals("keywords") && keywordMatch(valA, valB)))) {
                //System.err.println("mismatch " + a.getRootRelativePath() + " key:" + keyA + " a:" + valA + " b:" + valB);
                return false;
            }
        }

        return true;
    }

    private static boolean keywordMatch(String a, String b) {
        // eek, not very efficient!
        String[] aa = StringArray.split(a);
        Arrays.sort(aa);
        String[] bb = StringArray.split(b);
        Arrays.sort(bb);
        return Arrays.equals(aa, bb);
    }

    /**
     * Get a list of tests that had bad checksums during the analysis.
     *
     * @return A set of tests, or null if none.
     */
    public TestResult[] getBadChecksumTests() {
        return badChecksumTests;
    }

    /**
     * Get a list of tests that had bad test descriptions during the analysis.
     *
     * @return A set of tests, or null if none.
     */
    public TestResult[] getBadTestDescriptions() {
        return badTestDescriptions;
    }

    /**
     * Get a list of tests that gave problems during the analysis.
     *
     * @return A set of tests, or null if none.
     */
    public TestDescription[] getBadTests() {
        return badTests;
    }

    /**
     * Get a list of tests that were executed with too many test cases
     * excluded.
     *
     * @return A set of tests, or null if none.
     */
    public TestResult[] getBadTestCaseTests() {
        return badTestCaseTests;
    }

    /**
     * Get the statistics about the test result checksums.
     *
     * @return An array of counts, indexed by
     * TestResult.GOOD_CHECKSUM, TestResult.BAD_CHECKSUM, etc
     */
    public int[] getChecksumCounts() {
        return checksumCounts;
    }

    /**
     * Get the statistics about the environment values used by the test results.
     *
     * @return An array of two integers; the first gives the number of env
     * values that were uniquely defined across all test results, the second gives
     * the number that were multiply defined across the results.
     */
    public int[] getEnvCounts() {
        return envCounts;
    }

    /**
     * Get the composite set of environment values used by the tests.
     *
     * @return A table of values.
     * The keys to the table are strings; the values are vectors of strings
     * containing the various values for that key.
     */
    public Hashtable<String, Vector<String>> getEnvTable() {
        return envTable;
    }

    /**
     * Get the statistics about the test results.
     *
     * @return An array of counts, indexed by Status.PASSED, Status.FAILED, etc
     */
    public int[] getStatusCounts() {
        return statusCounts;
    }

    /**
     * Get earliest recorded start time for a test.
     *
     * @return The earliest recorded valid start time for a test,
     * or null, if none was found.
     */
    public Date getEarliestStartTime() {
        return earliestStart;
    }

    /**
     * Get latest recorded start time for a test.
     *
     * @return The latest recorded valid start time for a test,
     * or null, if none was found.
     */
    public Date getLatestStartTime() {
        return latestStart;
    }

    public boolean hasBadStartTimes() {
        return badDates;
    }

    /**
     * Get an overall thumps-up/thumbs-down for the analysis.
     * It is a composite of the other isXXXOK() methods.
     *
     * @return true if all checks are OK
     */
    public boolean isOK() {
        return isStatusCountsOK()
                && isChecksumCountsOK()
                && isDateStampsOK()
                && isAllTestsOK()
                && isAllTestCasesOK()
                && isAllTestDescriptionsOK();
    }

    /**
     * Determine if all tests were analyzed successfully.
     *
     * @return true if all tests were analyzed successfully.
     */
    public boolean isAllTestsOK() {
        return badTests == null;
    }

    /**
     * Determine if all test cases were correctly executed for those
     * tests that support test cases.
     *
     * @return true if all test cases were correctly executed for those
     * tests that support test cases.
     */
    public boolean isAllTestCasesOK() {
        return badTestCaseTests == null;
    }

    /**
     * Determine if all test descriptions were OK.
     *
     * @return true is all test descriptions were OK.
     */
    public boolean isAllTestDescriptionsOK() {
        return badTestDescriptions == null;
    }

    /**
     * Determine if the checksum counts are acceptable.
     * This is currently defined as "no bad checksums";
     * in time, it should become "all good checksums".
     *
     * @return true is there were no test results with bad checksums.
     */
    public boolean isChecksumCountsOK() {
        return checksumCounts[TestResult.BAD_CHECKSUM] == 0;
    }

    /**
     * Determine if all the test results have valid date stamps.
     * "Valid" just means present and parseable dates: no restriction
     * on the value is currently imposed.
     *
     * @return true if no dates have invalid datestamps.
     */
    public boolean isDateStampsOK() {
        return earliestStart != null && latestStart != null && !badDates;
    }

    /**
     * Determine if the test result outcomes are acceptable.
     * All must pass, none must fail.
     *
     * @return true if all necessary tests passed.
     */
    public boolean isStatusCountsOK() {
        if (testCount == 0) {
            return false;
        }

        for (int i = 0; i < statusCounts.length; i++) {
            if (i != Status.PASSED && statusCounts[i] != 0) {
                return false;
            }
        }
        return statusCounts[Status.PASSED] == testCount;
    }

    /**
     * Print out a report of the analysis.
     *
     * @param out                   A stream to which to write the output.
     * @param showAllEnvValues      Include a listing of all environment values used
     *                              by the collected set of tests.
     * @param showMultipleEnvValues Include a listing of those environment values
     *                              which were multiply defined by the collected set of tests.
     */
    public synchronized void report(PrintStream out,
                                    boolean showAllEnvValues,
                                    boolean showMultipleEnvValues) {
        this.out = out;

        showResultCounts();
        showChecksumCounts();
        showDateStampInfo();
        showEnvCounts();

        showBadChecksums();
        showBadTestDescriptions();
        showBadTestCaseTests();
        showBadTests();

        if (showAllEnvValues || showMultipleEnvValues) {
            showEnvValues(showAllEnvValues);
        }
    }

    /**
     * Print out a short summary about any tests with bad checksums
     */
    private void showBadChecksums() {
        if (badChecksumTests != null) {
            out.println("The following " + badChecksumTests.length + " tests had bad checksums.");
            for (TestResult tr : badChecksumTests) {
                out.println(tr.getWorkRelativePath());
            }
        }
    }

    /**
     * Print out a short summary about any tests with bad test descriptions
     */
    private void showBadTestDescriptions() {
        if (badTestDescriptions != null) {
            out.println("The following " + badTestDescriptions.length + " tests had bad test descriptions.");
            for (TestResult tr : badTestDescriptions) {
                out.println(tr.getWorkRelativePath());
            }
        }
    }

    /**
     * Print out a short summary report about any tests with too many test cases
     * excluded.
     */
    private void showBadTestCaseTests() {
        if (badTestCaseTests != null) {
            out.println(i18n.getString("adt.tooManyTestCases", Integer.valueOf(badTestCaseTests.length)));
            for (TestResult tr : badTestCaseTests) {
                out.println(tr.getWorkRelativePath());
            }
        }
    }

    /**
     * Print out a short summary report about any tests that gave problems
     * during the analysis.
     */
    private void showBadTests() {
        if (badTests != null) {
            out.println(i18n.getString("adt.badTests", Integer.valueOf(badTests.length)));
            for (TestDescription td : badTests) {
                out.println(TestResult.getWorkRelativePath(td));
            }
        }
    }

    /**
     * Print out a short summary report about the earliest and latest
     * start times for the test results.
     */
    private void showDateStampInfo() {
        if (earliestStart == null || latestStart == null) {
            out.println(i18n.getString("adt.noDateStamps"));
        } else {
            Integer b = Integer.valueOf(badDates ? 1 : 0);
            out.println(i18n.getString("adt.earliestResult",
                    earliestStart, b));
            out.println(i18n.getString("adt.latestResult",
                    latestStart, b));
            if (badDates) {
                out.println(i18n.getString("adt.badDateStamps"));
            }
        }
    }

    /**
     * Print out a short summary report of the environment statistics.
     */
    private void showEnvCounts() {
        int u = envCounts[0];
        int m = envCounts[1];
        if (u + m > 0) {
            if (m == 0) {
                out.println(i18n.getString("adt.env.allOK"));
            } else {
                out.println(i18n.getString("adt.env.count",
                        Integer.valueOf(u), Integer.valueOf((u > 0 && m > 0) ? 1 : 0), Integer.valueOf(m)));
            }
        }
    }

    /**
     * Print out a listing of some or all of the env values used by the tests.
     *
     * @param showAll show all environment values (uniquely and multiply defined.)
     *                The default is to just show the multiple defined values.
     */
    private void showEnvValues(boolean showAll) {
        out.println();
        out.print(i18n.getString("adt.envList.title"));

        SortedSet<String> ss = new TreeSet<>();
        for (Enumeration<String> e = envTable.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            ss.add(key);
        }

        for (String key : ss) {
            Vector<String> allValuesForKey = envTable.get(key);
            if (allValuesForKey.size() == 1) {
                if (showAll) {
                    out.println(i18n.getString("adt.envKeyValue",
                            key, allValuesForKey.get(0)));
                }
            } else {
                out.println(i18n.getString("adt.envKey", key));
                for (String s : allValuesForKey) {
                    out.println(i18n.getString("adt.envValue", s));
                }
            }
        }
    }

    /**
     * Print out a short summary report of the checksum statistics.
     */
    private void showChecksumCounts() {
        if (testCount > 0) {
            int g = checksumCounts[TestResult.GOOD_CHECKSUM];
            int b = checksumCounts[TestResult.BAD_CHECKSUM];
            int n = checksumCounts[TestResult.NO_CHECKSUM];
            if (b == 0 && n == 0) {
                out.println(i18n.getString("adt.cs.allOK"));
            } else {
                out.println(i18n.getString("adt.cs.count",
                        Integer.valueOf(g), Integer.valueOf((g > 0) && (b + n > 0) ? 1 : 0), Integer.valueOf(b), Integer.valueOf((b > 0) && (n > 0) ? 1 : 0), Integer.valueOf(n)));
            }
        }
    }

    /**
     * Print out a short summary report of the test result status statistics.
     */
    private void showResultCounts() {
        if (testCount == 0) {
            out.println(i18n.getString("adt.status.noTests"));
        } else {
            int p = statusCounts[Status.PASSED];
            int f = statusCounts[Status.FAILED];
            int e = statusCounts[Status.ERROR];
            int nr = statusCounts[Status.NOT_RUN];

            if (p == testCount) {
                out.println(i18n.getString("adt.status.allOK"));
            } else {
                out.println(i18n.getString("adt.status.count",
                        Integer.valueOf(p), Integer.valueOf((p > 0) && (f + e + nr > 0) ? 1 : 0), Integer.valueOf(f), Integer.valueOf((f > 0) && (e + nr > 0) ? 1 : 0), Integer.valueOf(e), Integer.valueOf((e > 0) && (nr > 0) ? 1 : 0), Integer.valueOf(nr)));
            }
        }
    }

    /**
     * Print out a labelled count if non-zero
     *
     * @param needSep Need a leading separator (comma)
     * @param msg   The label to display
     * @param count   The count to display, if non-zero
     * @return true if a separator will be needed for the next value to be shown
     */
    private boolean showCount(String msg, boolean needSep, int count) {
        if (count == 0) {
            return needSep;
        } else {
            out.print(i18n.getString(msg,
                    Integer.valueOf(needSep ? 1 : 0), Integer.valueOf(count)));
            return true;
        }
    }

    private boolean checkTestCases(TestResult tr, ExcludeList excludeList)
            throws TestResult.Fault {
        // If no test cases excluded when test was run, then OK.
        // (This is the typical case.)
        String[] etcTest;
        String etcTestProp = tr.getProperty("excludedTestCases");
        if (etcTestProp == null) {
            return true;
        } else {
            etcTest = StringArray.split(etcTestProp);
        }

        // This next test is probably redundant, assuming the excludeList is
        // the same as that used to locate the tests, but check anyway:
        // if test is now completely excluded, then OK.
        if (excludeList.excludesAllOf(tr.getDescription())) {
            return true;
        }

        String[] etcTable = excludeList.getTestCases(tr.getDescription());
        // null indicates test not found in table, or the entire test is excluded
        // (without specifying test cases.)  Because of the previous check, the
        // latter cannot be the case, so null means the test is not present
        // in the exclude list. Since we also have checked that the test has
        // excluded test cases that means we have a problem...
        if (etcTable == null) {
            return false;
        }

        // now check that etcTest is a subset of etcTable
        nextTestCase:
        for (String anEtcTest : etcTest) {
            for (String anEtcTable : etcTable) {
                if (anEtcTest.equals(anEtcTable)) {
                    continue nextTestCase;
                }
            }
            // etcTest[i] was not found in etcTable;
            // that means we have a problem
            return false;
        }

        // if we're here, we found all the test cases that were actually
        // excluded were all validly excluded, according to excludedTestCases.
        return true;
    }

    private Date parseDate(String s) {
        if (dateFormats == null) {
            initDateFormats();
        }

        for (int i = 0; i < dateFormats.length; i++) {
            try {
                Date d = dateFormats[i].parse(s);
                // successfully parsed the date; shuffle the format to the front
                // to speed up future parses, assuming dates will likely be similar
                if (i > 0) {
                    DateFormat tmp = dateFormats[i];
                    System.arraycopy(dateFormats, 0, dateFormats, 1, i);
                    dateFormats[0] = tmp;
                }
                return d;
            } catch (ParseException e) {
                //System.err.println("pattern: " + ((SimpleDateFormat)dateFormats[i]).toPattern());
                //System.err.println("  value: " + s);
                //System.err.println("example: " + dateFormats[i].format(new Date()));
            }
        }
        return null;
    }

    private void initDateFormats() {
        // Create an array of possible date formats to parse dates in .jtr files.
        // Most likely is Unix C time in English; the array will be reordered in use
        // by moving the recently used entries to the front of the array.
        Vector<DateFormat> v = new Vector<>();

        // generic Java default
        // 10-Sep-99 3:25:11 PM
        v.add(DateFormat.getDateTimeInstance());
        v.add(DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                Locale.ENGLISH));

        // standard IETF date syntax
        // Fri, 10 September 1999 03:25:12 PDT
        v.add(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm:ss zzz"));
        v.add(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm:ss zzz", Locale.ENGLISH));

        // Unix C time
        // Fri Sep 10 14:41:37 PDT 1999
        v.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"));
        v.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH));

        // allow user-specified format
        String s = System.getProperty("javatest.date.format");
        if (s != null) {
            v.add(new SimpleDateFormat(s));
        }

        dateFormats = v.toArray(new DateFormat[v.size()]);
    }
}
