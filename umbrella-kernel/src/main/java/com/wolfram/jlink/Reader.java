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

// The Reader is what listens for calls from Mathematica for the "installable Java" functionality
// of J/Link. When InstallJava[] is called, the Reader thread is started up. It either polls ml.ready()
// or blocks in ml.nextPacket() (this is the default), depending on settings controlled from Mathematica.
// Blocking will hang the whole JVM unless either
//    - the JVM supports native threads
//    - a yield function is used to call back into Java
// All windows JVMs support native threads, and they are available for all UNIX platforms (although
// you might have to get a recent one). Blocking will give the best performance, since it avoids the
// busy-wait associated with polling. On the Macintosh, native threads are not available, so we block
// and use a yielder. On UNIX, we poll in non-native threads VMs (it is the users responsibility inform
// J/Link that native threads are not supported by setting $NativeThreads = False before calling InstallJava[]).
// When blocking, the Reader thread owns the link's monitor, so computations that need to originate on
// another thread (typically the UI thread) cannot proceed. The kernel has to call jAllowUIComputations
// to cause the Reader to stop blocking and begin polling, which it does for the period during which
// UI computations are allowed. This period can be an extended time if DoModal[] is called, or it can
// be just a single computation (as allowed by ShareKernel[] or ServiceJava[]).
//
// Calls arrive in the form of CallPackets, and they are sent to the KernelLink's handleCallPacket() method.

public class Reader extends Thread { 

	private KernelLink ml;
	private boolean quitWhenLinkEnds = true;
	private boolean requestedStop = false;
    // The Reader needs to operate slightly differently on the mainLink (JavaLink[]) than when used
    // on the new preemptiveLink. Specifically, it never polls on the preemptiveLink because that
    // link can never participate in DoModal[]. This variables distinguishes the two behaviors of this class.
    private boolean isMainLink;
	
	private static Reader reader; // Singleton instance
	
	private static int sleepInterval = 2;
	
	
	// startReader() and stopReader() allow advanced programmers to manage a Reader thread in their own programs,
	// if they desire. This can be tricky, and these methods are not formally documented for now (although they are public).
	// One reason to start a Reader thread is to allow scriptability of a Java application that was launched standalone.
	// Your program must then be able to handle requests arriving from the kernel at unexpected times. This means that you
	// need a component in your program that is able to read incoming packets all the time. That is what the Reader thread is.
	// You might also want to have your Java program and the front end share the kernel so that either can be used to initiate
	// computations. ShareKernel requires a Reader thread because it peppers the Java link with calls (for sleeping and for
	// jUIThreadWaiting[]).
	//
	// Users who call startReader() will probably want to set quitWhenLinkEnds=false (so the JVM does
	// not exit when the link or the Reader thread dies) and alwaysPoll=true. The main advantage of alwaysPoll is that by
	// forcing polling you make it possible to stop the Reader thread by simply calling stopReader(). It is not logically
	// required to force polling--as long as your program calls StdLink.requestTransaction() and synchronizes properly
	// on the link (as documented elsewhere), it should work with either polling or blocking.
	//
	// Once you call startReader(), your StdLink.requestTransaction() calls will block until the kernel gives them
	// permission to proceed (via ShareKernel, DoModal, or ServiceJava). So call ShareKernel or equivalent right after
	// you start the Reader. And don't forget that when the Reader is running, _every_ computation you send must be
	// guarded by StdLink.requestTransaction() and synchronized(ml), whether from the main thread or the UI thread.
	
	public static Thread startReader(KernelLink ml, boolean quitWhenLinkEnds, boolean alwaysPoll) {
		
		reader = new Reader(ml, quitWhenLinkEnds, true);
		reader.start();
        // We set StdLink to be the main link primarily for pre-5.1 kernels. For 5.1+ kernels there is a second UI link
        // that will be used for calls to M from the UI thread (unless we are in the modal state, in which case
        // the link we set here is used).
		StdLink.setLink(ml);
		StdLink.setHasReader(true);
		if (MathLinkImpl.DEBUGLEVEL > 1) System.err.println("Reader thread started.");
		return reader;
	}
	
	
	// StopReader() will generally only be effective if you have called startReader() with alwaysPoll=true. Always call this
	// instead of (or at least before) stop() on the thread (unless you are about to exit the VM).
	// When Java is launched via InstallJava[] (the normal mode for "installable Java"), this method is not used--the
	// Reader thread dies when the link to the kernel is killed.
		
