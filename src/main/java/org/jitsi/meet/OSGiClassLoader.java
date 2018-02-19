package org.jitsi.meet;

import org.xeustechnologies.jcl.JarClassLoader;

public class OSGiClassLoader extends ClassLoader {

    private final JarClassLoader proxyClassLoader;

    private final ClassLoader defaultClassLoader;

    public OSGiClassLoader(JarClassLoader proxyClassLoader, ClassLoader defaultClassLoader) {
        this.proxyClassLoader = proxyClassLoader;
        this.defaultClassLoader = defaultClassLoader;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        Class res = proxyClassLoader.loadClass(className);
        if (res == null) {
            return defaultClassLoader.loadClass(className);
        }
        return res;
    }
    
}
