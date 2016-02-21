//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2015, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;


/* This is the worker class that does all the actual classloading. The JLinkClassLoader class is just a facade
 * that defers all loading to an instance of this class. This system gives us the ability to add locations to 
 * the start as well as the end of the class path. The URLClassLoader that we want to inherit much useful 
 * behavior from does not support adding to the start of the class path--only the end. When we want to add 
 * locations to the start of the path we need to create a new loader with a newly-ordered set of locations. 
 * To avoid changing the identity of the JLinkClassLoader instance (shared, and possibly cached, by other code),
 * we make that instance an unchanging facade that swaps out its implementation as needed. The implementation
 * is this class, and every time we create a new instance of this class we keep a reference to the old one.
 * When looking up a class in findClass(), we always go down the chain of old instances and ask them if
 * they have already loaded the requested class. In this way we never reload classes, but we force all new
 * loading to happen in the current JLinkClassLoaderHelper instance.
 */

public class JLinkClassLoaderHelper extends URLClassLoader {

    private final JLinkClassLoader top;
    private final JLinkClassLoaderHelper prevLoader;
            
    JLinkClassLoaderHelper(URL[] urls, JLinkClassLoaderHelper prevLoader, ClassLoader parent, JLinkClassLoader top) {
        super(urls, parent);
        this.prevLoader = prevLoader;
        this.top = top;
    }
    
    /**
     * Returns the object which protects this instance's class loading.
     * This <em>should</em> be {@link #top}.
     * After all, {@code top} is used as the universal lock so that no matter which
     * of the linked {@link JLinkClassLoaderHelper}'s is chosen as the entry point, 
     * there will be no lock ordering issue.  Unfortunately, this convention is broken by the superclass.
     * Thus, if the superclass initiates the class loading, then it may obtain the lock on this object
     * before obtaining the lock on {@code top}, while normal class loading first obtained the lock
     * on {@code top}, resulting in deadlock.
     * Barring another way around this, and considering that the explicit locking
     * in this class is new
     * (to fix bug 190015), this method is used to enforce lock ordering, at the possible
     * cost of correctness, by refusing
     * to lock on {@code top} if the calling thread already has a lock on {@code this}.
     * In such an unfortunate case, {@code this} is returned; otherwise {@code top} is returned.
     * (Of course, if the calling thread already owns both locks, it matter which is
     * returned.)  This should hopefully fix bug 271621 while continuing to minimize the impact of
     * bug 190015.
     * 
     * @return the object on which to lock.
     * @author jmichelson
     */
    private Object lockObject() {
        return Thread.holdsLock(this) ? this : top;
    }
    
    
    // Cannot override findLoadedClass() because it is final, so give it a new name.
    Class findLoadedClassExposed(String name) {
        
        synchronized (lockObject()) {
            // Give the previous loaders a chance to produce a previously-loaded class.
            Class c = null;
            if (prevLoader != null)
                c = prevLoader.findLoadedClassExposed(name);
            if (c == null)
                c = findLoadedClass(name);
            return c;
        }
    }
    
    
    public void addURL(URL url) {
        super.addURL(url);
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        
        synchronized (lockObject()) {
            // Allow the whole chain of helper loaders to look the class up in their previously-loaded
            // sets _before_ calling super.findClass()
            Class c = top.findLoadedClassExposed(name);            
            if (c == null)
                // If not previously loaded, then look for it in the current set of URL locations.
                c = super.findClass(name);
            return c;
        }
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        
        synchronized (lockObject()) {
            // First, check if the class has already been loaded
            Class<?> c = top.findLoadedClassExposed(name);
            if (c != null) {
                if (resolve)
                    resolveClass(c);
            } else {
                c = super.loadClass(name, resolve);
            }
            return c;
        }
    }


    protected String findLibrary(String libName) {
        
        String platformSpecificName = System.mapLibraryName(libName);
        URL[] locs = getURLs();
        for (int i = 0; i < locs.length; i++) {
            if (locs[i].getProtocol().equals("file")) {
                String fileName = locs[i].getFile();
                boolean isJarOrZip = fileName.toLowerCase().endsWith(".zip") || fileName.toLowerCase().endsWith(".jar");
                if (!isJarOrZip) {
                    // Treat fileName as a dir name. Look for the library in this dir and also dir/Libraries/$SystemID/.
                    File f = new File(fileName, platformSpecificName);
                    if (f.exists())
                        return f.getAbsolutePath();
                    f = new File(fileName, "Libraries" + java.io.File.separator + 
                                        Utils.getSystemID() + java.io.File.separator + platformSpecificName);
                    if (f.exists())
                        return f.getAbsolutePath();
                }
            }
        }
        return null;
    }


    // In effect, a public export of the final ClassLoader method defineClass().
    public Class classFromBytes(String className, byte[] bytes) {
        
        Class c = defineClass(className, bytes, 0, bytes.length);
        resolveClass(c);
        return c;
    }

}
