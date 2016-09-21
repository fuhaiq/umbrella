/*
 * Created on Jul 25, 2008
 *
 */
package com.wolfram.jlink;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;


// This class exists for precisely one reason--to allow Java system classes that call
// Class.forName() to be able to find classes that are known only to the JLinkClassLoader (this includes
// the CLASSPATH and all the special J/Link locations).
//
// In general, system classes (like JDBC, SSL, etc.) that want to load classes
// by name use a method like Class.forName("classname", ..., clsLoader) that allows them to specify
// the loader to use. They generally get the loader from either Thread.getContextClassLoader() or
// ClassLoader.getSystemClassLoader(). Using Thread.getContextClassLoader() is easier for J/Link to deal
// with because there is a setContextClassLoader() method (which J/Link currently uses). The "system" class loader
// (also called the "app" class loader) is harder to deal with, and that is what this class is for.
//
// This class is used in conjunction with a system property set by J/Link on the command line that launches Java:
//    -Djava.system.class.loader=com.wolfram.jlink.JLinkSystemClassLoader
// This causes an instance of this class to be created (by calling the public ctor herein) and used as
// the replacement for the standard system classloader, which is the one that finds things from
// CLASSPATH, etc. and is usually called the "application classloader". This class never loads anything
// by itself--it's just a shell that delegates to one of two other loaders. If there is an
// appropriate JLinkClassLoader available at the moment, that instance is used; if there is no such loader available
// then the original system classloader is used (which is this loader's delegation parent). In calls to Java
// that originate in Mathematica (which is what this class is intended for), there will be a JLinkClassLoader
// available and thus calls to Class.forName("classname", ..., getSystemClassLoader()) will be able to see the
// full set of classes known to J/Link (the same set that LoadJavaClass[] can find).
//
// Note that this is mainly relevant only for calls to Class.forName() that come from system classes (ones in the
// JRE itself). That is because all other classes are already being found by the JLinkClassLoader, so if these classes
// call Class.forName() without specifying a loader (which is typical) they get the classloader that loaded them,
// which is the JLinkClassLoader, and things are fine.

public class JLinkSystemClassLoader extends ClassLoader {

    private final ClassLoader origSystemLoader;

    private ClassLoader getDelegateLoader() {
        ClassLoader jlinkLoader = JLinkClassLoader.getInstance();
        return jlinkLoader != null ? jlinkLoader : origSystemLoader;
    }


    public JLinkSystemClassLoader(ClassLoader origSystemLoader) {
        // The orig loader becomes this loader's delegation parent.
        super(origSystemLoader);
        this.origSystemLoader = origSystemLoader;
    }


    ////////////////  From here down we implement all public methods by delegation  ////////////////

    public synchronized void clearAssertionStatus() {
        getDelegateLoader().clearAssertionStatus();
    }

    public URL getResource(String name) {
        return getDelegateLoader().getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        return getDelegateLoader().getResourceAsStream(name);
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return getDelegateLoader().loadClass(name);
    }

    public synchronized void setClassAssertionStatus(String className, boolean enabled) {
        getDelegateLoader().setClassAssertionStatus(className, enabled);
    }

    public synchronized void setDefaultAssertionStatus(boolean enabled) {
        getDelegateLoader().setDefaultAssertionStatus(enabled);
    }

    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled) {
        getDelegateLoader().setPackageAssertionStatus(packageName, enabled);
    }

}
