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

import net.java.sip.communicator.util.*;

import org.influxdb.*;
import org.influxdb.dto.*;
import org.jitsi.eventadmin.*;
import org.jitsi.service.configuration.*;

import java.util.concurrent.*;

/**
 * Base class for InfluxDb logger implementation.
 *
 * @author Boris Grozev
 * @author George Politis
 * @author Pawel Domas
 * @author Justin Martinez
 */
public abstract class AbstractLoggingHandler
    implements EventHandler
{
    /**
     * The logger.
     */
    private final static Logger logger
        = Logger.getLogger(AbstractLoggingHandler.class);

    /**
     * The name of the property which specifies whether logging to an
     * <tt>InfluxDB</tt> is enabled.
     */
    public static final java.lang.String ENABLED_PNAME
        = "org.jitsi.videobridge.log.INFLUX_DB_ENABLED";

    /**
     * The name of the property which specifies the protocol, hostname and
     * port number (in URL format) to use to connect to <tt>InfluxDB</tt>.
     */
    public static final String URL_BASE_PNAME
        = "org.jitsi.videobridge.log.INFLUX_URL_BASE";

    /**
     * The name of the property which specifies the name of the
     * <tt>InfluxDB</tt> database.
     */
    public static final String DATABASE_PNAME
        = "org.jitsi.videobridge.log.INFLUX_DATABASE";

    /**
     * The name of the property which specifies the username to use to connect
     * to <tt>InfluxDB</tt>.
     */
    public static final String USER_PNAME
        = "org.jitsi.videobridge.log.INFLUX_USER";

    /**
     * The name of the property which specifies the password to use to connect
     * to <tt>InfluxDB</tt>.
     */
    public static final String PASS_PNAME
        = "org.jitsi.videobridge.log.INFLUX_PASS";

    /**
     * The influxdb object used to build the line protocol for writing.
     */
    private final InfluxDB influxDB;

    /**
     * The influxdb database name that data will be written to.
     */
    private final String database;

    /**
     * Initializes a new <tt>LoggingHandler</tt> instance, by reading
     * its configuration from <tt>cfg</tt>.
     *
     * @param cfg the <tt>ConfigurationService</tt> to use.
     * @throws Exception if initialization fails
     */
    public AbstractLoggingHandler(ConfigurationService cfg)
        throws Exception
    {
        if (cfg == null)
            throw new NullPointerException("cfg");

        String s = "Required property not set: ";
        String urlBase = cfg.getString(URL_BASE_PNAME, null);
        if (urlBase == null)
            throw new Exception(s + URL_BASE_PNAME);

        String user = cfg.getString(USER_PNAME, null);
        if (user == null)
            throw new Exception(s + USER_PNAME);

        String pass = cfg.getString(PASS_PNAME, null);
        if (pass == null)
            throw new Exception(s + PASS_PNAME);

        database = cfg.getString(DATABASE_PNAME, null);
        if (database == null)
            throw new Exception(s + DATABASE_PNAME);

        influxDB = InfluxDBFactory.connect(urlBase, user, pass);
        influxDB.createDatabase(database);
        // Flush every 2000 Points, at least every 100ms
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);

        logger.info("Initialized InfluxDBLoggingService for " + urlBase
            + ", database \"" + database + "\"");
    }

    /**
     * Logs an <tt>InfluxDBEvent</tt> to an <tt>InfluxDB</tt> database.
     * @param e the <tt>Event</tt> to log.
     */
    @SuppressWarnings("unchecked")
    protected void logEvent(InfluxDBEvent e)
    {
        boolean useLocalTime = e.useLocalTime();
        long now = System.currentTimeMillis();
        String measurement = e.getName();
        String[] columns = e.getColumns();
        Object[] values = e.getValues();

        int pointCount = values[0] instanceof Object[] ? values.length : 1;
        int fieldCount = columns.length;

        for (int i = 0; i < pointCount; i++) {
            if (pointCount > 1 && !(values[i] instanceof Object[]))
                continue;

            Point.Builder ptBuilder = Point.measurement(measurement);

            if (useLocalTime)
                ptBuilder.time(now, TimeUnit.MILLISECONDS);

            Object[] fieldValues;
            if (pointCount > 1)
                fieldValues = (Object[]) values[i];
            else
                fieldValues = values;

            for (int j = 0; j < fieldCount; j++)
                ptBuilder.field(columns[j], fieldValues[j]);

            Point pt = ptBuilder.build();
            writePoint(pt);
        }
    }

    /**
     * Writes a data point to influxdb
     * @param pt
     */
    public void writePoint(Point pt)
    {
        influxDB.write(database, "default", pt);
    }
}

