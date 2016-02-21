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

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class loader does all the loading of classes requested via the Mathematica function LoadJavaClass[].
 * It also loads native libraries and resources used by classes that it loads.
 * The directories and files it searches for classes and other resources are provided
 * to it via the addLocations() method, which is called from Mathematica during every call to LoadJavaClass[]
 * and also by AddToClassPath[].
 * The JLink Mathematica code manages the class path locations, including providing a default set
 * of extra dirs. Directories are automatically searched for jar or zip
 * files they contain, so you do not have to name jar files explicitly in addLocations(), although you
 * can if you want to limit the search to a specific jar file in a directory.
 * <p>
 * Most uses of this class are internal to J/Link, and most programmers will never deal with it directly.
 * One reason to use this class is if you want to load a class in Java code that is found in the special
 * set of extra locations that this class loader knows about (such as Java subdirectories of Mathematica
 * application directories). Each KernelLink has a JLinkClassLoader instance associated with it,
 * which you can retrieve using the link's {@link KernelLink#getClassLoader() getClassLoader()} method.
 * Here is an example of loading a class from a special J/Link-specific directory.
 * <pre>
 * Class cls = Class.forName("Some.class.that.only.JLink.can.find", ml.getClassLoader());</pre>
 * You can add directories and jar files to the search path using the addLocations() method:
 * <pre>
 * // This is equivalent to calling AddToClassPath["/some/dir", "/another/dir"] in Mathematica.
 * ml.getClassLoader().addLocations(new String[]{"/some/dir", "/another/dir"}, true);</pre>
 * If you using this class from a standalone Java program you should first call
 * the KernelLink method {@link KernelLink#enableObjectReferences() enableObjectReferences()} to
 * initialize the class loader with the special Mathematica-specific locations for Java classes.
 * <p>
 * Another advanced reason to use this class is if you need the class loader
 * to have a specific parent loader, because that parent loader has certain capabilities or
 * can load classes from certain locations that J/Link would otherwise not know about.
 * <pre>
 * KernelLink ml = MathLinkFactory.createKernelLink(...);
 * ml.discardAnswer();
 * ml.setClassLoader(new JLinkClassLoader(mySpecialParentClassLoader));
 * ml.enableObjectReferences();</pre>
 * The class has some static methods that are intended to be used from Mathematica code. Here is an
 * example that loads a resource from a Mathematica application's Java subdirectory:
 * <pre>
 * (* Mathematica code *)
 * LoadJavaClass["com.wolfram.jlink.JLinkClassLoader"];
 * JLinkClassLoader`getInstance[]@getResource["myImage.gif"];</pre>
 * When using the class from Java, you will always have a specific link in hand, so instead of calling
 * static methods you should obtain a JLinkClassLoader instance from the link using its getClassLoader()
 * method and then call instance methods.
 *
 * @since 3.0
 * @see KernelLink#getClassLoader()
 * @see KernelLink#setClassLoader(JLinkClassLoader)
 */

public class JLinkClassLoader extends ClassLoader {

    /* This ClassLoader doesn't actually load any classes itself. It defers all loading to a helper loader.
     * This system gives us the ability to add locations to the start as well as the end of the class path.
     * The URLClassLoader that we want to inherit much useful behavior from does not support adding to the
     * start of the class path--only the end. When we want to add locations to the start of the path we need
     * to create a new loader with a newly-ordered set of locations. The new loader holds the previous ones
     * in a chain and checks them for previously-loaded classes as necessary. More details are in the
     * JLinkClassLoaderHelper class.
     *
     * In this class we override all public non-static ClassLoader methods and forward them to the helper instance.
     */

    protected volatile JLinkClassLoaderHelper helper;

    // Only public method we can't override here is getParent(), as it is final.
    // But we don't need to because both this instance and helper have the same parent.
    public synchronized void clearAssertionStatus() { helper.clearAssertionStatus(); }
    public synchronized void setClassAssertionStatus(String className, boolean enabled) { helper.setClassAssertionStatus(className, enabled); }
    public synchronized void setDefaultAssertionStatus(boolean enabled) { helper.setDefaultAssertionStatus(enabled); }
    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled) { helper.setPackageAssertionStatus(packageName, enabled); }
    public synchronized URL getResource(String name) { return helper.getResource(name); }
    public Enumeration getResources(String name) throws IOException { return helper.getResources(name); }
    public synchronized InputStream getResourceAsStream(String name) { return helper.getResourceAsStream(name); }
    public synchronized Class loadClass(String name) throws ClassNotFoundException { return helper.loadClass(name); }
    // Cannot override findLoadedClass() because it is final, so give it a new name. This method is part of the fix for
    // bug 190015. Every time a helper loader does findClass(), it is essential that it go all the way back to here,
    // so that the complete chain of helpers is searched for an already-loaded class. It is not enough for helpers to
    // only search "later" helpers for classes--they must also search earlier ones in the chain as well.
    // Note that callers should always be synchronized, so this method is not -- and cannot be; cf bug 271621
    Class findLoadedClassExposed(String name) { return helper.findLoadedClassExposed(name); }

    ///////////////////////////////////  Static Interface  //////////////////////////////////

    //  Static interface intended to be used by Mathematica code.

	/**
     * Retrieves the JLinkClassLoader instance being used by the currently-active link.
     * This method is intended to be called only from Mathematica, where the "active link"
	 * is well-defined (it is the link on which the call is coming in on).
     *
     * @since 3.0
	 */
	public static JLinkClassLoader getInstance() {
	    KernelLink link = StdLink.getLink();
		return link != null ? link.getClassLoader() : null;
	}

    /**
     * Loads the class with the given name using the JLinkClassLoader instance of the currently-active link.
     * This method is intended to be called only from Mathematica, where the "active link"
     * is well-defined (it is the link on which the call is coming in on).
     *
     * @since 3.0
     */
    public static Class classFromName(String name) throws ClassNotFoundException {
        // This used to be getInstance().loadClass(name), but that broke for classes like "[I" in Java 6.
        // Sun says "that wasn't how loadClass() was supposed to work; use Class.forName() instead."
        // in http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6387908. Not really sure whether
        // 2nd arg should be true or false. False seems closer to the original helperementation.
        return Class.forName(name, false, getInstance());
    }


	///////////////////////////////////  Instance Methods  //////////////////////////////////


    /**
     * Constructs a new JLinkClassLoader instance. Only advanced programmers will need to use this.
     *
     * @since 3.0
     */
    public JLinkClassLoader() {
        // In the absence of any specifically set parent to use, make this loader's parent be the loader that
        // loaded this class. This increases the likelihood that J/Link will be able to find
        // application-specific classes by ensuring that it can find any classes visible to the loader that
        // loaded J/Link itself. [Although this could backfire if the loader that loaded J/Link had less
        // capabilities than the "Application" class loader, which is the default parent for loaders created
        // without a specific parent.]
        // This feature was motivated by the needs of Tomcat and webM. Tomcat 4 uses a special
        // classloader to load webapp classes. If JLink.jar is in webM's web-inf/lib dir, it will be loaded
        // by the special classloader for web apps, and now J/Link will have access to all the classes that
        // servlets and other Java code in the web app have access to. If we didn't set the parent below,
        // then LoadClass calls from an MSP would fail if they asked for classes visible only to web apps. In
        // fact (if JLink.jar is in webM's web-inf/lib dir), LoadClass["com.wolfram.jlink.MathLink"] would fail.
        // For normal "installable Java" uses, this parent-setting makes no difference, as the classloader
        // that loads J/Link itself is the "application" classloader, which would be the default parent anyway.
        this(JLinkClassLoader.class.getClassLoader());
    }

    /**
     * Constructs a new JLinkClassLoader instance that has the given class loader as its parent loader.
     * Only advanced programmers will need to use this.
     *
     * @since 3.0
     */
    public JLinkClassLoader(ClassLoader parent) {
		super(parent);
		helper = new JLinkClassLoaderHelper(new URL[0], null, parent, this);
	}


    /**
     * Adds URLs, directories, and jar files in which this classloader will look for classes.
     * The elements of the locations array must be full paths to directories or jar or zip files,
     * or http URLs.
     * The searchForJars parameter determines whether directories will be automatically searched
     * for any jar files they contain.
     *
     * @since 3.0
     */
    public synchronized void addLocations(String[] locations, boolean searchForJars) {
        addLocations(locations, searchForJars, false);
    }

    public synchronized void addLocations(String[] locations, boolean searchForJars, boolean prepend) {

		if (locations == null)
			return;

		List urlsToAdd = new ArrayList(locations.length);

		for (int i = 0; i < locations.length; i++) {
			String thisLocation = locations[i];
			if (thisLocation.toLowerCase().startsWith("http:")) {
				// Is an http URL.
				try {
				    urlsToAdd.add(new URL(thisLocation));
				} catch (MalformedURLException e) {
					continue;
				}
			} else {
				// Is a file or dir spec.
				if (!thisLocation.toLowerCase().endsWith(".jar") && !thisLocation.toLowerCase().endsWith(".zip")) {
					// Is a directory. Make sure every directory name ends with a dir separator.
					String dirName = thisLocation.endsWith(File.separator) ? thisLocation : thisLocation + File.separator;
					try {
					    urlsToAdd.add(fileToURL(new File(dirName)));
						if (searchForJars) {
							// Scan through dirs looking for jars and zips to add.
							String[] filesInDir = (new File(dirName)).list();
							if (filesInDir != null) {
								for (int j = 0; j < filesInDir.length; j++) {
									String lowerCaseFile = filesInDir[j].toLowerCase();
									if (lowerCaseFile.endsWith(".jar") || lowerCaseFile.endsWith(".zip")) {
										File f = new File(dirName, filesInDir[j]);
										urlsToAdd.add(fileToURL(f));
									}
								}
							}
						}
					} catch (Exception e) {
						continue;
					}
				} else {
					// Is a jar or zip file.
					try {
					    urlsToAdd.add(fileToURL(new File(thisLocation)));
					} catch (Exception e) {
						continue;
					}
				}
			}
		}
		addAll(urlsToAdd, prepend);
	}


    /**
     * Gives the set of locations in which this class loader will search for classes.
     * Only the locations known by this loader, not its parent loader, are returned.
     *
     * @since 3.0
     */
	public synchronized String[] getClassPath() {

		URL[] existingLocs = getURLs();
		String[] result = new String[existingLocs.length];
		for (int i = 0; i < existingLocs.length; i++) {
			if (existingLocs[i].getProtocol().equals("file")) {
				String fileString = existingLocs[i].getFile();
				try {
					URI u = new URI(existingLocs[i].toString());
					fileString = u.getPath();
				} catch (Exception e) {}
				result[i] = fileString;
			} else {
				result[i] = existingLocs[i].toString();
			}
		}
		return result;
	}


    /**
     * Converts an array of bytes into an instance of class Class.
     * This method is effectively a public export of the final ClassLoader method defineClass().
     * It exists mainly for future internal uses within J/Link applications.
     *
     * @since 3.0
     */
    public synchronized Class classFromBytes(String className, byte[] bytes) {
        return helper.classFromBytes(className, bytes);
    }



	private void addAll(List urlsToAdd, boolean prepend) {

        URL[] existingLocs = getURLs();

	    if (prepend) {
	        if (urlsToAdd.size() > 0) {
	            // On a prepend we need to create a new helper instance with the newly-ordered set of locations.
	            // New helper must always have the full set of URLs. Just put the new ones at the front (after deleting them
	            // from the old set). That is, urlsToAdd ~Join~ (existingURLs ~Complement~ urlsToAdd).
	            List newURLSet = new ArrayList(existingLocs.length + urlsToAdd.size());
	            newURLSet.addAll(urlsToAdd);
                for (int i = 0; i < existingLocs.length; i++) {
                    URL u = existingLocs[i];
                    if (!urlsToAdd.contains(u))
                        newURLSet.add(u);
                }
                helper = new JLinkClassLoaderHelper((URL[]) newURLSet.toArray(new URL[newURLSet.size()]), helper, getParent(), this);
	        }
	    } else {
	        // Remove any "new" URLs that are already in the classpath.
	        urlsToAdd.removeAll(Arrays.asList(existingLocs));
	        Iterator iter = urlsToAdd.iterator();
    		while (iter.hasNext())
                helper.addURL((URL) iter.next());
	    }
	}


    private URL[] getURLs() {
        return helper.getURLs();
    }


	private static URL fileToURL(File f) throws MalformedURLException {
		// Using toURI() ensures proper handling of spaces in path names. The toURL() method
		// is broken in that regard and will not be fixed by Sun for backwards compatibility reasons.
		return f.toURI().toURL();
	}

}

