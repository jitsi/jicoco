/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.cmd;


import org.jitsi.util.*;

import java.util.*;

/**
 *
 */
public class CmdLine
{
    private final static Logger logger = Logger.getLogger(CmdLine.class);

    private Map<String, String> argMap = new HashMap<String, String>();

    public void parse(String[] args)
    {
        for (String arg : args)
        {
            if (arg.startsWith("--"))
                arg = arg.substring(2);
            else if (arg.startsWith("-"))
                arg = arg.substring(1);

            int eqIdx = arg.indexOf("=");
            if (eqIdx <= 0)
            {
                logger.warn("Skipped invalid cmd line argument: " + arg);
                continue;
            }
            else if (eqIdx == arg.length() - 1)
            {
                logger.warn("Skipped empty cmd line argument: " + arg);
                continue;
            }

            String key = arg.substring(0, eqIdx);
            String val = arg.substring(eqIdx+1);
            argMap.put(key, val);
        }
    }

    public String getOptionValue(String opt)
    {
        return argMap.get(opt);
    }

    public String getOptionValue(String opt, String defaultValue)
    {
        String val = getOptionValue(opt);
        return val != null ? val : defaultValue;
    }

    public int getIntOptionValue(String opt, int defaultValue)
    {
        String val = getOptionValue(opt);
        if (val == null)
            return defaultValue;
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException fmt)
        {
            return defaultValue;
        }
    }
}
