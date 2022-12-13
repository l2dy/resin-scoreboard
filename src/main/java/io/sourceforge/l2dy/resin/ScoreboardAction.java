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

import com.caucho.admin.thread.ThreadActivityGroup;

import java.lang.management.ThreadMXBean;
import java.util.Map;

public class ScoreboardAction {
    public static String execute(ThreadMXBean threadMXBean, boolean greedy, int lineWidth) {
        ThreadActivityReport report = new ThreadActivityReport();
        ThreadActivityGroup[] groups = report.execute(threadMXBean, greedy);

        StringBuilder sb = new StringBuilder();

        for (ThreadActivityGroup group : groups) {
            String scoreboard = group.toScoreboard();

            sb.append("[");
            sb.append(group.getName());
            sb.append("]");
            sb.append("\n");

            sb.append(breakIntoLines(scoreboard, lineWidth));
            sb.append("\n");
            sb.append("\n");
        }

        sb.append("[Scoreboard Key]");
        sb.append("\n");

        Map<Character, String> key = report.getScoreboardKey();
        for (Map.Entry<Character, String> entry : key.entrySet()) {
            sb.append(entry.getKey());
            sb.append("   ");
            sb.append(entry.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String breakIntoLines(String s, int w) {
        if (s.length() <= w) return s;

        StringBuilder sb = new StringBuilder(s);
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            int pos = (i * w) + (i - 1);
            if (pos >= sb.length()) break;
            sb.insert(pos, "\n");
        }

        return sb.toString();
    }
}
