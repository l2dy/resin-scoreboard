/*
 * Copyright (c) 2022 l2dy
 * Copyright (c) 1998-2018 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 */

package io.sourceforge.l2dy.resin;

import com.caucho.admin.thread.ResinThreadActivityReport;
import com.caucho.admin.thread.ThreadActivityGroup;
import com.caucho.admin.thread.ThreadSnapshot;
import com.caucho.admin.thread.filter.AnyThreadFilter;
import com.caucho.admin.thread.filter.CauchoThreadFilter;
import com.caucho.admin.thread.filter.PortThreadFilter;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import io.sourceforge.l2dy.resin.beans.RemoteConnection;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ThreadActivityReport extends ResinThreadActivityReport {
    private static final Logger log = Logger.getLogger(ThreadActivityReport.class.getName());

    public ThreadActivityGroup[] execute(ThreadMXBean threadMXBean, ServerMXBean serverMXBean, boolean greedy) {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        if (threadInfos == null || threadInfos.length == 0) {
            log.fine("execute failed: ThreadMXBean.dumpAllThreads produced no results");
            return null;
        }

        ThreadSnapshot[] threads = createThreadSnapshots(serverMXBean, threadInfos);

        ThreadActivityGroup[] groups = partitionThreads(serverMXBean, threads, greedy);

        return groups;
    }

    private ThreadSnapshot[] createThreadSnapshots(ServerMXBean serverMXBean, ThreadInfo[] threadInfos) {
        Map<Long, TcpConnectionInfo> connectionsById = getConnectionsById(serverMXBean);

        ThreadSnapshot[] threads = new ThreadSnapshot[threadInfos.length];

        for (int i = 0; i < threadInfos.length; i++) {
            threads[i] = new ThreadSnapshot(threadInfos[i]);

            TcpConnectionInfo connectionInfo = connectionsById.get(threadInfos[i].getThreadId());

            if (connectionInfo != null) threads[i].setConnectionInfo(connectionInfo);

            assignActivityCode(threads[i]);
        }

        return threads;
    }

    private Map<Long, TcpConnectionInfo> getConnectionsById(ServerMXBean serverMXBean) {
        Map<Long, TcpConnectionInfo> connectionInfoMap = new HashMap<Long, TcpConnectionInfo>();

        if (serverMXBean != null) {
            PortMXBean[] ports = serverMXBean.getPorts();
            MBeanServerConnection connection = RemoteConnection.getServerConnection();

            if (ports != null) {
                for (PortMXBean portMXBean : ports) {
                    PortMXBean port = JMX.newMBeanProxy(connection, portMXBean.getObjectName(), PortMXBean.class);
                    TcpConnectionInfo[] connectionInfos = port.connectionInfo();

                    if (connectionInfos != null) {
                        for (TcpConnectionInfo connectionInfo : connectionInfos) {
                            long threadId = connectionInfo.getThreadId();

                            connectionInfoMap.put(threadId, connectionInfo);
                        }
                    }
                }
            }
        }

        return connectionInfoMap;
    }

    private ThreadActivityGroup[] createGroups(ServerMXBean serverMXBean) {
        List<ThreadActivityGroup> groups = new ArrayList<ThreadActivityGroup>();

        if (serverMXBean != null) {
            PortMXBean[] ports = serverMXBean.getPorts();

            if (ports != null) {
                for (PortMXBean port : ports) {
                    String portName = (port.getAddress() == null ? "*" : port.getAddress()) + ":" + port.getPort();

                    String groupName = "Port " + portName + " Threads";

                    PortThreadFilter filter = new PortThreadFilter(portName);
                    ThreadActivityGroup group = new ThreadActivityGroup(groupName, filter);

                    groups.add(group);
                }
            }
        }


        CauchoThreadFilter cauchoFilter = new CauchoThreadFilter();
        groups.add(new ThreadActivityGroup("Resin Threads", cauchoFilter));

        AnyThreadFilter miscFilter = new AnyThreadFilter();
        groups.add(new ThreadActivityGroup("Other Threads", miscFilter));

        ThreadActivityGroup[] array = new ThreadActivityGroup[groups.size()];
        groups.toArray(array);

        return array;
    }

    private ThreadActivityGroup[] partitionThreads(ServerMXBean serverMXBean, ThreadSnapshot[] threads, boolean greedy) {
        ThreadActivityGroup[] groups = createGroups(serverMXBean);

        for (ThreadSnapshot thread : threads) {
            for (ThreadActivityGroup group : groups) {
                boolean added = group.addIfMatches(thread);
                if (added && greedy) break;
            }
        }

        return groups;
    }
}
