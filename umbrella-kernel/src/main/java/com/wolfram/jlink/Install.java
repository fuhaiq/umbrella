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

package com.wolfram.jlink;public class Install {
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

    public static boolean install(MathLink ml) {        return install(ml, Integer.MAX_VALUE);    }

    public static boolean install(MathLink ml, int timeout) {		try {			ml.connect(timeout);			return true;		} catch (MathLinkException e) {			if (MathLinkImpl.DEBUGLEVEL > 1) System.err.println("Fatal error: MathLinkException during Install.");			return false;		}	}
}
