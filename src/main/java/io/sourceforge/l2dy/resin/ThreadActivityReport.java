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
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import com.caucho.server.cluster.ServletService;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ThreadActivityReport extends ResinThreadActivityReport {
    private static final Logger log = Logger.getLogger(ThreadActivityReport.class.getName());

    public ThreadActivityGroup[] execute(ThreadMXBean threadMXBean, boolean greedy) {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        if (threadInfos == null || threadInfos.length == 0) {
            log.fine("execute failed: ThreadMXBean.dumpAllThreads produced no results");
            return null;
        }

        ThreadSnapshot[] threads = createThreadSnapshots(threadInfos);

        ThreadActivityGroup[] groups = partitionThreads(threads, greedy);

        return groups;
    }

    private ThreadSnapshot[] createThreadSnapshots(ThreadInfo[] threadInfos) {
        Map<Long, TcpConnectionInfo> connectionsById = getConnectionsById();

        ThreadSnapshot[] threads = new ThreadSnapshot[threadInfos.length];

        for (int i = 0; i < threadInfos.length; i++) {
            threads[i] = new ThreadSnapshot(threadInfos[i]);

            TcpConnectionInfo connectionInfo = connectionsById.get(threadInfos[i].getThreadId());

            if (connectionInfo != null) threads[i].setConnectionInfo(connectionInfo);

            assignActivityCode(threads[i]);
        }

        return threads;
    }

    private Map<Long, TcpConnectionInfo> getConnectionsById() {
        Map<Long, TcpConnectionInfo> connectionInfoMap = new HashMap<Long, TcpConnectionInfo>();

        ServletService servletService = ServletService.getCurrent();
        if (servletService != null) {
            ServerMXBean serverAdmin = servletService.getAdmin();

            if (serverAdmin != null) {
                PortMXBean[] ports = serverAdmin.getPorts();

                if (ports != null) {
                    for (PortMXBean port : ports) {
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
        }

        return connectionInfoMap;
    }

    private ThreadActivityGroup[] partitionThreads(ThreadSnapshot[] threads, boolean greedy) {
        ThreadActivityGroup[] groups = createGroups();

        for (ThreadSnapshot thread : threads) {
            for (ThreadActivityGroup group : groups) {
                boolean added = group.addIfMatches(thread);
                if (added && greedy) break;
            }
        }

        return groups;
    }
}
