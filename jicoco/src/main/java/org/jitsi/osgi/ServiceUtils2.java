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

import java.lang.*;
import java.util.*;

/**
 * Gathers utility functions related to OSGi services such as getting a service
 * registered in a BundleContext.
 *
 * @author Lyubomir Marinov
 */
public class ServiceUtils2
{
    /**
     * Gets an OSGi service registered in a specific <tt>BundleContext</tt> by
     * its <tt>Class</tt>
     *
     * @param <T> the very type of the OSGi service to get
     * @param bundleContext the <tt>BundleContext</tt> in which the service to
     * get has been registered
     * @param serviceClass the <tt>Class</tt> with which the service to get has
     * been registered in the <tt>bundleContext</tt>
     * @return the OSGi service registered in <tt>bundleContext</tt> with the
     * specified <tt>serviceClass</tt> if such a service exists there;
     * otherwise, <tt>null</tt>
     */
    public static <T> T getService(
            BundleContext bundleContext,
            java.lang.Class<T> serviceClass)
    {
        ServiceReference<T> serviceReference
                = bundleContext == null
                ? null : bundleContext.getServiceReference(serviceClass);

        return
                (serviceReference == null)
                        ? null
                        : bundleContext.getService(serviceReference);
    }

    public static <T> Collection<T> getServices(
            BundleContext bundleContext,
            Class<T> serviceClass)
    {
        List<T> services = new LinkedList<T>();

        if (bundleContext != null)
        {
            Collection<ServiceReference<T>> serviceReferences = null;

            try
            {
                serviceReferences
                    = bundleContext.getServiceReferences(serviceClass, null);
            }
            catch (IllegalStateException e)
            {
            }
            catch (InvalidSyntaxException e)
            {
            }
            if (serviceReferences != null)
            {
                for (ServiceReference<T> serviceReference : serviceReferences)
                {
                    T service = null;

                    try
                    {
                        service = bundleContext.getService(serviceReference);
                    }
                    catch (IllegalArgumentException e)
                    {
                    }
                    catch (IllegalStateException e)
                    {
                        // The bundleContext is no longer valid.
                        break;
                    }
                    catch (SecurityException e)
                    {
                    }
                    if ((service != null) && !services.contains(service))
                        services.add(service);
                }
            }
        }
        return services;
    }

    /** Prevents the creation of <tt>ServiceUtils2</tt> instances. */
    private ServiceUtils2()
    {
    }
}
