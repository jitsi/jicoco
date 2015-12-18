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

import net.java.sip.communicator.util.Logger;

import org.jitsi.eventadmin.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.json.simple.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base class for InfluxDb logger implementation.
 *
 * @author Boris Grozev
 * @author George Politis
 * @author Pawel Domas
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
     * The <tt>URL</tt> to be used to POST to <tt>InfluxDB</tt>. Besides the
     * protocol, host and port also encodes the database name, user name and
     * password.
     */
    protected final URL url;
    /**
     * The <tt>Executor</tt> which is to perform the task of sending data to
     * <tt>InfluxDB</tt>.
     */
    private final Executor executor
        = ExecutorUtils
            .newCachedThreadPool(true, AbstractLoggingHandler.class.getName());

    /**
     * Initializes a new <tt>LoggingHandler</tt> instance, by reading
     * its configuration from <tt>cfg</tt>.
     * @param cfg the <tt>ConfigurationService</tt> to use.
     *
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

        String database = cfg.getString(DATABASE_PNAME, null);
        if (database == null)
            throw new Exception(s + DATABASE_PNAME);

        String user = cfg.getString(USER_PNAME, null);
        if (user == null)
            throw new Exception(s + USER_PNAME);

        String pass = cfg.getString(PASS_PNAME, null);
        if (pass == null)
            throw new Exception(s + PASS_PNAME);

        String urlStr
            = urlBase +  "/write?db=" + database + "&u=" + user +"&p=" +pass;

        url = new URL(urlStr);

        logger.info("Initialized InfluxDBLoggingService for " + urlBase
            + ", database \"" + database + "\"");
    }

    /**
     * Logs an <tt>InfluxDBEvent</tt> to an <tt>InfluxDB</tt> database. This
     * method returns without blocking, the blocking operations are performed
     * by a thread from {@link #executor}.
     *
     * @param e the <tt>Event</tt> to log.
     */
    @SuppressWarnings("unchecked")
    protected void logEvent(InfluxDBEvent e)
    {
        final String writeString = formatEntry(e);
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                sendPost(writeString);
            }
        });
    }

    /**
     *
     * @param key
     * @return
     */
    private String escapeKey(String key)
    {
        String escKey = key.replaceAll(" ", "\\\\ ");
        escKey = escKey.replaceAll(",", "\\\\,");
        return escKey;
    }

    private String formatFieldValue(Object value)
    {
        String formattedVal = "";

        //TODO there are better ways to do this
        if (value instanceof Integer || value instanceof Double)
        {
            formattedVal = value + "i";
        }
        else if (value instanceof Float ||
                value instanceof Double ||
                value instanceof Boolean)
        {
            System.out.println("I'm a float or Boolean dewd = " + value);
            formattedVal = value.toString();
        }
        else {
            // We are a string
            formattedVal = value.toString();
            formattedVal = formattedVal.replaceAll("\"", "\\\\\"");
            formattedVal = "\"" + formattedVal + "\"";
        }

        return formattedVal;
    }

    /**
     *
     * @param e the <tt>Event</tt> to log.
     * @return
     */
    public String formatEntry(InfluxDBEvent e)
    {
        // measurement value=12
        // measurement value=12 1439587925
        // measurement,foo=bar value=12
        // measurement,foo=bar value=12 1439587925
        // measurement,foo=bar,bat=baz value=12,otherval=21 1439587925

        boolean useLocalTime = e.useLocalTime();
        long now = System.currentTimeMillis();
        String measurement = escapeKey(e.getName());
        String[] cols = e.getColumns();
        Object[] vals = e.getValues();
        boolean multipoint = false;
        int pointCount = 1;
        int fieldCount = cols.length;

        if (vals[0] instanceof Object[])
        {
            multipoint = true;
            pointCount = vals.length;
        }

        StringBuilder sb = new StringBuilder();
        // one line per point
        for (int i = 0; i < pointCount; i++)
        {
            //Add key section
            sb.append(measurement);
            sb.append(" ");

            //Add fields and values
            String[] fields = new String[fieldCount];
            Object[] fieldsVals;

            if (multipoint)
                fieldsVals = (Object[]) vals[i];
            else
                fieldsVals = vals;

            for (int j = 0; j < fieldCount; j++)
            {
                fields[j] = escapeKey(cols[j]) + "=" +
                        formatFieldValue(fieldsVals[j]);
            }
            sb.append(String.join(",", fields));

            //Add timestamp
            if (e.useLocalTime())
            {
                sb.append(" ");
                sb.append(now);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Sends the string <tt>s</tt> as the contents of an HTTP POST request to
     * {@link #url}.
     * @param s the content of the POST request.
     */
    private void sendPost(final String s)
    {
        try
        {
            HttpURLConnection connection
                = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            DataOutputStream outputStream
                = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(s);
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode != 204)
                throw new IOException("HTTP response code: "
                    + responseCode);
        }
        catch (IOException ioe)
        {
            logger.info("Failed to post to influxdb: " + ioe);
        }
    }
}
