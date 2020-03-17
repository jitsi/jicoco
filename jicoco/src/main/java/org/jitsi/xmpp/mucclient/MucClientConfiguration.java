/*
 * Copyright @ 2018 - present, 8x8 Inc
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
 *
 */
package org.jitsi.xmpp.mucclient;

import org.jitsi.service.configuration.*;
import org.jitsi.utils.logging.*;

import java.util.*;

/**
 * Represents the configuration of a {@link MucClient}.
 *
 * @author Boris Grozev
 */
public class MucClientConfiguration
{
    /**
     * The {@link Logger} used by the {@link MucClientConfiguration} class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MucClientConfiguration.class);

    /**
     * The name of the property (without a prefix) which specifies the
     * hostname to connect to.
     * This is a required property.
     */
    public static String HOSTNAME = "HOSTNAME";

    /**
     * The name of the property (without a prefix) which specifies the
     * xmpp server's port to connect to.
     * This is an optional property and defaults to 5222.
     */
    public static String PORT = "PORT";

    /**
     * The name of the property (without a prefix) which specifies the
     * XMPP domain to use.
     * This is not a required property (if it is missing the hostname is
     * used as a domain)
     */
    public static String DOMAIN = "DOMAIN";

    /**
     * The name of the property (without a prefix) which specifies the
     * username to use to authenticate.
     * This is a required property.
     */
    public static String USERNAME = "USERNAME";

    /**
     * The name of the property (without a prefix) which specifies the
     * password to use to authenticate.
     * This is a required property.
     */
    public static String PASSWORD = "PASSWORD";

    /**
     * The name of the property (without a prefix) which specifies a
     * comma-separated list of full JIDs of the MUCs to join, e.g.:
     * {@code JvbBrewery@conference.example.com,JigasiBrewery@conference.example.com}
     *
     * This is a required property.
     */
    public static String MUC_JIDS = "MUC_JIDS";

    /**
     * The name of the property (without a prefix) which specifies the
     * nickname (i.e. the XMPP resource part) to use when joining the MUC.
     *
     * This is a required property.
     */
    public static String MUC_NICKNAME = "MUC_NICKNAME";

    /**
     * The name of the property (without a prefix) which specifies
     * whether to disable TLS certificate verifications.
     *
     * This is not a required property, the default behavior is to perform
     * verification.
     */
    public static String DISABLE_CERTIFICATE_VERIFICATION
        = "DISABLE_CERTIFICATE_VERIFICATION";

    /**
     * The name of the property (without a prefix) which specifies
     * the mode (sync or async) to use for the Smack IQ request handler.
     *
     * This is not a required property.
     */
    public static String IQ_HANDLER_MODE = "IQ_HANDLER_MODE";

    /**
     * Loads a list of {@link MucClientConfiguration} objects based on
     * properties read from a {@link ConfigurationService} with a given
     * {@code prefix}.
     * See {@link #loadFromMap(Map, String, boolean)} for the format of the
     * properties.
     *
     * @param config the {@link ConfigurationService} to read properties from.
     * @param prefix the prefix for property names.
     * @param removeIncomplete whether to remove any incomplete (see
     * {@link MucClientConfiguration#isComplete()}) entries from the returned
     * collection, or to return all of them regardless.
     *
     * @return a list of {@link MucClientConfiguration}s described by properties
     * in {@code config} with a prefix of {@code prefix}.
     */
    public static Collection<MucClientConfiguration> loadFromConfigService(
        ConfigurationService config, String prefix, boolean removeIncomplete)
    {
        Map<String, String> properties = new HashMap<>();
        for (String pname : config.getPropertyNamesByPrefix(prefix, false))
        {
            properties.put(pname, config.getString(pname));
        }

        return loadFromMap(properties, prefix, removeIncomplete);
    }

    /**
     * Loads a list of {@link MucClientConfiguration} objects based on
     * properties in a {@link Map<String, String>} with a given {@code prefix}.
     *
     * The properties can be described with map entries like this for an
     * ID of "":
     * PREFIX.HOSTNAME=hostname1
     * PREFIX.DOMAIN=domain
     *
     * Or like this for an ID of "id1":
     * PREFIX.id1.HOSTNAME=hostname2
     * PREFIX.id1.USERNAME=user
     *
     * @param properties the {@link Map} which contains the properties.
     * @param prefix the common prefix (to be ignored) for the keys in the map.
     * @param removeIncomplete whether to remove any incomplete (see
     * {@link MucClientConfiguration#isComplete()}) entries from the returned
     * collection, or to return all of them regardless.
     *
     * @return a list of {@link MucClientConfiguration}s described by the
     * entries of {@code properties} with a prefix of {@code prefix}.
     */
    public static Collection<MucClientConfiguration> loadFromMap(
        Map<String, String> properties, String prefix, boolean removeIncomplete)
    {
        Map<String, MucClientConfiguration> configurations = new HashMap<>();

        for (String pname : properties.keySet())
        {
            String stripped = pname.substring(prefix.length());
            String id = "";
            String prop = stripped;
            if (stripped.contains("."))
            {
                id = stripped.substring(0, stripped.indexOf("."));
                prop = stripped.substring(id.length() + 1);
            }

            MucClientConfiguration c
                = configurations.computeIfAbsent(
                id,
                MucClientConfiguration::new);
            c.setProperty(prop, properties.get(pname));
        }

        if (removeIncomplete)
        {
            configurations.values().removeIf(
                c ->
                {
                    if (!c.isComplete())
                    {
                        logger.warn(
                            "Ignoring incomplete configuration with id=" + c
                                .getId());
                        return true;
                    }
                    return false;
                });
        }

        return configurations.values();
    }

