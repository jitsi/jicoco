/*
 * Copyright @ 2015 - present, 8x8 Inc
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
package org.jitsi.osgi;

import org.osgi.framework.*;

/**
 * Class can be used for lazy OSGi service reference. The intention is to
 * replace common code:
 * <p><code>
 * if (configService == null)<br/>
 * {<br/>
 *     configService = ServiceUtil.getService(...);<br/>
 * }<br/>
 * return configService;<br/>
 * </code></p>
 *
 * @author Pawel Domas
 */
public class OSGIServiceRef<ServiceClass>
{
    /**
     * OSGi bundle context which will be used to obtain the service.
     */
    private final BundleContext ctx;

    /**
     * Service class.
     */
    private final Class<ServiceClass> clazz;

    /**
     * Service instance.
     */
    private ServiceClass instance;

    /**
     * Creates new instance of <tt>OSGIServiceRef</tt>.
     *
     * @param ctx the OSGi bundle context which will be used to obtain
     *            the service.
     * @param serviceClass the class of the service that will be access through
     *                     newly created <tt>OSGIServiceRef</tt> instance.
     */
    public OSGIServiceRef(BundleContext ctx, Class<ServiceClass> serviceClass)
    {
        this.ctx = ctx;
        this.clazz = serviceClass;
    }

    /**
     * Will return instance of <tt>ServiceClass</tt> by trying to access it in
     * the OSGi bundle context if we don't have a reference to it yet. Once we
     * cache the instance we no longer access the bundle context.
     *
     * @return <tt>ServiceClass</tt> service instance if we have managed to
     *         obtain one from the OSGi context ever or <tt>null</tt> if there
     *         was no valid service reference in the context found so far.
     */
    public ServiceClass get()
    {
        if (instance == null)
        {
            instance = ServiceUtils2.getService(ctx, clazz);
        }
        return instance;
    }
}
