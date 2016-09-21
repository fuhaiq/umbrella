//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2000, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.lang.reflect.*;
import java.util.Properties;

/**
 * MathLinkFactory is the class that is used to construct objects of the various
 * link interfaces (MathLink, KernelLink, and LoopbackLink). Because these are interfaces, not
 * classes, and the actual classes that implement them are deliberately unknown to the client,
 * &quot;factory&quot; methods are needed to create the actual objects used.
 * <p>
 * Most programmers will use createKernelLink() instead of createMathLink().
 * <p>
 * These methods correspond to calling one of the MLOpen functions in the C-language MathLink API.
 */

public class MathLinkFactory {


	/////////////////////////////  KernelLink  ////////////////////////////////

	/**
	 * Creates a KernelLink. The argument is a string that follows the same
	 * specification as in the C-language function MLOpenString, as documented in the
	 * Mathematica book.
	 * <p>
	 * Here are some examples:
	 * <pre>
	 * // Typical launch on Windows
	 * KernelLink ml =
	 *     MathLinkFactory.createKernelLink(&quot;-linkmode launch -linkname 'c:\\program files\\wolfram research\\mathematica\\5.1\\mathkernel.exe'&quot;);
     * <p>
	 * // Typical launch on Unix
	 * KernelLink ml =
	 *     MathLinkFactory.createKernelLink(&quot;-linkmode launch -linkname 'math -mathlink'&quot;);
     * <p>
	 * // Typical launch on Mac OS X
	 * KernelLink ml =
	 *     MathLinkFactory.createKernelLink(&quot;-linkmode launch -linkname '\"/Applications/Mathematica 5.1.app/Contents/MacOS/MathKernel\" -mathlink'&quot;);
	 * <p>
	 * // Typical "listen" link on any platform:
	 * KernelLink ml =
	 *     MathLinkFactory.createKernelLink(&quot;-linkmode listen -linkname 1234 -linkprotocol tcp&quot;);
	 * // Windows can use the default protocol for listen/connect links:
	 * KernelLink ml =
	 *     MathLinkFactory.createKernelLink(&quot;-linkmode listen -linkname foo&quot;);</pre>
	 *
	 * @param cmdLine a string parsed as a command line
	 * @return the KernelLink
	 * @exception com.wolfram.jlink.MathLinkException if the link fails to open
	 */

	public static KernelLink createKernelLink(String cmdLine) throws MathLinkException {
		return createKernelLink0(cmdLine, null);
	}

	/**
	 * Creates a KernelLink. The argument is an array of strings that follows the same
	 * specification as in the C-language function MLOpenArgv, as documented in the
	 * Mathematica book.
	 * <p>
	 * Here are some example argv arrays:
	 * <pre>
	 * // Typical launch on Windows:
	 * String[] argv = {&quot;-linkmode&quot;, &quot;launch&quot;, &quot;-linkname&quot;,
	 * &quot;c:\\program files\\wolfram research\\mathematica\\5.1\\mathkernel.exe&quot;};
	 * <p>
	 * // Typical launch on UNIX:
	 * String[] argv = {&quot;-linkmode&quot;, &quot;launch&quot;, &quot;-linkname&quot;, &quot;math -mathlink&quot;};
	 * <p>
	 * // Typical launch on Mac OS X:
	 * String[] argv = {&quot;-linkmode&quot;, &quot;launch&quot;, &quot;-linkname&quot;,
	 * &quot;\"/Applications/Mathematica 5.1.app/Contents/MacOS/MathKernel\" -mathlink&quot;};
	 * <p>
	 * // Typical "listen" link on any platform:
	 * String[] argv = {&quot;-linkmode&quot;, &quot;listen&quot;, &quot;-linkname&quot;, &quot;1234&quot;, &quot;-linkprotocol&quot;, &quot;tcp&quot;};
	 * <p>
	 * // Windows can use the default protocol for listen/connect links:
	 * String[] argv = {&quot;-linkmode&quot;, &quot;listen&quot;, &quot;-linkname&quot;, &quot;foo&quot;};</pre>
	 *
	 * @param argv an array of string arguments
	 * @return the KernelLink
	 * @exception com.wolfram.jlink.MathLinkException if the link fails to open
	 */

