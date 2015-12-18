package org.jitsi.influxdb;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by justin.martinez on 12/15/15.
 */
@RunWith(JUnit4.class)
public class LineProtocolTest
{
    @Test
    public void singlePointTest()
    {
        String name = "singlept";
        String[] cols = {"col1", "col2"};
        Object[] vals = {"1", "2"};
        InfluxDBEvent e = new InfluxDBEvent(name,cols, vals);
        e.setUseLocalTime(false);
        //TODO this does not need to happen per test
        AbstractLoggingHandler abl = mock(AbstractLoggingHandler.class,
                CALLS_REAL_METHODS);

        String exp = "singlept col1=\"1\",col2=\"2\"\n";
        String line = abl.formatEntry(e);

        System.out.println(line);

        assertEquals(exp, line);
    }

    @Test
    public void multipointTest()
    {
        String name = "multipt";
        String[] cols = {"col1", "col2"};
        Object[] row1 = {'1', '2'};
        Object[] row2 = {'3', '4'};
        Object[] vals = {row1, row2};
        InfluxDBEvent e = new InfluxDBEvent(name, cols, vals);
        e.setUseLocalTime(false);
        AbstractLoggingHandler abl = mock(AbstractLoggingHandler.class,
                CALLS_REAL_METHODS);

        String exp = "multipt col1=\"1\",col2=\"2\"\n";
        exp += "multipt col1=\"3\",col2=\"4\"\n";
        String line = abl.formatEntry(e);

        System.out.println(line);

        assertEquals(exp, line);
    }

    @Test
    public void keyEscapingTest()
    {
        String name = "space , comma";
        String[] cols = {"col 1", "col,2"};
        Object[] vals = {" 1 ", ",2,"};
        InfluxDBEvent e = new InfluxDBEvent(name, cols, vals);
        e.setUseLocalTime(false);
        AbstractLoggingHandler abl = mock(AbstractLoggingHandler.class,
                CALLS_REAL_METHODS);

        String exp = "space\\ \\,\\ comma col\\ 1=\" 1 \",col\\,2=\",2,\"\n";
        String line = abl.formatEntry(e);

        System.out.println(line);

        assertEquals(exp, line);
    }

    @Test
    public void fieldValueEscapingTest()
    {
        String name = "fieldEscapingTest";
        String[] cols = {"field1"};
        Object[] vals = {" '\"\"one\"\"' "};
        InfluxDBEvent e = new InfluxDBEvent(name, cols, vals);
        e.setUseLocalTime(false);
        AbstractLoggingHandler abl = mock(AbstractLoggingHandler.class,
                CALLS_REAL_METHODS);

        String exp = "fieldEscapingTest field1=\" '\\\"\\\"one\\\"\\\"' \"\n";
        String line = abl.formatEntry(e);

        System.out.println(line);

        assertEquals(exp, line);
    }


}
