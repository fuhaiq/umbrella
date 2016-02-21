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

import java.lang.reflect.*;


// NativeLink implements the MathLink interface via native methods in JLinkNativeLibrary. These
// native methods in turn call the MathLink C library. NativeLink is the only point where J/Link "touches"
// the native library. NativeLink inherits a lot from MathLinkImpl, which handles generic logic that does
// not require direct calls into the native library. The code that resides in NativeLink is just the lowest
// layer of the functionality of a MathLink implementation, where there is nothing left to do but call into
// the MathLink C library.

public class NativeLink extends MathLinkImpl {

	protected long link;  // The C-code MLINK pointer.

    // This lock protects all link open/close operations within the one shared MLEnvironment.
    // Such operations are not thread-safe. Choose an obscure object that is guaranteed to be unique
	// within the entire JVM.
    protected static final Object environmentLock = Void.class;

	static final String LINK_NULL_MESSAGE		= "Link is not open.";
	static final String CREATE_FAILED_MESSAGE	= "Link failed to open.";

	// This is intended to be used within J/Link to determine whether the native
	// library is loaded and therefore whether it is OK to do things that would require it to be loaded.
	// Right now, only the Expr class uses this, to decide whether it can use a loopback link. It does not want
	// to attempt to load the library if it has not been loaded already.
	static volatile boolean nativeLibraryLoaded = false;

    // See the comments for the JLinkShutdownThread class below for info on how this flag is used.
    static volatile boolean jvmIsShuttingDown = false;

    public static Throwable loadLibraryException = null;


	public static void loadNativeLibrary() {

	    synchronized (environmentLock) {
    	    if (nativeLibraryLoaded)
    			return;

    		String libName = "JLinkNativeLibrary";
    		try {
                // Look first for the JLINK_LIB_DIR environment variable.
                String libDir = null;
                try {
                    // getEnv() will throw an Error() in some Java versions (supported in 1.5 and greater).
                    libDir = System.getenv("JLINK_LIB_DIR");
                } catch (Throwable t) {}
                if (libDir != null)
                    nativeLibraryLoaded = loadNativeLib(libName, libDir);
                // If that fails, try the com.wolfram.jlink.libdir property.
                if (!nativeLibraryLoaded) {
                    libDir = System.getProperty("com.wolfram.jlink.libdir");
                    if (libDir != null)
                        nativeLibraryLoaded = loadNativeLib(libName, libDir);
                }
    			// If that fails, try looking in same dir as JLink.jar.
    			if (!nativeLibraryLoaded) {
    				String jarDir = Utils.getJLinkJarDir();
    				if (jarDir != null)
    					nativeLibraryLoaded = loadNativeLib(libName, jarDir);
    			}
    			if (!nativeLibraryLoaded) {
    				try {
    					System.loadLibrary(libName);
    					nativeLibraryLoaded = true;
    				} catch (UnsatisfiedLinkError e) {
    				    loadLibraryException = e;
    					System.err.println("Fatal error: cannot find the required native library named " + libName + ".");
    				}
    			}
    		} catch (SecurityException e) {
    		        loadLibraryException = e;
    				System.err.println("Fatal error: security exception trying to load " + libName +
    										". This thread does not have permission to load native libraries. Message is: " + e.getMessage());
    		}

    		if (nativeLibraryLoaded) {
    		    loadLibraryException = null;
    			MLInitialize();
                // See the comments for the JLinkShutdownThread class below.
                // The addShutdownHook() method is new in JDK 1.3, so this call
                // introduces a dependency on 1.3.
                Runtime.getRuntime().addShutdownHook(new JLinkShutdownThread());
            }
	    }
	}


	//////////////////////////////////  Constructors  ////////////////////////////////////////

	// Need better error reporting during link open. Should use the error arg available in MathLink API.

