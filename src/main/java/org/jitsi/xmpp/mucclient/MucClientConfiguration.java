/*
 * Copyright @ 2018 Atlassian Pty Ltd
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

import org.jitsi.impl.neomedia.pulseaudio.*;
import org.jivesoftware.smack.iqrequest.*;

import java.util.*;

/**
 * Represents the configuration of a {@link MucClient}.
 *
 * @author Boris Grozev
 */
public class MucClientConfiguration
{
    /**
     * The name of the property (without a prefix) which specifies the
     * hostname to connect to.
     * This is a required property.
     */
    public static String HOSTNAME = "HOSTNAME";

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

    private static final String TRUE = "true";
    /**
     * Holds the properties of this {@link MucClientConfiguration}.
     */
    HashMap<String, String> props = new HashMap<>();

    /**
     * The ID of this {@link MucClientConfiguration}.
     */
    private String id;

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
        return props.get(HOSTNAME);
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
     * @return the XMPP domain.
     */
    public String getDomain()
    {
        return props.get(DOMAIN);
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
        return props.get(USERNAME);
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
        return props.get(PASSWORD);
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
        String str = props.get(MUC_JIDS);
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
        return props.get(MUC_NICKNAME);
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
        return TRUE.equals(props.get(DISABLE_CERTIFICATE_VERIFICATION));
    }

    /**
     * Sets whether TLS certificate verification should be disabled.
     * @param disableCertificateVerification whether to disable TLS certificate
     * verification.
     */
    public void setDisableCertificateVerification(
        boolean disableCertificateVerification)
    {
        props.put(DISABLE_CERTIFICATE_VERIFICATION, TRUE);
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
        props.put(name, value);
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
                " username=" + getUsername() +
                " mucs=" + getMucJids() +
                " mucNickname=" + getMucNickname() +
                " disableCertificateVerification="
                    + getDisableCertificateVerification() +
                "]";
    }
}
