//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2002, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import com.wolfram.jlink.ui.ConsoleStream;


/**
 * Install is the class that has the main entry point when Java is launched from Mathematica
 * via InstallJava[]. It sets up definitions in Mathematica for calls into Java and starts up the
 * Reader thread, which waits for calls arriving from Mathematica. It has only one method of interest
 * to users, getStdLink(), which has been deprecated. 
 */

public class Install {

	static final int CALLJAVA              = 1;
	static final int LOADCLASS             = 2;
	static final int THROW                 = 3;
	static final int RELEASEOBJECT         = 4;
	static final int VAL                   = 5;
	static final int ONLOADCLASS           = 6;
	static final int ONUNLOADCLASS         = 7;
	static final int SETCOMPLEX            = 8;
	static final int REFLECT               = 9;
	static final int SHOW                  = 10;
	static final int SAMEQ                 = 11;
	static final int INSTANCEOF            = 12;
	static final int ALLOWRAGGED           = 13;
    static final int GETEXCEPTION          = 14;
	static final int CONNECTTOFE           = 15;
	static final int DISCONNECTTOFE        = 16;
	static final int PEEKCLASSES           = 17;
	static final int PEEKOBJECTS           = 18;
	static final int CLASSPATH             = 19;
	static final int ADDTOCLASSPATH        = 20;
	static final int SETUSERDIR            = 21;
	static final int ALLOWUICOMPUTATIONS   = 22;
	static final int UITHREADWAITING       = 23;
	static final int YIELDTIME             = 24;
	static final int GETCONSOLE            = 25;
    static final int EXTRALINKS            = 26;
    static final int GETWINDOWID           = 27;
    static final int ADDTITLECHANGELISTENER= 28;
    static final int SETVMNAME             = 29;
    static final int SETEXCEPTION          = 30;


    /**
	 * This is the entry point called by the Mathematica function InstallJava.
	 * Users will not call this unless they are trying to manually establish a link between Java
	 * and Mathematica, instead of using the default launch behavior of InstallJava. For example,
	 * here is how you could establish a link using listen/connect modes. You might do this if you
	 * wanted to launch the Java runtime inside a debugger such as the one provided in your
	 * Java development environment:
	 * <pre>
	 * On the command line, or in your Java development environment:
	 *     java com.wolfram.jlink.Install -linkname 1234 -linkmode listen
	 *
	 * Then in Mathematica:
	 *     InstallJava[LinkConnect["1234"]]</pre>
	 *
	 * @param args
	 */

