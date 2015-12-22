package org.jitsi.influxdb;

import org.jitsi.service.configuration.ConfigurationService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.influxdb.dto.Point;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Random;

import static org.mockito.Mockito.*;


/**
 * Created by justin.martinez on 12/15/15.
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
        int num = rnd.nextInt(100);
        Object[] row = { "one", "two" };
        Object[] vals = new Object[num];
        for (int i = 0; i < num; i++)
            vals[i] = row;

        InfluxDBEvent event = new InfluxDBEvent(name, cols, vals);

        System.out.println("Number of points: " + num);
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
