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

public class NativeLoopbackLink extends NativeLink implements LoopbackLink {


	public NativeLoopbackLink() throws MathLinkException {

	    loadNativeLibrary();
	    String[] errMsgOut = new String[1];
        synchronized (environmentLock) {
            link = MLLoopbackOpen(errMsgOut);
        }
		if (link == 0) {
			String msg = errMsgOut[0] != null ? errMsgOut[0] : CREATE_FAILED_MESSAGE;
			throw new MathLinkException(MathLink.MLE_CREATION_FAILED, msg);
		}
	}

    public NativeLoopbackLink(long mlinkPtr) {
        super(mlinkPtr);
    }

    // These ops are no-nos on a loopback link, so we override NativeLink implementations.
	public boolean setYieldFunction(Class cls, Object obj, String methName) {return false;}
	public boolean addMessageHandler(Class cls, Object obj, String methName) {return false;}

}
