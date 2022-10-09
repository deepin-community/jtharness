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
package com.sun.javatest.exec;

import com.sun.javatest.Parameters;
import com.sun.javatest.TestFilter;
import com.sun.javatest.TestResult;
import com.sun.javatest.TestResultTable;
import com.sun.javatest.tool.UIFactory;
import com.sun.javatest.util.Debug;
import com.sun.javatest.util.DynamicArray;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.EventQueue;
import java.util.Enumeration;

/**
 * Tree to render test structure.
 */
class TestTree extends JTree {
    //protected static boolean debug = boolean.getboolean("debug." + testtree.class.getname());
    private static int debug = Debug.getInt("debug." + TestTree.class.getName());
    private UIFactory uif;
    private TestTreeModel currModel;
    private TreePanelModel tpm;
    private FilterSelectionHandler filterHandler;
    private EventWatcher watcher = new EventWatcher();

    /**
     * @param uif       The UI factory object to use.
     * @param model     The GUI model object.
     * @param fh   The active filter configuration object.
     * @param treeModel The data model.
     */
    public TestTree(UIFactory uif, TreePanelModel model, FilterSelectionHandler fh,
                    TestTreeModel treeModel) {
        super();

        this.uif = uif;
        this.tpm = model;
        this.filterHandler = fh;

        filterHandler.addObserver(watcher);

        //currModel = new TestTreeModel(null);
        currModel = treeModel;
        setModel(currModel);
        setLargeModel(true);
        setScrollsOnExpand(true);
        setName("tree");
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Call when it is likely that the Parameters or filters have changed internally.
     */
    public void updateGUI() {
        // delete this if the model becomes top-level

        // invalidate the node info cache because we don't know what part
        // of the parameters have changed.  A filter change is likely if
        // the user has just completed an interview
        repaint();
    }

    @Override
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        newModel.addTreeModelListener(watcher);
    }

    // ---------- private ----------
    void setParameters(Parameters p) {
        // this is only here to notify the model when we change things which
        // affect the tree structure itself.
        // otherwise, most changes are the result of filter changes
    }

    /**
     * For proper operation, this method is recommended over the method on JTree.
     *
     * @param mod The new data model.
     */
    void setTreeModel(TestTreeModel mod) {
        if (currModel != null) {
            currModel.removeTreeModelListener(watcher);
        }

        currModel = mod;
        setModel(currModel);
    }

    TestResultTable getTestResultTable() {
        // remove this method when the model becomes independent
        return currModel.getTestResultTable();
    }

    TreePath[] snapshotSelectedPaths() {

        TreeSelectionModel tsm = getSelectionModel();
        if (tsm == null) {
            return null;
        }

        return tsm.getSelectionPaths();

    }

    void restoreSelection(String... selectedUrls) {
        if (selectedUrls == null || selectedUrls.length == 0) {
            return;
        }

        if (currModel instanceof TestTreeModel) {
            final TreePath[] paths = currModel.urlsToPaths(selectedUrls);

            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(() -> restoreSelection(paths));
            } else {
                restoreSelection(paths);
            }
        }
    }

    private void restoreSelection(TreePath... selectedPaths) {
        if (selectedPaths == null || selectedPaths.length == 0) {
            return;
        }

        TreeSelectionModel tsm = getSelectionModel();
        if (tsm == null) {
            return;
        }

        // add or set ???
        tsm.addSelectionPaths(selectedPaths);
    }

    /**
     * Record which tree nodes are currently visible.
     *
     * @see #restorePaths
     */
    TreePath[] snapshotOpenPaths() {
        // currModel.getRoot()==null may indicate that the GUI has been
        // disposed already (?).  We have seen exceptions in the code
        // below because this situation occurs.
        if (currModel == null || currModel.getRoot() == null) {
            return null;
        }

        TreePath[] paths = new TreePath[0];
        Enumeration<TreePath> e = getDescendantToggledPaths(new TreePath(currModel.getRoot()));

        while (e != null && e.hasMoreElements()) {
            TreePath tp = e.nextElement();
            if (!isVisible(tp))     // if we can't see it, we don't care
            {
                continue;
            }
            if (!isExpanded(tp))    // if it's not expanded, we don't need it
            {
                continue;
            }

            paths = DynamicArray.append(paths, tp);
        }   // while

        return paths;
    }

    /**
     * Restore open paths in the tree.  Should be called on the event thread.
     *
     * @param openUrls The path urls to restore.
     * @param queue    True if the request should be put into the event queue at the
     *                 end - this can be important for proper queuing of operations.
     */
    void restorePaths(String[] openUrls, boolean queue) {
        if (openUrls == null || openUrls.length == 0) {
            return;
        }

        if (currModel instanceof TestTreeModel) {
            final TreePath[] paths = currModel.urlsToPaths(openUrls);

            if (!EventQueue.isDispatchThread() || queue) {
                EventQueue.invokeLater(() -> restorePaths(paths));
            } else {
                restorePaths(paths);
            }
        }
    }

    /**
     * Attempt to restore paths which were previously recorded.
     *
     * @see #snapshotOpenPaths
     */
    void restorePaths(TreePath... paths) {
        // make sure root still matches
        if (paths == null || paths.length == 0) {
            return;
        }

        for (TreePath path : paths) {
            if (path.getPathCount() == 1) {
                continue;
            }

            //expandPath(paths[i]);
            setExpandedState(path, true);
            makeVisible(path);
        }   // for
    }

    private class EventWatcher implements TreeModelListener, FilterSelectionHandler.Observer {
        // TreeModelListener - on event thread
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            // NOTE: cannot get selected item from tree because if it has
            // been replaced already, there may be nothing selected
            String activeTest = tpm.getSelectedTest();
            if (activeTest == null) {
                return;
            }

            Object[] targets = e.getChildren();
            if (targets == null) {
                return;     // don't care then
            }

            for (Object target : targets) {
                if (target instanceof TT_TestNode) {
                    TestResult tr = ((TT_TestNode) target).getTestResult();

                    if (tr.getTestName().equals(activeTest)) {
                        tpm.showTest(tr);
                    }
                }
            }

            repaint();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            // NOTE: cannot get selected item from tree because if it has
            // been replaced already, there may be nothing selected
            String activeTest = tpm.getSelectedTest();
            if (activeTest == null) {
                return;
            }

            Object[] targets = e.getChildren();
            if (targets == null) {
                return;     // don't care then
            }

            for (Object target : targets) {
                if (target instanceof TT_TestNode) {
                    TestResult tr = ((TT_TestNode) target).getTestResult();

                    if (tr.getTestName().equals(activeTest)) {
                        tpm.showTest(tr);
                    }
                }
            }
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            // XXX need to handle case when shown node is removed
            // select root in that case?
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            TreePath[] tp = snapshotOpenPaths();

            TreePath[] selected = snapshotSelectedPaths();

            //tpm.showNode(getModel().getRoot(), new TreePath(getModel().getRoot()));

            if (tp != null && tp.length != 0) {
                restorePaths(tp);
            }

            restoreSelection(selected);
        }

        // FilterConfig.Observer - may not be on event thread
        @Override
        public void filterUpdated(TestFilter f) {
            updateGUI();
        }

        @Override
        public void filterSelected(TestFilter f) {
            updateGUI();
        }

        @Override
        public void filterAdded(TestFilter f) {
            // don't care
        }

        @Override
        public void filterRemoved(TestFilter f) {
            // don't care
        }

    }
}
