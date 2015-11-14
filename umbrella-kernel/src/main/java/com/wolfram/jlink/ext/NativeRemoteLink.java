package com.wolfram.jlink.ext;

import com.wolfram.jlink.*;

// Run rmic on this class like this:
//    E:\MathJava\JLink\src\Java>rmic -v1.2 -classpath . -d . com.wolfram.jlink.ext.NativeRemoteLink


public class NativeRemoteLink extends NativeLink implements RemoteMathLink {
	
    private String cmdLine = null;
    private String[] argv = null;
    private boolean isOpen = false;
    
    
	public NativeRemoteLink(String cmdLine) throws MathLinkException {
        
        super(cmdLine);
        this.cmdLine = cmdLine;
        isOpen = true;
	}

	public NativeRemoteLink(String[] argv) throws MathLinkException {
        
        super(argv);
        this.argv = argv;
        isOpen = true;
	}
	
    public synchronized void close() {
        // Do everything possible to kill the kernel before closing the link.
        // This is a decision based on how a remote link is likely to be used.
        // For example, typically a long-lived server process stays alive for
        // multiple client connect/disconnect cycles. If a client calls close()
        // we assume that they mean to walk away from this kernel and that
        // no one else is likely to be using it. We want to avoid having
        // orphaned kernels remain running.
        try { putMessage(MLTERMINATEMESSAGE); } catch (MathLinkException e) {}
        super.close();
        isOpen = false;
    }
    
    
    
    // Override all methods that might begin a computation
    // and make sure they reopen the link if necessary.
    
    public synchronized void connect() throws MathLinkException {
        reopenIfNecessary();
        super.connect();
    }
    
    public synchronized long createMark() throws MathLinkException {
        reopenIfNecessary();
        return super.createMark();
    }
    
    public synchronized void putFunction(String f, int argCount) throws MathLinkException {
        reopenIfNecessary();
        super.putFunction(f, argCount);
    }
    
    public synchronized void putNext(int type) throws MathLinkException {
        reopenIfNecessary();
        super.putNext(type);
    }
    
    public synchronized void putSymbol(String s) throws MathLinkException {
        reopenIfNecessary();
        super.putSymbol(s);
    }
    
    
    public synchronized void sendString(String s) throws MathLinkException {
        putString(s);
    }
    
    
    protected void reopenIfNecessary() throws MathLinkException {
        
        if (isOpen)
            return;
        
        // Create a new link object and co-opt its 'link' variable
        // (the only state held in a NativeLink object).
        NativeRemoteLink newLink;
        if (cmdLine != null)
            newLink = new NativeRemoteLink(cmdLine);
        else
            newLink = new NativeRemoteLink(argv);
        this.link = newLink.link;
        // Clear out the link value to prevent finalizer for newLink
        // from trying to close() that link.
        newLink.link = 0;
        isOpen = true;
    }
}