	public static KernelLink createKernelLink(String[] argv) throws MathLinkException {
		return createKernelLink0(null, argv);
	}

	/**
	 * Creates a KernelLink by wrapping a MathLink. This method is primarily of use to
	 * developers who want to create their own implementations of the MathLink interface,
	 * for example one based on CORBA rather than the native protocols used by the MathLink
	 * library. All you have to do is implement MathLink; KernelLink is free because there is
	 * an internal KernelLink implementation class that can do everything by manipulating
	 * a MathLink instance. This is the method that creates such a KernelLink.
	 * <p>
	 * You give up ownership of the MathLink you pass in, meaning that it can only be used,
	 * including being closed, by the KernelLink.
	 *
	 * @param ml the MathLink to wrap
	 * @return the KernelLink
	 * @exception com.wolfram.jlink.MathLinkException
	 */

	public static KernelLink createKernelLink(MathLink ml) throws MathLinkException {
		return new WrappedKernelLink(ml);
	}


	private static KernelLink createKernelLink0(String cmdLine, String[] argv) throws MathLinkException {

		if (cmdLine == null && argv == null)
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, "Null argument to KernelLink constructor");
		// One or other of cmdLine and argv must be null.
		boolean usingCmdLine = cmdLine != null;
		String protocol = usingCmdLine ? determineProtocol(cmdLine) : determineProtocol(argv);
		if (!protocol.equals("native")) {
			String implClassName = null;
			try {
				implClassName = System.getProperty("KernelLink." + protocol);
			} catch (SecurityException e) {}
			if (implClassName == null) {
				Properties props = loadProperties();
				implClassName = props.getProperty("KernelLink." + protocol);
			}
			if (implClassName != null) {
				Class implementingClass = null;
				try {
                    // If there is a StdLink (e.g., this is happening in a call that originated from Mathematica), use
                    // the JLinkClasLoader associated with that link so that we can find classes from J/Link's extended
                    // clsaspath. If not, use the loader that loaded this class.
                    KernelLink stdLink = StdLink.getLink();
                    ClassLoader cl = stdLink != null ? JLinkClassLoader.getInstance() : MathLinkFactory.class.getClassLoader();
                    implementingClass = cl.loadClass(implClassName);
				} catch (ClassNotFoundException e) {}
				// We need do nothing if no classes were found--will fall through to using "wrapped" implementation.
				if (implementingClass != null) {
					try {
						Class argsClass = usingCmdLine ? String.class : String[].class;
						Constructor ctor = implementingClass.getConstructor(new Class[]{argsClass});
						return (KernelLink) ctor.newInstance(usingCmdLine ? new Object[]{cmdLine} : new Object[]{argv});
					} catch (Exception e) {
						if (MathLinkImpl.DEBUGLEVEL > 0) {
							System.err.println("Exception creating link object of class " + implementingClass.getName() + ": " + e.toString());
							if (e instanceof InvocationTargetException)
								System.err.println("Was InvocationTargetExeption: " + ((InvocationTargetException)e).getTargetException().toString());
						}
						if (e instanceof InvocationTargetException)
							throw new MathLinkException(((InvocationTargetException) e).getTargetException(),
										"Error instantiating link object of class " + implementingClass.getName());
						else
							throw new MathLinkException(e,
										"Error instantiating link object of class " + implementingClass.getName());
					}
				}
			}
		}
		// Fall through to here means we look for a MathLink implementation to wrap.
		return new WrappedKernelLink(usingCmdLine ? createMathLink(cmdLine) : createMathLink(argv));
	}

	/////////////////////////////  MathLink  ////////////////////////////////

	/**
	 * Creates a MathLink. The argument is a string that follows the same specification
	 * as in the C-language function MLOpenString, as documented in the Mathematica book.
	 * <p>
	 * Most programmers will use createKernelLink() instead, because they want to work with
	 * the higher-level KernelLink interface, not MathLink.
	 * <p>
	 * Here is an example:
	 * <pre>
	 * MathLinkFactory.createMathLink(&quot;-linkmode listen -linkname 1234 -linkprotocol tcp&quot;);</pre>
	 *
	 * @param cmdLine a string parsed as a command line
	 * @return the MathLink
	 * @exception com.wolfram.jlink.MathLinkException if the link does not open correctly
	 * @see #createKernelLink(String)
	 */

	public static MathLink createMathLink(String cmdLine) throws MathLinkException {
		return createMathLink0(cmdLine, null);
	}

	/**
	 * Creates a MathLink. The argument is an array of strings that follows the same specification
	 * as in the C-language function MLOpenArgv, as documented in the Mathematica book.
	 * <p>
	 * Most programmers will use createKernelLink() instead, because they want to work with
	 * the higher-level KernelLink interface, not MathLink.
	 * <p>
	 * Here is an example:
	 * <pre>
	 * String[] argv = {&quot;-linkmode&quot;, &quot;listen&quot;, &quot;-linkname&quot;, &quot;1234&quot;, &quot;-linkprotocol&quot;, &quot;tcp&quot;};
	 * MathLinkFactory.createMathLink(argv);</pre>
	 *
	 * @param argv an array of string arguments
	 * @return the MathLink
	 * @exception com.wolfram.jlink.MathLinkException if the link does not open correctly
	 * @see #createKernelLink(String[])
	 */

	public static MathLink createMathLink(String[] argv) throws MathLinkException {
		return createMathLink0(null, argv);
	}

	private static MathLink createMathLink0(String cmdLine, String[] argv) throws MathLinkException {

		if (cmdLine == null && argv == null)
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, "Null argument to MathLink constructor");
		// One or other of cmdLine and argv must be null.
		boolean usingCmdLine = cmdLine != null;
		String protocol = usingCmdLine ? determineProtocol(cmdLine) : determineProtocol(argv);
		if (!protocol.equals("native")) {
			String implClassName = null;
			try {
				implClassName = System.getProperty("MathLink." + protocol);
			} catch (SecurityException e) {}
            if (implClassName == null) {
                Properties props = loadProperties();
                implClassName = props.getProperty("MathLink." + protocol);
            }
            // If we still haven't found a class name for this protocol, fall back to hard-coded link implementations.
            // These class choices would be overriden if a properties file named other ones, so there is no loss in
            // flexibility by hard-coding them here. We just avoid the need for a JLink.properties file to be
            // added to the distribution.
            if (implClassName == null) {
                if (protocol.equals("rmi"))
                    implClassName = "com.wolfram.jlink.ext.MathLink_RMI";
                else if (protocol.equals("remoteservices"))
                    implClassName = "com.wolfram.remoteservices.jlink.RemoteServicesLink";
            }
			if (implClassName != null) {
				Class implementingClass = null;
				try {
					// If there is a StdLink (e.g., this is happening in a call that originated from Mathematica), use
				    // the JLinkClasLoader associated with that link so that we can find classes from J/Link's extended
				    // clsaspath. If not, use the loader that loaded this class.
				    KernelLink stdLink = StdLink.getLink();
				    ClassLoader cl = stdLink != null ? JLinkClassLoader.getInstance() : MathLinkFactory.class.getClassLoader();
					implementingClass = cl.loadClass(implClassName);
				} catch (ClassNotFoundException e) {}
				if (implementingClass != null) {
					try {
						Class argsClass = usingCmdLine ? String.class : String[].class;
						Constructor ctor = implementingClass.getConstructor(new Class[]{argsClass});
						return (MathLink) ctor.newInstance(usingCmdLine ? new Object[]{cmdLine} : new Object[]{argv});
					} catch (Exception e) {
						if (MathLinkImpl.DEBUGLEVEL > 0) {
							System.err.println("Exception creating link object of class " + implementingClass.getName() + ": " + e.toString());
							if (e instanceof InvocationTargetException)
								System.err.println("Was InvocationTargetExeption: " + ((InvocationTargetException)e).getTargetException().toString());
						}
						if (e instanceof InvocationTargetException)
							throw new MathLinkException(((InvocationTargetException) e).getTargetException(),
										"Error instantiating link object of class " + implementingClass.getName());
						else
							throw new MathLinkException(e,
										"Error instantiating link object of class " + implementingClass.getName());
					}
				} else {
					// If no class was found, issue a warning and fall through to using NativeLink.
					System.err.println("J/Link Warning: could not find any Java class that implements the requested " +
												protocol + " protocol. This protocol name will be passed to the MathLink library to " +
												"see if it has a native implementation.");
				}
			}
		}
		return usingCmdLine ? new NativeLink(cmdLine) : new NativeLink(argv);
	}


	/////////////////////////////  LoopbackLink  ////////////////////////////////

	/**
	 * Creates a LoopbackLink, a special type of link that is written to and read
	 * by the same program.
	 *
	 * @return the loopback link
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see LoopbackLink
	 */

	public static LoopbackLink createLoopbackLink() throws MathLinkException {
		return new NativeLoopbackLink();
	}

	/////////////////////////////  Command-line parsing  ///////////////////////////////

	// The determineProtocol functions return "NATIVE" for protocols that are implemented
	// by the NativeLink class (TCP, filemap, PPC, pipes, etc.) For special link types
	// (e.g., HTTP), they return the exact name specified following the -linkprotocol
	// specifier, in upper case.

	private static String determineProtocol(String cmdLine) {

		java.util.StringTokenizer st = new java.util.StringTokenizer(cmdLine);
		String prot = "native";
		while (st != null && st.hasMoreTokens()) {
			if (st.nextToken().toLowerCase().equals("-linkprotocol") && st.hasMoreTokens()) {
				prot = st.nextToken().toLowerCase();
				break;
			}
		}
		return isNative(prot) ? "native" : prot;
	}

	private static String determineProtocol(String[] argv) {

		String prot = "native";
		if (argv != null) {
			for (int i = 0; i < argv.length - 1; i++) {
				if (argv[i].toLowerCase().equals("-linkprotocol")) {
					prot = argv[i+1].toLowerCase();
					break;
				}
			}
		}
		return isNative(prot) ? "native" : prot;
	}


	// Incoming string is in lower case. It is not a problem if we are overly conservative here,
	// failing to identify as native types that are. The only cost is that we waste time
	// looking for non-existent classes that might implement them. What we must not do is
	// return true for a type that requires a special class.
	private static boolean isNative(String prot) {
		return prot.equals("native") || prot.equals("local") || prot.equals("filemap") ||
				prot.equals("fm") || prot.equals("ppc") || prot.equals("tcp") || prot.equals("tcpip") ||
					prot.equals("pipes") || prot.equals("sharedmemory") || prot.equals("");
	}


	///////////////////////////////////  Properties File  //////////////////////////////////////

	private static Properties loadProperties() {

		Properties props = new Properties();
		try {
		    // First look alongside JLink.jar. If that fails, try to load from classpath.
		    java.io.InputStream in = null;
            String jarDir = Utils.getJLinkJarDir();
            if (jarDir != null) {
                try {
                    in = new java.io.FileInputStream(jarDir + "JLink.properties");
                } catch (Exception e) {}
            }
            if (in == null) {
                // If there is a StdLink (e.g., this is happening in a call that originated from Mathematica), use
                // the JLinkClasLoader associated with that link so that we can find the props file from J/Link's extended
                // clsaspath. If not, use the loader that loaded this class.
                KernelLink stdLink = StdLink.getLink();
                ClassLoader cl = stdLink != null ? JLinkClassLoader.getInstance() : MathLinkFactory.class.getClassLoader();
                in = cl.getResourceAsStream("JLink.properties");
            }
            if (in != null) {
				props.load(in);
				in.close();
            }
		} catch (Exception e) {
			// Ignore exceptions.
		}
		return props;
	}

}
