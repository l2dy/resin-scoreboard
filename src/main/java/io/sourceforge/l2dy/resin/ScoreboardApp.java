/*
 * Copyright (c) 2022 l2dy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.sourceforge.l2dy.resin;

import com.caucho.management.server.ServerMXBean;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.sourceforge.l2dy.resin.beans.RemoteConnection;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.logging.Logger;

/**
 * Entrypoint for scoreboard command.
 */
public class ScoreboardApp {
    private static final Logger log = Logger.getLogger(ScoreboardApp.class.getName());
    private static final String MANAGEMENT_PREFIX = "com.sun.management.";
    private static final String CONNECTOR_ADDRESS = MANAGEMENT_PREFIX + "jmxremote.localConnectorAddress";
    private static final String RESIN_SERVER_MXBEAN_NAME = "resin:type=Server";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No pid specified!\n");
            for (VirtualMachineDescriptor vmd : VirtualMachine.list()) {
                System.out.println(vmd.toString());
            }
            return;
        }

        String id = args[0];
        boolean greedy = args.length < 3 || !"--greedy".equals(args[1]) || !"false".equals(args[2]);
        try {
            String message = execute(id, greedy, 80);
            System.out.println(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String execute(String id, boolean greedy, int lineWidth) throws IOException, AttachNotSupportedException, MalformedObjectNameException {
        VirtualMachine virtualMachine = VirtualMachine.attach(id);

        String connectorAddress = virtualMachine.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        if (connectorAddress == null) {
            System.err.println("Starting local agent");
            connectorAddress = virtualMachine.startLocalManagementAgent();
            if (connectorAddress == null) {
                System.err.println("Unable to start local agent");
                System.exit(1);
            }
        }

        JMXServiceURL jmxServiceURL = new JMXServiceURL(connectorAddress);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);

        MBeanServerConnection server = jmxConnector.getMBeanServerConnection();
        RemoteConnection.setServerConnection(server);

        ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        ServerMXBean serverMXBean = JMX.newMXBeanProxy(server, new ObjectName(RESIN_SERVER_MXBEAN_NAME), ServerMXBean.class);

        return ScoreboardAction.execute(threadMXBean, serverMXBean, greedy, lineWidth);
    }
}