	public static void main(String[] args) {

		KernelLink ml;
        int timeout = 25000; // Max millis to wait for kernel to connect to us.

        // Only print the banner if not linked. Look for -mathlink argument.
        boolean isLinked = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-mathlink")) {
                isLinked = true;
                break;
            }
        }
        if (!isLinked) {
            System.out.println("J/Link (tm)");
    		System.out.println("Copyright (C) 1999-2014, Wolfram Research, Inc. All Rights Reserved.");
    		System.out.println("www.wolfram.com");
    		System.out.println("Version " + KernelLink.VERSION);
    		System.out.println("");
    		System.out.flush();
        }

		if (MathLinkImpl.DEBUGLEVEL > 1) {
			for (int i = 0; i < args.length; i++)
				System.err.println(args[i]);
		}

        // This is a simple hack to help the ConsoleWindow class be as useful as possible. See
        // comments for the ConsoleStream class for more info.
        ConsoleStream.setSystemStdoutStream(System.out);
        ConsoleStream.setSystemStderrStream(System.err);
 
        // We want to set up the SecurityManager immdiately, so to avoid problems with file permissions
        // when reading the init file, we read it here and store its content in a char array. After we set
        // the SecurityManager we parse the contents of this array, so security is in effect while classpath
        // modifications are being made.
        BufferedReader initFileReader = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-init") && i < args.length - 1) {
                String initFile = args[i + 1];
                // Will be wrapped in ""; drop them.
                if (initFile.startsWith("\""))
                    initFile = initFile.substring(1, initFile.length() - 2);
                try {
                    java.io.Reader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(initFile), "UTF-8"));
                    char[] chars = new char[30000];
                    int numChars = rdr.read(chars, 0, 30000);
                    if (numChars > 0)
                        initFileReader = new BufferedReader(new CharArrayReader(chars, 0, numChars));
                    rdr.close();
                    new File(initFile).delete();
                } catch (Exception e) {}
            }
        }

        // Set a SecurityManager.
        String securityManagerClass = System.getProperty("com.wolfram.jlink.security");
        try {
            if (securityManagerClass == null) {
                // This security manager only prevents calls to System.exit().
                System.setSecurityManager(new JLinkSecurityManager());
            } else {
                // Look for security manager class in the same classloader as loaded this calss. This means that
                // addon locations from JLinkClassLoader will not be available (those aren't even set up yet).
                // This is done for security reasons.
                Class securityClass = Install.class.getClassLoader().loadClass(securityManagerClass);
                System.setSecurityManager((SecurityManager) securityClass.newInstance());
            }
        } catch (Exception e) {
            System.err.println("FATAL ERROR: attempt to set a SecurityManager failed: " + e);
            return;
        }

		try {
			ml = MathLinkFactory.createKernelLink(args);
		} catch (MathLinkException e) {
			System.err.println("FATAL ERROR: link creation failed.");
			return;
		}

        // Read the init file, if one is supplied. The init file is specified by adding
        // -init "filename" on the command line. The idea is to allow Mathematica code to
        // let Java get started on some things before the link is connected. It's an "extra-MathLink"
        // means of communicating with Java. It is purely a performance optimization (we are
        // talking about saving a few tenths of a second.) The idea is to split the InstallJava
        // process into two parts--the first part is very quick and simply starts Java running.
        // It does not wait until Java is up and connected back to Mathematica. We can trigger this
        // process at kernel startup time because it is fast. Then by the time the kernel gets around
        // to calling Java, Java will have launched and possibly performed some app-specific
        // initializations. The init file is where such app-specific initializations can be specified.
        // Because initializations probably require the classpath to be set up, we also put the
        // initial classpath specs into the init file.
        // Why not put the contents of the init file directly into the command line that launches Java?
        // One reason is that MathLink allows only short command lines in LinkOpen. Another is that
        // we avoid problems with quoting arguments. It's just easier to write them into a file, one
        // set per line, than to try to deal with an extremely long, complex command line.
        // Lines in the init file must fit a precise form, starting with s single keyword, followed
        // by a single space char and space-separated arguments:
        //     cp some/dir/or/jar/file
        //     cpf some/dir           (cpf means don't search for jars in the dir)
        //     NO LONGER SUPPORTED: run ClassNameHavingAMainMethod arg1ToMain arg2ToMain ...
        // In a "run" line, the args must not have spaces in them. If you need to have spaces
        // (such as in file paths), convert them to %20.
        if (initFileReader != null) {
            try {
                String line;
                while ((line = initFileReader.readLine()) != null) {
                    try {
                        if (line.startsWith("cp ")) {
                            String addToClassPath = line.substring(3);
                            ml.getClassLoader().addLocations(new String[]{addToClassPath}, true);
                        } else if (line.startsWith("cpf ")) {
                            String addToClassPath = line.substring(4);
                            ml.getClassLoader().addLocations(new String[]{addToClassPath}, false);
                        } else if (line.startsWith("run ")) {
                            // I am no longer supporting the "run" lines. That feature was devised for the era when Java
                            // was launched at startup, and it was importnt to hide the startup time. In fact, the whole
                            // init file feature was designed for that purpose, but it's a little more work to rip it all
                            // out, so leaving the cp and cpf lines for now. Not supporting run lines also means that
                            // the undocumented RegisterJavaInitialization M function is no longer supported.
                            /*
                            line = line.substring(4);
                            // Split line by spaces. When it was written, spaces within args were converted
                            // to %20.
                            String[] cmd = line.split(" ");
                            if (cmd.length > 0) {
                                String clsName = cmd[0];
                                String[] argv = new String[cmd.length - 1];
                                for (int c = 0; c < argv.length; c++)
                                    argv[c] = cmd[c + 1].replaceAll("%20", " ");
                                Class c = ml.getClassLoader().loadClass(clsName);
                                Method mainMeth = c.getMethod("main", new Class[]{String[].class});
                                mainMeth.invoke(null, new Object[]{argv});
                            }
                            */
                        }
                    } catch (Throwable t) {
                        // Do nothing if any single line of the init file triggered an exception.
                        // Just go on to the next one.
                    }
                }
            } catch (Exception e) {
                // Do nothing.
            } finally {
                try {
                    initFileReader.close();                            
                } catch (Exception ee) {}
            }
        }
      
        // Read timeout parameter or determine if it's a listen link. For a listen link,
        // we don't ever want to time out. User might want Java to wait
        // arbitrarily long before the kernel connects to it. Example is launching Java in
        // a debugger. The -timeout parameter is not currently used on the Mathematica
        // side of J/Link, but we support it here in case it needs to be set from Mathematica.
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-timeout") && i < args.length - 1) {
                try {
                    timeout = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    // Do nothing; leave at default.
                }
            } else if (args[i].equalsIgnoreCase("-linklisten") ||
                    (args[i].equalsIgnoreCase("-linkmode") && i < args.length - 1 && 
                           args[i+1].equalsIgnoreCase("listen"))) {
                timeout = Integer.MAX_VALUE;
                break;
            }
        }
        
        // Must process the init file (above) before connecting the link so that operations
        // can be performed on threads while we wait to connect the link here.
        if (!install(ml, timeout)) {
            ml.close();
            // Force quit with exit() instead of just returning here because
            // the initFile processing may have started up other threads.
            JLinkSecurityManager.setAllowExit(true);
            System.exit(1);
        }
        
        if (Utils.isWindows()) {
            // Here we hide the java DOS window that is created if java.exe is the runtime used instead of javaw.exe.
            if (ml instanceof WrappedKernelLink) {
                MathLink impl = ((WrappedKernelLink) ml).getMathLink();
                if (impl instanceof NativeLink)
                    NativeLink.hideJavaWindow();
            }
        }
        Reader.startReader(ml, true, false);
	}


	/**
	 * @deprecated As of J/Link 1.1, use {@link StdLink#getLink() StdLink.getLink()} instead.
	 */
	public static KernelLink getStdLink() {
		return StdLink.getLink();
	}


    // Although the install() method has been gutted and now does essentially nothing, leave it as a
    // separate method because it has been public for years. Also keep the old one-arg signature.
    
    public static boolean install(MathLink ml) {
        return install(ml, Integer.MAX_VALUE);
    }

    public static boolean install(MathLink ml, int timeout) {

		try {
            // If this times out, Java will quit.
			ml.connect(timeout);
			return true;
		} catch (MathLinkException e) {
			if (MathLinkImpl.DEBUGLEVEL > 1) System.err.println("Fatal error: MathLinkException during Install.");
			return false;
		}
	}

}
