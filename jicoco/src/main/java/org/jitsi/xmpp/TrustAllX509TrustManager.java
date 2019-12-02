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
package org.jitsi.xmpp;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * @author bbaldino
 */
public class TrustAllX509TrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] c, String s)
    {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] c, String s)
    {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return new X509Certificate[0];
    }
}
