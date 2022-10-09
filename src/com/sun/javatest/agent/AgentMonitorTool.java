/*
 * $Id$
 *
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javatest.agent;

import com.sun.javatest.Status;
import com.sun.javatest.tool.Tool;
import com.sun.javatest.util.StringArray;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import static com.sun.javatest.agent.Agent.MILLIS_PER_SECOND;

/**
 * The "Agent Monitor" tool, which allows a user to monitor and control
 * the agents used to help run tests.
 */
public class AgentMonitorTool extends Tool {
    /*OLD
    static AgentMonitorTool access() {
        if (theOne == null)
            theOne = new AgentMonitorTool();
        return theOne;
    }
    */

    /*OLD
    private static AgentMonitorTool theOne;
    */

    private AgentManager agentManager = AgentManager.access();
    private ActiveAgentPool activeAgentPool = agentManager.getActiveAgentPool();
    private JMenuBar menuBar;
    private AgentPoolSubpanel agentPoolSubpanel;
    private CurrentAgentsSubpanel currAgentsSubpanel;
    AgentMonitorTool(AgentMonitorToolManager m) {
        super(m, "agentMonitor", "agent.window.csh");
        setI18NTitle("tool.title");
        setShortTitle(uif.getI18NString("tool.shortTitle"));
        setLayout(new GridBagLayout());

        menuBar = new JMenuBar();
        menuBar.add(uif.createHorizontalGlue("tool.glue"));

        JMenu helpMenu = uif.createMenu("tool.help");
        helpMenu.add(uif.createHelpMenuItem("tool.help.window", "agent.window.csh"));
        menuBar.add(helpMenu);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;

        agentPoolSubpanel = new AgentPoolSubpanel();
        add(agentPoolSubpanel, c);

        currAgentsSubpanel = new CurrentAgentsSubpanel();
        add(currAgentsSubpanel, c);
    }

    @Override
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    @Override
    public void save(Map<String, String> m) {
        int port = agentPoolSubpanel.getPort();
        if (port != Integer.MIN_VALUE) {
            m.put("port", String.valueOf(port));
        }
        int timeout = agentPoolSubpanel.getTimeout();
        if (timeout != Integer.MIN_VALUE) {
            m.put("timeout", String.valueOf(timeout));
        }
        m.put("listening", String.valueOf(agentPoolSubpanel.isListening()));
    }

    /**
     * Restore an AgentMonitorTool from its saved state.
     *
     * @param m The map containing the saved data
     */
    @Override
    protected void restore(Map<String, String> m) {
        String l = m.get("listening");
        try {
            String p = m.get("port");
            if (p != null && !p.isEmpty()) {
                agentPoolSubpanel.setPort(Integer.parseInt(p));
            }

            String t = m.get("timeout");
            if (t != null && !t.isEmpty()) {
                agentPoolSubpanel.setTimeout(Integer.parseInt(t));
            }

            if (l != null && !l.isEmpty()) {
                agentPoolSubpanel.setListening(l.equals("true"));
            }
        } catch (NumberFormatException ignore) {
        }
    }

    //----------nested classes-------------------------------------------------------