	public static Thread stopReader() {
		
		reader.requestedStop = true;
		StdLink.setHasReader(false);
		return reader;
	}
	
	
	// Only advanced users (e.g., WRI developers) will call this constructor directly. Programmers
    // who want a Reader thread in their own apps will call the static startReader() method.
    
	public Reader(KernelLink ml, boolean quitWhenLinkEnds, boolean isMainLink) {

		super("J/Link reader" + (isMainLink ? "" : "2"));
		// This helps code that runs in calls from M to be able to find the same set of classes
		// that LoadClass can find. In other words, factory methods that are called from M that do their own class
		// lookup internally might choose to use the thread's contextClassLoader, so we set this for them to be
		// the JLinkClassLoader. An example where this is important is javax.xml.parsers.SAXParserFactory. 
		setContextClassLoader(ml.getClassLoader());
		this.ml = ml;
		this.quitWhenLinkEnds = quitWhenLinkEnds;
        this.isMainLink = isMainLink;
		ml.addMessageHandler(null, this, "terminateMsgHandler");
	}
	
	
	public void run() {
				
		long loopsAgo = 0;
		boolean mustPoll = false;
		try {
			while (!requestedStop) {
				if (isMainLink && mustPoll) {
					// Polling is much less efficient than blocking. It is used only in special circumstances (such as while the kernel is
					// executing DoModal[], or after the kernel has called jAllowUIComputations[True]). It is also used on non-native threads
					// UNIX JVMs (this use is controlled from Mathematica via jForcePolling).
					boolean isReady = false;
					try {
						isReady = ml.ready();
						if (!isReady)
							sleep(sleepInterval + Math.min(loopsAgo++, 20));
					} catch (Exception e) {}
					synchronized (ml) {
						try {
							if (ml.error() != 0)
								throw new MathLinkException(ml.error(), ml.errorMessage());
							if (isReady) {
								loopsAgo = 0;
								int pkt = ml.nextPacket();
								ml.handlePacket(pkt);
								ml.newPacket();
								mustPoll = StdLink.mustPoll();
							}
						} catch (MathLinkException e) {
							if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException in Reader thread: " + e.toString());
							// 11 is "other side closed link"; not sure why this succeeds clearError, but it does.
							if (e.getErrCode() == 11 || !ml.clearError())
								return;
							ml.newPacket();
						}
					}
				} else {
					// Use blocking style (dive right into MLNextPacket and block there). Much more efficient. Requires a native threads JVM
					// or a yield function callback into Java; otherwise, all threads in the JVM will hang.
					synchronized (ml) {
						try {
							int pkt = ml.nextPacket();
							ml.handlePacket(pkt);
							ml.newPacket();
							mustPoll = StdLink.mustPoll();
						} catch (MathLinkException e) {
							if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException in Reader thread: " + e.toString());
							// 11 is "other side closed link"; not sure why this succeeds clearError, but it does.
							if (e.getErrCode() == 11 || !ml.clearError())
								return;
							ml.newPacket();
						}
					} // end synchronized
				}
			}
        } finally {
			// Get here on unrecoverable MathLinkException, ThreadDeath exception caused by "hard" aborts
			// from Mathematica (see KernelLinkImpl.msgHandler()), or other Error exceptions (except during invoke()).
			if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Reader thread shutting down");
			// TODO: For sake of JavaKernel, do I want to move the link-closing stuff up here before the quitWhenLinkEnds test?
			if (quitWhenLinkEnds) {
				ml.close();
				ml = null;
                if (StdLink.getUILink() != null) {
                    StdLink.getUILink().close();
                    StdLink.setUILink(null);
                }
				StdLink.setLink(null);
				JLinkSecurityManager.setAllowExit(true);
				System.exit(0);
			}
		}
	}    


	
	// This is what causes Java to quit when user selects "Kill linked program" from Mathematica's Interrupt dialog.
	public void terminateMsgHandler(int msg, int ignore) {

		if (msg == MathLink.MLTERMINATEMESSAGE) {
			// Will throw ThreadDeath exception, which triggers finally clause in run(), closing link and killing Java.
			if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("In Reader.terminateMsgHandler: about to call stop");
			stop();
			
			// On some systems (Linux, perhaps all UNIX), if you are blocking in a read call, the stop() above will
			// have no effect. Something about being called from a native method, I suppose (the stop() works fine
			// if Java is busy with something other than blocking inside MathLink). For such cases, we set a yielder
			// that just returns true to make the read back out. Then the Reader thread stops.
			ml.setYieldFunction(null, this, "terminateYielder");
			requestedStop = true;  // Just in case.
		}
	}

	
	boolean terminateYielder() {
		return true;
	}

}

