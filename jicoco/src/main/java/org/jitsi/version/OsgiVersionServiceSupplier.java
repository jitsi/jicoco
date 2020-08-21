/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.version;

import org.jitsi.osgi.*;
import org.jitsi.utils.version.*;
import org.osgi.framework.*;

public class OsgiVersionServiceSupplier implements VersionServiceSupplier
{
    private final BundleContext bundleContext;
    public OsgiVersionServiceSupplier(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    @Override
    public VersionService get()
    {
        return ServiceUtils2.getService(bundleContext, VersionService.class);
    }
}