    private class AgentPoolSubpanel extends JPanel
            implements ItemListener, ActiveAgentPool.Observer {
        private JCheckBox listeningCheck;
        private JLabel portLabel;
        private JTextField portField;
        private JLabel timeoutLabel;
        private JTextField timeoutField;
        private JList<String> list;
        private DefaultListModel<String> listData;

        AgentPoolSubpanel() {
            setName("tool.pool");
            setBorder(uif.createTitledBorder("tool.pool"));
            setLayout(new GridBagLayout());
            uif.setAccessibleDescription(this, "tool.pool");

            GridBagConstraints lc = new GridBagConstraints();
            GridBagConstraints fc = new GridBagConstraints();
            fc.insets.right = 10;

            listeningCheck = uif.createCheckBox("tool.pool.listen", false);
            listeningCheck.setSelected(activeAgentPool.isListening());
            listeningCheck.addItemListener(this);
            add(listeningCheck, fc);

            portLabel = uif.createLabel("tool.pool.port", true);
            add(portLabel, lc);
            portField = uif.createInputField("tool.pool.port", 6);
            portField.setText(String.valueOf(activeAgentPool.getPort()));
            add(portField, fc);
            portLabel.setLabelFor(portField);

            timeoutLabel = uif.createLabel("tool.pool.timeout", true);
            add(timeoutLabel, lc);
            timeoutField = uif.createInputField("tool.pool.timeout", 6);
            // agentMgr API is in msec; field is in seconds
            int t = activeAgentPool.getTimeout(); // round to nearest second
            timeoutField.setText(String.valueOf((t + 500) / 1000));
            fc.anchor = GridBagConstraints.WEST;
            fc.gridwidth = GridBagConstraints.REMAINDER;
            fc.weightx = 1;
            add(timeoutField, fc);
            timeoutLabel.setLabelFor(timeoutField);

            listData = new DefaultListModel<>();
            list = uif.createList("tool.pool", listData);
            list.setPrototypeCellValue("abcdefghiklmnopqrstuvwxyz");
            list.setVisibleRowCount(3);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            add(uif.createScrollPane(list), c);

            // Ensure any existing entries in the pool are displayed
            // Note there is a slight synchronization window between the call of
            // elements and that of addObserver.
            for (Enumeration<Connection> e = activeAgentPool.elements(); e.hasMoreElements(); ) {
                Connection conn = e.nextElement();
                listData.addElement(conn.getName());
            }

            activeAgentPool.addObserver(this);
            enableFields();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getItemSelectable() == listeningCheck) {
                try {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        try {
                            int timeout = Integer.parseInt(timeoutField.getText());

                            if (timeout <= 0) {
                                throw new NumberFormatException();
                            }

                            // field is in seconds; agentMgr API is in msec
                            activeAgentPool.setTimeout(timeout * MILLIS_PER_SECOND);
                        } catch (NumberFormatException ex) {
                            uif.showError("tool.badTimeout");
                            return;
                        }

                        try {
                            int port = Integer.parseInt(portField.getText());
                            activeAgentPool.setPort(port);

                            if (port < 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException ex) {
                            uif.showError("tool.badPort");
                            return;
                        }

                        try {
                            activeAgentPool.setListening(true);
                        } catch (IOException ex) {
                            uif.showError("tool.listenOn", ex);
                            return;
                        }
                    } else {
                        try {
                            activeAgentPool.setListening(false);
                        } catch (IOException ex) {
                            uif.showError("tool.listenOff", ex);
                            return;
                        }
                    }
                } finally {
                    boolean b = activeAgentPool.isListening();
                    if (b != listeningCheck.isSelected()) {
                        listeningCheck.setSelected(b);
                    }

                    enableFields();
                }
            }
        }

        @Override
        public void addedToPool(final Connection c) {
            // item must match that used in removedFromAgentPool
            if (!EventQueue.isDispatchThread()) {
                // run()
                Runnable cmd = () -> AgentPoolSubpanel.this.addedToPool(c);      // end anon. class

                EventQueue.invokeLater(cmd);
            } else {
                listData.addElement(c.getName());
            }
        }

        @Override
        public void removedFromPool(final Connection c) {
            // item must match that used in addedToAgentPool
            if (!EventQueue.isDispatchThread()) {
                // run()
                Runnable cmd = () -> AgentPoolSubpanel.this.removedFromPool(c);      // end anon. class

                EventQueue.invokeLater(cmd);
            } else {
                listData.removeElement(c.getName());
            }
        }

        boolean isListening() {
            return listeningCheck.isSelected();
        }

        void setListening(boolean b) {
            listeningCheck.setSelected(b);
        }

        int getPort() {
            try {
                return Integer.parseInt(portField.getText());
            } catch (NumberFormatException ex) {
                return Integer.MIN_VALUE;
            }
        }

        void setPort(int port) {
            portField.setText(String.valueOf(port));
        }

        int getTimeout() {
            try {
                return Integer.parseInt(timeoutField.getText());
            } catch (NumberFormatException ex) {
                return Integer.MIN_VALUE;
            }
        }

        void setTimeout(int timeout) {
            timeoutField.setText(String.valueOf(timeout));
        }