	public NativeLink(String cmdLine) throws MathLinkException {

		loadNativeLibrary();
		String[] errMsgOut = new String[1];
		String mode = Utils.determineLinkmode(cmdLine);
		if (mode != null && mode.equals("exec")) {
			doExecMode(cmdLine, null);
		} else {
            synchronized (environmentLock) {
                // MLForceYield forces yielding under Unix even in the presence of a yield function.
                link = MLOpenString(cmdLine + " -linkoptions MLForceYield", errMsgOut);
            }
		}
		if (link == 0) {
			String msg = errMsgOut[0] != null ? errMsgOut[0] : CREATE_FAILED_MESSAGE;
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, msg);
		}
	}

	public NativeLink(String[] argv) throws MathLinkException {

		loadNativeLibrary();
		String[] errMsgOut = new String[1];
		String mode = Utils.determineLinkmode(argv);
		String[] newArgv = new String[argv.length + 2];
		System.arraycopy(argv, 0, newArgv, 0, argv.length);
		// MLForceYield forces yielding under Unix even in the presence of a yield function.
		newArgv[newArgv.length - 2] = "-linkoptions";
		newArgv[newArgv.length - 1] = "MLForceYield";
		if (mode != null && mode.equals("exec")) {
			doExecMode(null, newArgv);
		} else {
            synchronized (environmentLock) {
                link = MLOpen(newArgv.length, newArgv, errMsgOut);
            }
		}
		if (link == 0) {
			String msg = errMsgOut[0] != null ? errMsgOut[0] : CREATE_FAILED_MESSAGE;
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, msg);
		}
	}


    public NativeLink() {
        this(0);
    }

    // Mainly for marshalling MLINKs in kernel code into KernelLinks.
    public NativeLink(long mlinkPtr) {

        loadNativeLibrary();
        link = mlinkPtr;
    }


	// Hack for "exec" linkmode. This is a special mode that does what "launch" does but
	// in a different way. This mode was put in to work around a mysterious problem with
	// Apache/JServ on Linux. A valid command line that uses exec mode must begin with
	// the executable name, have no protocol spec, and no linkname spec. Example:
	//     String cmd = "math -mathlink -linkmode exec";
	//     String cmd = "math -mathlink -linkmode exec -pwpath foo -initfile bar";
	//     String[] argv = {"math -mathlink", "-linkmode", "exec"};

	private void doExecMode(String cmdLine, String[] argv) throws MathLinkException {

		String[] errMsgOut = new String[1];
        synchronized (environmentLock) {
            link = MLOpenString("-linkmode listen -linkprotocol tcp -linkoptions MLDontInteract", errMsgOut);
        }
		if (link == 0)
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, "Link open failed for exec mode.");
		try {
			String name = MLName(link);
			Process p = null;
			// One of cmdLine and argv must be null
			if (cmdLine != null) {
				// Here we drop out the "-linkmode exec" part.
				int execPos = cmdLine.indexOf("-linkmode exec");
				String newCmdLine = cmdLine.substring(0, execPos) +
												cmdLine.substring(execPos + 14) +
													" -linkmode connect -linkprotocol tcp -linkname " + name;
				p = Runtime.getRuntime().exec(newCmdLine);
			} else {
				String[] newArgv = new String[argv.length + 4];
				System.arraycopy(argv, 0, newArgv, 0, argv.length);
				for (int i = 0; i < argv.length; i++)
					if (newArgv[i].equals("exec"))
						newArgv[i] = "connect";
				newArgv[newArgv.length - 4] = "-linkprotocol";
				newArgv[newArgv.length - 3] = "tcp";
				newArgv[newArgv.length - 2] = "-linkname";
				newArgv[newArgv.length - 1] = name;
				p = Runtime.getRuntime().exec(newArgv);
			}
			if (p == null)
				throw new MathLinkException(MathLink.MLE_CREATION_FAILED, "Process was null");
		} catch (Exception e) {
            synchronized (environmentLock) {
                MLClose(link);
            }
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, e.toString());
		}
	}


	///////////////////////////////////////  Accessors  //////////////////////////////////////////

	public synchronized long getLink() {
		return link;
	}


	//////////////////////////////////////  Public methods  ////////////////////////////////////////

    public static void setEnvID(String idStr) {

        MLSetEnvIDString(idStr);
    }


    public synchronized String getLinkedEnvID() throws MathLinkException {

        if (link == 0) {
            throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
        }
        String idStr = MLGetLinkedEnvIDString(link);
        int errCode = MLError(link);
        if (isException(errCode)) {
            throw new MathLinkException(errCode, MLErrorMessage(link));
        }
        return idStr;
    }


	public synchronized void close() {

		if (link != 0) {
            synchronized (environmentLock) {
                MLClose(link);
            }
			link = 0;
		}
	}


	public synchronized void connect() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLConnect(link);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


    public synchronized String name() throws MathLinkException {

        if (link == 0) {
            throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
        }
        String res = MLName(link);
        int errCode = MLError(link);
        if (isException(errCode)) {
            throw new MathLinkException(errCode, MLErrorMessage(link));
        }
        return res;
    }


	public synchronized void newPacket() {

		MLNewPacket(link);
	}

	public synchronized int nextPacket() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int pkt = MLNextPacket(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return pkt;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void endPacket() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLEndPacket(link);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized int error() {

		if (link == 0) {
			return MLE_LINK_IS_NULL;
		}
		return MLError(link);
	}

	public synchronized boolean clearError() {

		if (link == 0) {
			return false;
		}
		return MLClearError(link);
	}

	public synchronized String errorMessage() {

		if (link == 0) {
			return LINK_NULL_MESSAGE;
		} else if (error() >= MLE_NON_ML_ERROR) {
			return MathLinkException.lookupMessageText(error());
		} else {
			return MLErrorMessage(link);
		}
	}

	public synchronized void setError(int err) {

		MLSetError(link, err);
	}


	public synchronized boolean ready() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		return MLReady(link);
	}


	public synchronized void flush() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLFlush(link);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized int getNext() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int type = MLGetNext(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return type;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized int getType() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int type = MLGetType(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return type;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void putNext(int type) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutNext(link, type);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized int getArgCount() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int argc = MLGetArgCount(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return argc;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void putArgCount(int argCount) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutArgCount(link, argCount);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized void putSize(int size) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutSize(link, size);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized int bytesToPut() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int res = MLBytesToPut(link);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
		return res;
	}


	public synchronized int bytesToGet() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int res = MLBytesToGet(link);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
		return res;
	}


	public synchronized void putData(byte[] data, int len) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutData(link, data, len);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized byte[] getData(int len) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		byte[] res = MLGetData(link, len);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
		return res;
	}


	public synchronized String getString() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		String s = MLGetString(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return s;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized byte[] getByteString(int missing) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		byte[] data = MLGetByteString(link, (byte) missing);
		int errCode = MLError(link);
		if(errCode == 0) {
			return data;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void putByteString(byte[] data) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutByteString(link, data, data.length);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized String getSymbol() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		String s = MLGetSymbol(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return s;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void putSymbol(String s) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutSymbol(link, s);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


	public synchronized void put(boolean b) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutSymbol(link, b ? "True" : "False");
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized int getInteger() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int i = MLGetInteger(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return i;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void put(int i) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutInteger(link, i);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized long getLongInteger() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		String s = MLGetString(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return Long.parseLong(s);
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void put(long i) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutNext(link, MLTKINT);
		byte[] bytes = String.valueOf(i).getBytes();
		MLPutSize(link, bytes.length);
		MLPutData(link, bytes, bytes.length);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized double getDouble() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		double d = MLGetDouble(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return d;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void put(double d) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		if (d == Double.POSITIVE_INFINITY)
			MLPutSymbol(link, "Infinity");
        else if (d == Double.NEGATIVE_INFINITY) {
            putFunction("DirectedInfinity", 1);
            MLPutInteger(link, -1);
        } else if (Double.isNaN(d))
            MLPutSymbol(link, "Indeterminate");
		else
			MLPutDouble(link, d);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized MLFunction getFunction() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int type = MLGetType(link);
		if (type == 0)
			throw new MathLinkException(MLError(link), MLErrorMessage(link));
		if (type != MLTKFUNC) {
			MLSetError(link, 3);// 3 is "Get out of sequence"
			throw new MathLinkException(MLError(link), MLErrorMessage(link));
		}
		int argc = MLGetArgCount(link);
		String head = MLGetSymbol(link);
		int errCode = MLError(link);
		if(errCode == 0) {
			return new MLFunction(head, argc);
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void putFunction(String f, int argCount) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutNext(link, MLTKFUNC);
		MLPutArgCount(link, argCount);
		MLPutSymbol(link, f);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized int checkFunction(String f) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int argCount = MLCheckFunction(link, f);
		int errCode = MLError(link);
		if(errCode == 0) {
			return argCount;
		} else {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void checkFunctionWithArgCount(String f, int argCount) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		int res = MLCheckFunctionWithArgCount(link, f, argCount);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	public synchronized void transferExpression(MathLink source) throws MathLinkException {

		if (link == 0 || source == null) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		if (source instanceof NativeLink) {
			MLTransferExpression(link, ((NativeLink)source).link);
		} else if (source instanceof WrappedKernelLink) {
			transferExpression(((WrappedKernelLink) source).getMathLink());
		} else {
			put(source.getExpr());
		}
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
		errCode = source.error();
		if (isException(errCode)) {
			throw new MathLinkException(errCode, source.errorMessage());
		}
	}

	public synchronized void transferToEndOfLoopbackLink(LoopbackLink source) throws MathLinkException {

		if (link == 0 || source == null) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		if (source instanceof NativeLoopbackLink) {
			MLTransferToEndOfLoopbackLink(link, ((NativeLoopbackLink) source).getLink());
		} else {
			// This branch is not intended to ever be used. Just a fall-through implementation that
			// should work in case someone ever writes a different implementation of LoopbackLink.
			while (source.ready()) {
				transferExpression(source);
			}
		}
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
		errCode = source.error();
		if (isException(errCode)) {
			throw new MathLinkException(errCode, source.errorMessage());
		}
	}

 // Message functions don't throw exceptions on MLErrors. They are also not synchronized.

	public int getMessage() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		return MLGetMessage(link);
	}

	public void putMessage(int msg) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutMessage(link, msg);
	}

	public boolean messageReady() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		return MLMessageReady(link);
	}


	public synchronized long createMark() throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		long mark = MLCreateMark(link);
		int errCode = MLError(link);
		if(isException(errCode)) {
            throw new MathLinkException(errCode, MLErrorMessage(link));
		} else if (mark == 0) {
            throw new MathLinkException(8 /* MLEMEM */, "Not enough memory to create Mark");
        } else {
            return mark;
		}
	}

	public synchronized void seekMark(long mark) {

		if (link == 0) {
			return;
		}
		MLSeekMark(link, mark);
	}

	public synchronized void destroyMark(long mark) {

		if (link == 0) {
			return;
		}
		MLDestroyMark(link, mark);
	}

	// The signature of the meth must be (V)Z. Can be static: pass null as target. Pass null for
	// methname to elminate callback from C yielder to Java. The C yield function always remains installed.
	public boolean setYieldFunction(Class cls, Object target, String methName) {

		// Convenient to set a yielder while a thread is blocking in MathLink. Thus, this method is not synchronized.
		// Instead, we synch on an object that is specific to the yielder-handling data structures.
		synchronized (yieldFunctionLock) {
			// This next line sets up the call from yielderCallback() to user's method.
			boolean res = super.setYieldFunction(cls, target, methName);
			boolean destroyYielder = (methName == null || !res);
			// This sets up or destroys the callback from C to the nativeYielderCallback method.
			MLSetYieldFunction(link, destroyYielder);
			return res;
		}
	}


	// The signature of the meth must be (II)V. Can be static; pass null as target.
	public synchronized boolean addMessageHandler(Class cls, Object target, String methName) {

		boolean result = super.addMessageHandler(cls, target, methName);
		if (result)
			// This establishes a callback from C to the nativeMessageCallback method.
			MLSetMessageHandler(link);
		return result;
	}


	public synchronized Object getArray(int type, int depth) throws MathLinkException {

		return getArray(type, depth, null);
	}

	public synchronized Object getArray(int type, int depth, String[] heads) throws MathLinkException {

		Object resArray = null;

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		if ((type == TYPE_INT || type == TYPE_DOUBLE || type == TYPE_BYTE || type == TYPE_CHAR ||
				type == TYPE_SHORT || type == TYPE_FLOAT) && (depth == 1 || !Utils.isRaggedArrays())) {
			// MLGetArray can pull in a whole N-dimensional array in the fastest way possible, but the array cannot be ragged.
			// This is the default method.
			resArray = MLGetArray(link, type, depth, heads);
			int errCode = MLError(link);
			if (errCode > MLE_NON_ML_ERROR) {
				throw new MathLinkException(errCode);
			} else if (errCode != 0) {
				throw new MathLinkException(errCode, MLErrorMessage(link));
			}
		} else {
			// The implementation in MathLinkImpl handles reading arrays "manually", a slice at a time. This method allows
			// arrays to be ragged, but it is much slower. The speed cost is not due to some of its implementation being in Java
			// instead of C, but rather because it uses MathLink in a way that picks the array apart rather than reading it
			// off the link in one call. It will eventually call back to this implementation of getArray to read the last level of
			// the array.
			resArray = super.getArray(type, depth, heads);
		}
		return resArray;
	}


	/////////////////////////////////////////  finalizer  ////////////////////////////////////////

	protected void finalize() throws Throwable {

		try {
			super.finalize();
		} finally {
            // See the comments for the JLinkShutdownThread class below
            // for the motivation for the jvmIsShuttingDown flag.
			if (!jvmIsShuttingDown)
                close();
		}
	}


	/////////////////////////////////  Protected methods  ////////////////////////////////////////

	// Decide if an MLError code is worthy of throwing an exception. Don't throw on 10
	// ("a deferred connection in still unconnected").
	protected static final boolean isException(int errCode) {
		return errCode != 0 && errCode != 10;
	}

	// Caller is put(Object) in MathLinkImpl
	protected void putString(String s) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		MLPutString(link, s);
		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}

	// Already guaranteed by caller (MathLinkImpl) that data is an array and not null. All
	// byvalue array-putting work goes through this function.
	protected void putArray(Object obj, String[] heads) throws MathLinkException {

		if (link == 0) {
			throw new MathLinkException(MLE_LINK_IS_NULL, LINK_NULL_MESSAGE);
		}
		Class objClass = obj.getClass();
		Class elementClass = Utils.getArrayComponentType(objClass);
		int type = 0;
		if (elementClass.isPrimitive()) {
			if (elementClass.equals(int.class)) {
				type = TYPE_INT;
			} else if (elementClass.equals(double.class)) {
				type = TYPE_DOUBLE;
			} else if (elementClass.equals(byte.class)) {
				type = TYPE_BYTE;
			} else if (elementClass.equals(char.class)) {
				type = TYPE_CHAR;
			} else if (elementClass.equals(short.class)) {
				type = TYPE_SHORT;
			} else if (elementClass.equals(long.class)) {
				type = TYPE_LONG;
			} else if (elementClass.equals(float.class)) {
				type = TYPE_FLOAT;
			} else if (elementClass.equals(boolean.class)) {
				type = TYPE_BOOLEAN;
			}
		} else if (elementClass.equals(String.class)) {
			type = TYPE_STRING;
		}
		// MathLink C API array functions have trouble with 0-length arrays. It is easiest to catch these up in Java
		// and force them to be routed through MLPutArray(), which has special code to handle this case. In other words,
		// we must make sure that arrays with a 0 anywhere in their dimensions get sent the slow way: putArraySlices().
		boolean anyDimsZero = false;
        // getArrayDims() will not be accurate for ragged arrays, but we won't use the result in that case.
		int[] dims = Utils.getArrayDims(obj);
		for (int i = 0; i < dims.length; i++)
			if (dims[i] == 0)
				anyDimsZero = true;
		if (type != 0) {
            boolean sent = false;
			int depth = objClass.getName().lastIndexOf('[') + 1;
			if (depth == 1) {
				MLPutArray(link, type, obj, heads != null ? heads[0] : "List");
                sent = true;
			} else if (depth > 1 && type != TYPE_STRING && type != TYPE_BOOLEAN &&
								nativeSizesMatch(type) && !anyDimsZero) {
				// Much faster to create a new flattened (1-D) array, to match the C-style memory layout of arrays.
				// This can be sent very efficiently over the link down in the native library. The cost is memory--an
				// extra copy of the array data is made here. To turn off this behavior and fall back to the slow method
				// (which was always used prior to J/Link 1.1), use a Java command line with -DJLINK_FAST_ARRAYS=false.
				int flatLen = 1;
				for (int i = 0; i < dims.length; i++)
					flatLen *= dims[i];
				Object flatArray;
				// Because byte and char types cannot be sent correctly using the appropriately-sized MLPutXXXArray
				// MathLink C API methods (signed<-->unsigned conversion occurs), we widen these arrays as they
				// are flattened. Other types are flattened without change.
				if (type == TYPE_BYTE)
					flatArray = Array.newInstance(short.class, flatLen);
				else if (type == TYPE_CHAR)
					flatArray = Array.newInstance(int.class, flatLen);
				else
					flatArray = Array.newInstance(elementClass, flatLen);
				try {
                    flattenInto(obj, dims, 0, flatArray, 0, type);
                    MLPutArrayFlat(link, type, flatArray, heads, dims);
                    sent = true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Thrown by flattenInto(), meaning array was ragged, not rectangular.
                    // Don't send or set sent flag.
                }
			}
			if (!sent) {
                // Either ragged, or other tests above were not met. Must send as slices.
				// This is equivalent to the pre-J/Link 1.1 method, except that the array is unwound in Java, not C.
				// We only call native code to put the last level of the array (a 1-dimensional slice). This method
				// works for everything, and it allows ragged arrays. It can be very slow for arrays where the product
				// of the first depth - 1 dimensions is very large (say, > 50000). In other words, slowness is probably
				// only an issue for very large depth >= 3 arrays.
				// putArraySlices needs full explicit heads array, not null.
				String[] explicitHeads;
				if (heads != null && heads.length == depth) {
					explicitHeads = heads;
				} else {
					explicitHeads = new String[depth];
					for (int i = 0; i < depth; i++) {
                        if (heads != null && i < heads.length)
                            explicitHeads[i] = heads[i];
                        else
                            explicitHeads[i] = "List";
                    }
				}
				putArraySlices(obj, type, explicitHeads, 0);
			}
		} else {
			if (DEBUGLEVEL > 1) System.err.println("In putArray, calling piecemeal");
			// Not a primitive type; must put element-by-element.
			putArrayPiecemeal(obj, heads, 0);
		}

		int errCode = MLError(link);
		if (isException(errCode)) {
			throw new MathLinkException(errCode, MLErrorMessage(link));
		}
	}


    /////////////////////////////////  Private array utility methods  ////////////////////////////////////////

	private static void flattenInto(Object source, int[] sourceDims, int sourceDimsIndex, Object dest, int destIndex, int type) {

		int i;
		int numElements = sourceDims[sourceDimsIndex];

        // Here we detect that the array isn't rectangular (it's ragged). Rather than
        // create a new exception class we just throw ArrayIndexOutOfBoundsException.
        // That exception is handled by callers as an indication of raggedness.
        if (Array.getLength(source) != numElements)
            throw new ArrayIndexOutOfBoundsException();

		if (sourceDimsIndex == sourceDims.length - 1) {
			// See comment in putArray for explanation of why byte and char are treated differently.
			if (type == TYPE_BYTE) {
				byte[] ba = (byte[]) source;
				for (i = 0; i < numElements; i++)
					Array.setShort(dest, destIndex++, ba[i]);
			} else if (type == TYPE_CHAR) {
				char[] ca = (char[]) source;
				for (i = 0; i < numElements; i++)
					Array.setInt(dest, destIndex++, ca[i]);
			} else {
				System.arraycopy(source, 0, dest, destIndex, numElements);
			}
		} else {
			int destIndexIncrement = 1;
			for (i = sourceDimsIndex + 1; i < sourceDims.length; i++)
				destIndexIncrement *= sourceDims[i];
			for (i = 0; i < numElements; i++)
				flattenInto(Array.get(source, i), sourceDims, sourceDimsIndex + 1, dest, destIndex + destIndexIncrement * i, type);
		}
	}

	// headIndex is the index into the heads array that should be used for the expr at the
	// current level. Because we use heads and headIndex as the counter for what level we are at, heads must
	// never be null; the caller must make an array of "List" if necessary.
	private void putArraySlices(Object obj, int type, String[] heads, int headIndex) throws MathLinkException {

		if (headIndex == heads.length - 1) {
			// We have reached the last level of the array.
			MLPutArray(link, type, obj, heads[headIndex]);
		} else {
			int len = Array.getLength(obj);
			putFunction(heads[headIndex], len);
			for (int i = 0; i < len; i++)
				putArraySlices(Array.get(obj, i), type, heads, headIndex + 1);
		}
	}


//////////////////////////////////  Private  ///////////////////////////////////////


private static boolean loadNativeLib(String libName, String libDir) throws SecurityException {

	String actualLibName;
	if (!libDir.endsWith(java.io.File.separator))
		libDir = libDir + java.io.File.separator;
	actualLibName = System.mapLibraryName(libName);
	String fullLibPath = libDir + actualLibName;
	try {
		System.load(fullLibPath);
		return true;
	} catch (UnsatisfiedLinkError e) {
		String[] systemIDs = Utils.getSystemID();
		for (int i = 0; i < systemIDs.length; i++) {
			fullLibPath = libDir + "SystemFiles" + java.io.File.separator + "Libraries" + java.io.File.separator +
							systemIDs[i] + java.io.File.separator + actualLibName;
			try {
				System.load(fullLibPath);
				return true;
			} catch (UnsatisfiedLinkError ee) {}
		}
	}
	return false;
}


//////////////////////////////////  Yielding, Messages  ///////////////////////////////////////

	// Most of the work here is done by methods inherited from MathLinkImpl (messageCallback and yielderCallback).

	// These two functions are currently named in C code, so their names or signatures can't be
	// changed here.

	private boolean nativeYielderCallback(boolean ignore) {

		boolean backOut = yielderCallback();
		return backOut;
	}

	private void nativeMessageCallback(int message, int n) {
		messageCallback(message, n);
	}


    //////////////////////// Shutdown Hook Thread ///////////////////////////

    // This class is a "shutdown hook" that is used to prevent the unusual but
    // possible circumstance where the JLinkNativeLibrary has already been
    // unloaded at the time a NativeLink finalizer is called. The finalizer calls
	// the native method MLClose, which is an instant crash if the library has
    // been unloaded. This can only happen when some code invoked by J/Link calls
    // the very naughty method System.runFinalizersOnExit(true).
    // To avoid the crash we use the shutdown hook feature, new in JDK 1.3.
    // This thread will start and finish during shutdown but before
    // the runFinalizersOnExit system gets invoked. We set the jvmIsShuttingDown
    // flag here and then in the NativeLink finalizer we only call MLClose if the
    // flag is false.

    private static class JLinkShutdownThread extends Thread {

        public JLinkShutdownThread() {
            setName("J/Link Shutdown Hook Thread");
        }
        public void run() {
            jvmIsShuttingDown = true;
        }
    }


	////////////////////////Native method declarations///////////////////////////

    protected static native void MLInitialize();

	// These two are not static. They need access to 'this' object in native code for setting up info for yield/message callbacks.
	protected native long MLOpenString(String cmdline, String[] errMsg);
	protected native long MLOpen(int argc, String argv[], String[] errMsg);

	protected static native long MLLoopbackOpen(String[] errMsg);

    protected static native void MLSetEnvIDString(String id);
    protected static native String MLGetLinkedEnvIDString(long link);

	protected static native void MLConnect(long link);
	protected static native void MLClose(long link);
	protected static native String MLName(long link);

    protected static native void MLNewPacket(long link);
	protected static native int MLNextPacket(long link);
	protected static native void MLEndPacket(long link);

    protected static native int MLError(long link);
	protected static native boolean MLClearError(long link);
	protected static native String MLErrorMessage(long link);
	protected static native void MLSetError(long link, int err);

	protected static native boolean MLReady(long link);
	protected static native void MLFlush(long link);

	protected static native int MLGetNext(long link);
	protected static native int MLGetType(long link);
	protected static native void MLPutNext(long link, int type);
	protected static native int MLGetArgCount(long link);
	protected static native void MLPutArgCount(long link, int argCount);
	protected static native void MLPutData(long link, byte[] buf, int len);
	protected static native void MLPutSize(long link, int len);
	protected static native byte[] MLGetData(long link, int max);
	protected static native int MLBytesToGet(long link);
	protected static native int MLBytesToPut(long link);

	protected static native String MLGetString(long link);
	protected static native void MLPutString(long link, String s);
	protected static native byte[] MLGetByteString(long link, byte missing);
	protected static native void MLPutByteString(long link, byte[] data, int len);

	protected static native String MLGetSymbol(long link);
	protected static native void MLPutSymbol(long link, String s);

	protected static native int MLGetInteger(long link);
	protected static native void MLPutInteger(long link, int i);

	protected static native double MLGetDouble(long link);
	protected static native void MLPutDouble(long link, double i);

	// Version 1.1 change: MLPutArray only puts 1-D lists. It works for all primitive types (+ strings) and
	// is as fast as possible (the slowness will be elsewhere, as depth > 1 arrays have to be sliced).
	protected static native void MLPutArray(long link, int type, Object data, String head);
	// MLPutArrayFlat can put multi-dimensional primitive arrays very quickly, but they must be pre-flattened.
	// This is new in J/Link 1.1 and is by far the fastest method, if it can be used.
	protected static native void MLPutArrayFlat(long link, int type, Object data, String[] heads, int[] dims);
	protected static native Object MLGetArray(long link, int type, int depth, String[] heads);

	protected static native int MLCheckFunction(long link, String f);
	protected static native int MLCheckFunctionWithArgCount(long link, String f, int argCount);

	protected static native void MLTransferExpression(long dest, long source);
	protected static native void MLTransferToEndOfLoopbackLink(long dest, long source);

	protected static native int MLGetMessage(long link);
	protected static native void MLPutMessage(long link, int msg);
	protected static native boolean MLMessageReady(long link);

	protected static native long MLCreateMark(long link);
	protected static native void MLSeekMark(long link, long mark);
	protected static native void MLDestroyMark(long link, long mark);

	protected static native void MLSetYieldFunction(long link, boolean remove);
	protected static native void MLSetMessageHandler(long link);

	// When we send primitive arrays, we need to know ahead of time whether the size of the JNI type equals a type
	// that can be accommodated by a MathLink API MLPutXXXArray function. This function tells us.
	protected static native boolean nativeSizesMatch(int type);

	// Note that the rest are not protected (called from other files in package).

	static native void hideJavaWindow();
	static native void macJavaLayerToFront();
	static native void winJavaLayerToFront(boolean pre);
	static native void mathematicaToFront();
    // Returns a native window handle for the given Java window. On Windows, this will
    // be the HWND. This is currently only implemented to do something meaningful
    // on Windows (returns the "error" code -1 on other platforms. This is to support
	// the ability to register Java windows to be managed by the FE.
    static native long getNativeWindowHandle(java.awt.Window obj, String javaHome);

	// Added as a utility for webMathematica. Not documented, and not part of the J/Link API. Returns 0 to indicate
	// success, nonzero to indicate failure. Actual nonzero value is OS-specific.
	public static native int killProcess(long pid);

    public static native void appToFront(long pid);
}
