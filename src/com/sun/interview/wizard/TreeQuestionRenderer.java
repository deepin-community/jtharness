/*
 * $Id$
 *
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.interview.wizard;

import com.sun.interview.Question;
import com.sun.interview.TreeQuestion;
import com.sun.interview.wizard.selectiontree.SelectionTree;
import com.sun.interview.wizard.selectiontree.selection.SelectionElement;
import com.sun.interview.wizard.selectiontree.selection.SelectionType;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

public class TreeQuestionRenderer
        implements QuestionRenderer {
    private static final I18NResourceBundle i18n = I18NResourceBundle.getDefaultBundle();

    @Override
    public JComponent getQuestionRendererComponent(Question qq, ActionListener listener) {
        final TreeQuestion tq = (TreeQuestion) qq;

        SelectNode rootNode = new SelectNode(tq.getModel(), tq.getModel().getRoot());
        final SelectionTree tree = new SelectionTree(rootNode, null, true);
        tree.setName("tree");
        tree.setToolTipText(i18n.getString("tree.tip"));
        tree.getAccessibleContext().setAccessibleName(tree.getName());
        tree.getAccessibleContext().setAccessibleDescription(tree.getToolTipText());
        JScrollPane sp = new JScrollPane(tree);

        tree.setSelection(tq.getValue());

        Runnable valueSaver = () -> tq.setValue(tree.getSelection());

        sp.putClientProperty(VALUE_SAVER, valueSaver);

        return sp;
    }

    @Override
    public String getInvalidValueMessage(Question q) {
        return null;
    }
////////-----------------------------------------------------------------------------------------------------------

    protected static class SelectNode implements SelectionElement {
        private SelectionType type = SelectionType.UNSELECTED;
        private Object object;
        private LinkedList<SelectionElement> children;
        private TreeQuestion.Model model;
        private String name;

        public SelectNode(TreeQuestion.Model model, Object object) {
            this.model = model;
            this.object = object;
        }

        @Override
        public SelectionType getSelectionType() {
            return type;
        }

        @Override
        public void setSelectionType(SelectionType selectionType) {
            if (!type.equals(selectionType)) {
                type = selectionType;
            }
        }

        @Override
        public String getDisplayableName() {
            return getName();
        }

        @Override
        public String getToolTip() {
            return null;
        }

        @Override
        public boolean isToolTipAlwaysShown() {
            return false;
        }

        @Override
        public List<SelectionElement> getChildren() {
            if (children == null) {
                initChildren();
            }
            return children;
        }

        private void initChildren() {
            int childCount = model.getChildCount(object);
            children = new LinkedList<>();
            for (int i = 0; i < childCount; i++) {
                SelectNode newChild = new SelectNode(model, model.getChild(object, i));
                children.add(newChild);
                if (type.equals(SelectionType.SELECTED)) {
                    newChild.setSelectionType(SelectionType.SELECTED);
                }
            }
            model.getChild(object, 0);
        }

        private String getName() {
            if (name == null) {
                name = model.getName(object);
            }
            return name;
        }
    }
}
