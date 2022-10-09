/*
 * $Id$
 *
 * Copyright (c) 2006, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javatest.TemplateUtilities;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileSystemTableModel extends AbstractTableModel {

    // Names of the columns.
    // TODO - i18!
    static protected String[] cNames = {"File Name", "Name", "Description"};

    // Types of the columns.
    static protected Class<?>[] cTypes = {String.class,
            String.class, String.class};

    private FileTableFilter filter = null;

    private FileTableNode root;
    private ArrayList<FileTableNode> data;
    private LinkedHashMap<String, String[]> fileData;
    private boolean allowTraversDirs;
    private File defTemplateDir;

    public FileSystemTableModel(String file, FileTableFilter flt, File defTemplateDir, boolean allowTraversDirs) {
        setFilter(flt);
        this.allowTraversDirs = allowTraversDirs;
        this.defTemplateDir = defTemplateDir;
        init(file);
    }

    @Override
    public void fireTableDataChanged() {
    }

    public void resetTable(String file, FileTableFilter flt) {
        setFilter(flt);
        init(file);
        fireTableChanged(new TableModelEvent(this));
    }

    public void resetTable(String file) {
        init(file);
        fireTableChanged(new TableModelEvent(this));
    }

    @Override
    public int getColumnCount() {
        return cNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return cNames[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return cTypes[column];
    }

    private void setFilter(FileTableFilter flt) {
        filter = flt;
    }

    private void init(String f) {
        File file = new File(f);
        init(file);
    }

    public File getCurrentDir() {
        return root.getFile();
    }

    private void init(File file) {
        root = new FileTableNode(file, 'r');
        data = new ArrayList<>();

//        if(allowTraversDirs) {
//            data.add(new FileTableNode(root.getFile(), 'u'));
//        }
        if (allowTraversDirs || !file.equals(defTemplateDir)) {
            if (file.getParent() != null) {
                data.add(new FileTableNode(file.getParentFile(), 'u'));
            }
        }

        File[] lst = file.listFiles();
        if (lst != null) {
            Arrays.sort(lst);
        }

        if (lst != null && lst.length > 0) {
//            if (allowTraversDirs) {
            for (File aLst1 : lst) {
                if (aLst1.isDirectory()) {
                    data.add(new FileTableNode(aLst1, 'd'));
                }
            }   // for
//            }
            for (File aLst : lst) {
                if (!aLst.isDirectory()) {
                    if (filter == null || filter.isApplicableFile(aLst)) {
                        data.add(new FileTableNode(aLst, 'f'));
                    }
                }
            }   // for
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) {
            return null;
        }
        FileTableNode f = data.get(rowIndex);
        File file = f.getFile();
        if (file == null) {
            return null;
        }
        if (columnIndex == 0) {
            return f;//file.getName();
        }
        if (file.isDirectory()) {
            return "";
        }
        if (columnIndex == 1) {
            return getConfigName(file);
        }
        if (columnIndex == 2) {
            return getConfigDesc(file);
        }
        return null;
    }

    public File getNode(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) {
            return null;
        }
        return data.get(rowIndex).getFile();
    }


    @Override
    public int getRowCount() {
        return data.size();
    }

    private String getConfigName(File file) {
        return getInfo(file)[0];
    }

    private String getConfigDesc(File file) {
        return getInfo(file)[1];
    }

    private String[] getInfo(File file) {

        if (fileData == null) {
            fileData = new LinkedHashMap<String, String[]>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > 500;
                }
            };
        }
        String key = file.getAbsolutePath();
        String[] value = fileData.get(key);
        if (value != null) {
            return value;
        }

        // refresh
        try {
            String[] data = {"", ""};
            /*
            String path = TemplateUtilities.getTemplateFromWd(file);
            if (path != null) {
                TemplateUtilities.ConfigInfo ci = TemplateUtilities.getConfigInfo(new File(path));
                if (ci != null) {
                    data = new String[] {ci.getName(), ci.getDescription()};
                }

            }
            */
            TemplateUtilities.ConfigInfo ci = TemplateUtilities.getConfigInfo(file);
            if (ci != null) {
                data = new String[]{ci.getName(), ci.getDescription()};
            }
            fileData.put(key, data);
            return data;
        } catch (Exception e) {
            return new String[]{"", ""};
        }
    }

    static class FileTableFilter {
        private String extension;

        FileTableFilter(String ext) {
            extension = ext;
        }

        protected boolean isApplicableFile(File f) {
            if (extension == null) {
                return true;
            }
            if (f.isDirectory()) {
                return true;
            }
            return f.getName().endsWith(extension);
        }
    }

}


class FileTableNode {

    File file;

    private char mode;

    public FileTableNode(File file, char mode) {
        this.file = file;
        this.mode = mode;
    }

    @Override
    public String toString() {
        return file.getName();
    }

    public File getFile() {
        return file;
    }

    public char getMode() {
        return mode;
    }

}
