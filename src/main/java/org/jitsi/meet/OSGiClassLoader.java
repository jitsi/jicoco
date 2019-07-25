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
package org.jitsi.meet;

import org.xeustechnologies.jcl.*;

/**
 * Class store specific (jar class loader) and default class loader.
 * Used for loadable plugins at jar files.
 */
public class OSGiClassLoader extends ClassLoader {

    /**
     * Plugin's (jar's) class loader. Will be checked first to load class.
     */
    private final JarClassLoader proxyClassLoader;

    /**
     * Default (for example platform or system) class loader. Will be checked
     * second to load class.
     */
    private final ClassLoader defaultClassLoader;

    /**
     * Create OSGiClassLoader with
     * @param proxyClassLoader - jar class loader
     * @param defaultClassLoader - default (could be platform or system) class
     *                           loader
     */
    public OSGiClassLoader(JarClassLoader proxyClassLoader, ClassLoader defaultClassLoader) {
        this.proxyClassLoader = proxyClassLoader;
        this.defaultClassLoader = defaultClassLoader;
    }

    /**
     * Load class from jar class loader first and default class loader second.
     * @param className string with class name
     * @return Class object or null if class was not found
     * @throws ClassNotFoundException if jar class loader or default class
     * loader will throw exception.
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        Class res = proxyClassLoader.loadClass(className);
        if (res == null) {
            return defaultClassLoader.loadClass(className);
        }
        return res;
    }
    
}