        private void enableFields() {
            boolean enable = !listeningCheck.isSelected();
            portLabel.setEnabled(enable);
            portField.setEnabled(enable);
            timeoutLabel.setEnabled(enable);
            timeoutField.setEnabled(enable);
        }
    }

    private class CurrentAgentsSubpanel extends JPanel
            implements ListSelectionListener, AgentManager.Observer {
        private JList<Entry> list;
        private DefaultListModel<Entry> listData;
        private Entry selectedEntry;
        private JLabel addressLabel;
        private JTextField addressField;
        private JLabel tagLabel;
        private JTextField tagField;
        private JLabel requestLabel;
        private JTextField requestField;
        private JLabel execLabel;
        private JTextField execField;
        private JLabel argsLabel;
        private JTextField argsField;
        private JCheckBox localizeArgsCheck;
        CurrentAgentsSubpanel() {
            setName("tool.curr");
            setBorder(uif.createTitledBorder("tool.curr"));
            setLayout(new GridBagLayout());
            uif.setToolTip(this, "tool.curr");

            listData = new DefaultListModel<>();
            list = uif.createList("tool.list.curr", listData);
            list.setVisibleRowCount(5);

            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object o, int index, boolean isSelected, boolean cellHasFocus) {
                    String name = o.toString();
                    return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
                }
            });

            list.addListSelectionListener(this);

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets.bottom = 5;
            c.weightx = 1;
            c.weighty = 1;
            add(uif.createScrollPane(list), c);

            GridBagConstraints lc = new GridBagConstraints();
            lc.gridwidth = 1;
            lc.weightx = 0;
            lc.anchor = GridBagConstraints.EAST;
            lc.insets.right = 5;

            GridBagConstraints fc = new GridBagConstraints();
            fc.gridwidth = GridBagConstraints.REMAINDER;
            fc.weightx = 1;
            fc.anchor = GridBagConstraints.WEST;
            fc.fill = GridBagConstraints.HORIZONTAL;

            addressLabel = uif.createLabel("tool.curr.address", true);
            add(addressLabel, lc);
            addressField = uif.createOutputField("tool.curr.address", addressLabel);
            add(addressField, fc);

            tagLabel = uif.createLabel("tool.curr.tag", true);
            add(tagLabel, lc);
            tagField = uif.createOutputField("tool.curr.tag", tagLabel);
            add(tagField, fc);

            requestLabel = uif.createLabel("tool.curr.request", true);
            add(requestLabel, lc);
            requestField = uif.createOutputField("tool.curr.request", requestLabel);
            add(requestField, fc);

            execLabel = uif.createLabel("tool.curr.execute", true);
            add(execLabel, lc);
            execField = uif.createOutputField("tool.curr.execute", execLabel);
            add(execField, fc);

            argsLabel = uif.createLabel("tool.curr.args", true);
            add(argsLabel, lc);
            argsField = uif.createOutputField("tool.curr.args", argsLabel);
            add(argsField, fc);

            add(uif.createGlue("tool.curr.pad"), lc);
            fc.fill = GridBagConstraints.VERTICAL;
            fc.weightx = 0;
            fc.anchor = GridBagConstraints.WEST;
            localizeArgsCheck = uif.createCheckBox("tool.mapArgs", false);
            add(localizeArgsCheck, fc);

            agentManager.addObserver(this);
        }

        @Override
        public synchronized void valueChanged(ListSelectionEvent ev) {
            Entry e = list.getSelectedValue();
            if (e == null) {
                addressField.setText("");
                tagField.setText("");
                requestField.setText("");
                execField.setText("");
                argsField.setText("");
                localizeArgsCheck.setSelected(false);
                selectedEntry = null;
            } else {
                addressField.setText(e.connection.getName());
                tagField.setText(e.tag);
                requestField.setText(e.request);
                execField.setText(e.executable);
                argsField.setText(StringArray.join(e.args));
                localizeArgsCheck.setSelected(e.localizeArgs);
                selectedEntry = e;
            }
        }

        @Override
        public synchronized void started(final Connection c,
                                         final String tag, final String request, final String executable,
                                         final String[] args, final boolean localizeArgs) {
            if (!EventQueue.isDispatchThread()) {
                // run()
                Runnable cmd = () -> CurrentAgentsSubpanel.this.started(c, tag, request, executable, args, localizeArgs);      // end anon. class

                EventQueue.invokeLater(cmd);
            } else {
                listData.addElement(new Entry(c, tag, request, executable, args, localizeArgs));
            }
        }

        @Override
        public synchronized void finished(final Connection c, final Status status) {
            if (!EventQueue.isDispatchThread()) {
                // run()
                Runnable cmd = () -> CurrentAgentsSubpanel.this.finished(c, status);      // end anon. class

                EventQueue.invokeLater(cmd);
            } else {
                for (int i = 0; i < listData.size(); i++) {
                    Entry e = listData.get(i);
                    if (e.connection == c) {
                        listData.removeElement(e);
                        break;
                    }
                }   // for
            }   // else
        }

        private class Entry {
            Connection connection;
            String tag;
            String request;
            String executable;
            String[] args;
            boolean localizeArgs;
            Entry(Connection connection,
                  String tag, String request, String executable, String[] args,
                  boolean localizeArgs) {
                this.connection = connection;
                this.tag = tag;
                this.request = request;
                this.executable = executable;
                this.args = args;
                this.localizeArgs = localizeArgs;
            }

            @Override
            public String toString() {
                return uif.getI18NString("tool.entry",
                        connection.getName(), tag);
            }
        }
    }
}