    /**
     * Holds the properties of this {@link MucClientConfiguration}. To make
     * the property names case insensitive we always store the keys in upper
     * case.
     */
    private final HashMap<String, String> props = new HashMap<>();

    /**
     * The ID of this {@link MucClientConfiguration}.
     */
    private final String id;

    /**
     * Initializes a new {@link MucClientConfiguration} instance.
     * @param id the ID.
     */
    public MucClientConfiguration(String id)
    {
        this.id = id;
    }

    /**
     * @return the ID of this {@link MucClientManager}.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the hostname (i.e. the address to connect to).
     */
    public String getHostname()
    {
        return props.get(HOSTNAME.toUpperCase());
    }

    /**
     * Sets the hostname (i.e. the address to connect to).
     * @param hostname the hostname
     */
    public void setHostname(String hostname)
    {
        props.put(HOSTNAME, hostname);
    }

    /**
     * @return the XMPP server's port number.
     */
    public String getPort()
    {
        return props.get(PORT.toUpperCase());
    }

    /**
     * Sets the XMPP server's port.
     * @param port the XMPP server's port
     */
    public void setPort(String port)
    {
        props.put(PORT, port);
    }

    /**
     * @return the XMPP domain.
     */
    public String getDomain()
    {
        return props.get(DOMAIN.toUpperCase());
    }

    /**
     * Sets the XMPP domain.
     * @param domain the domain to set.
     */
    public void setDomain(String domain)
    {
        props.put(DOMAIN, domain);
    }

    /**
     * @return the username to use to authenticate to the XMPP server.
     */
    public String getUsername()
    {
        return props.get(USERNAME.toUpperCase());
    }

    /**
     * Sets the username to use to authenticate to the XMPP server.
     * @param username
     */
    public void setUsername(String username)
    {
        props.put(USERNAME, username);
    }

    /**
     * @return the password to use to authenticate to the XMPP server.
     */
    public String getPassword()
    {
        return props.get(PASSWORD.toUpperCase());
    }

    /**
     * Sets the password to use to authenticate to the XMPP server.
     * @param password
     */
    public void setPassword(String password)
    {
        props.put(PASSWORD, password);
    }

    /**
     * @return the JID of the MUC to join, e.g.
     * "JvbBrewery@conference.example.com,JigasiBrewery@conference.example.com"
     */
    public List<String> getMucJids()
    {
        String str = props.get(MUC_JIDS.toUpperCase());
        if (str != null)
        {
            return Arrays.asList(str.split(","));
        }

        return null;
    }

    /**
     * Sets the list of JIDs of the MUCs to join.
     * @param mucJids the list of full JIDs of the MUCs to join.
     */
    public void setMucJids(List<String> mucJids)
    {
        props.put(MUC_JIDS, String.join(",", mucJids));
    }

    /**
     * @return the nickname to use when joining the MUCs.
     */
    public String getMucNickname()
    {
        return props.get(MUC_NICKNAME.toUpperCase());
    }

    /**
     * Sets the nickname to use when joining the MUCs.
     * @param mucNickname the nickname.
     */
    public void setMucNickname(String mucNickname)
    {
        props.put(MUC_NICKNAME, mucNickname);
    }

    /**
     * @return whether TLS certificate verification should be disabled.
     */
    public boolean getDisableCertificateVerification()
    {
        return Boolean.parseBoolean(props.get(DISABLE_CERTIFICATE_VERIFICATION));
    }

    /**
     * Sets whether TLS certificate verification should be disabled.
     * @param disableCertificateVerification whether to disable TLS certificate
     * verification.
     */
    public void setDisableCertificateVerification(
        boolean disableCertificateVerification)
    {
        props.put(DISABLE_CERTIFICATE_VERIFICATION, Boolean.TRUE.toString());
    }

    /**
     * @return a string which represents the mode (sync or async) to use
     * for the smack IQ request handler.
     */
    public String getIqHandlerMode()
    {
        return props.get(IQ_HANDLER_MODE);
    }

    /**
     * Sets string which represents the mode (sync or async) to use
     * for the smack IQ request handler.
     */
    public void setIqHandlerMode(String iqHandlerMode)
    {
        props.put(IQ_HANDLER_MODE, iqHandlerMode);
    }

    /**
     * Checks whether this {@link MucClientConfiguration} has all of the required
     * properties.
     * @return {@code true} if all required properties are set, and
     * {@code false} otherwise.
     */
    public boolean isComplete()
    {
        return getHostname() != null && getUsername() != null
            && getPassword() != null
            && getMucJids() != null
            && getMucNickname() != null;
    }

    /**
     * Sets a property.
     * @param name the name of the property.
     * @param value the value to set.
     */
    public void setProperty(String name, String value)
    {
        props.put(name.toUpperCase(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return
            "[ " + MucClientConfiguration.class.getName() +
                " id=" + id+
                " domain=" + getDomain() +
                " hostname=" + getHostname() +
                " port=" + getPort() +
                " username=" + getUsername() +
                " mucs=" + getMucJids() +
                " mucNickname=" + getMucNickname() +
                " disableCertificateVerification="
                    + getDisableCertificateVerification() +
                "]";
    }
}
