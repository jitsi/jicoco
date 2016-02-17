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
package org.jitsi.influxdb;

import org.influxdb.dto.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.util.*;

import static org.mockito.Mockito.*;


/**
 * Created by justin.martinez on 12/15/15.
 * @author Justin Martinez
 */
@RunWith(JUnit4.class)
public class LineProtocolTest
{
    private AbstractLoggingHandler alh;

    @Before
    public void init()
    {
        alh = mock(AbstractLoggingHandler.class,
                CALLS_REAL_METHODS);
        doNothing().when(alh).writePoint((Point) anyObject());
    }

    @Test
    public void singlePointTest()
    {
        String name = "singlept";
        String[] cols = {"col1", "col2"};
        Object[] vals = {"1", "2"};
        InfluxDBEvent event = new InfluxDBEvent(name, cols, vals);

        alh.logEvent(event);
        verify(alh, times(1)).writePoint((Point) anyObject());
    }

    @Test
    public void multipointTest()
    {
        String name = "multipt";
        String[] cols = {"col1", "col2"};

        Random rnd = new Random();
        int num = rnd.nextInt(100 - 2) + 2;
        Object[] row = { "one", "two" };
        Object[] vals = new Object[num];
        for (int i = 0; i < num; i++)
            vals[i] = row;

        InfluxDBEvent event = new InfluxDBEvent(name, cols, vals);

        alh.logEvent(event);
        verify(alh, times(num)).writePoint((Point) anyObject());
    }

    @Test
    public void badMultipointTest()
    {
        String name = "multipt";
        String[] cols = {"col1", "col2"};
        Object[] row1 = { "one", "two" };
        Object badRow = "three";
        Object[] row2 = { "four", "five" };
        Object[] vals = { row1, badRow, row2 };

        InfluxDBEvent event = new InfluxDBEvent(name, cols, vals);
        alh.logEvent(event);

        verify(alh, times(2)).writePoint((Point) anyObject());
    }

}
