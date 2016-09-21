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

/**
 * The exception thrown by methods in the MathLink and KernelLink interfaces when
 * a link error occurs.
 * <p>
 * MathLinkExceptions are only for errors that involve the low-level link itself. After
 * you catch a MathLinkException, the first thing you should do is call clearError() to
 * try to clear the error condition. If you do not, then the next MathLink or KernelLink method
 * you call will throw an exception again.
 * <p>
 * For programmers familiar with the C-language MathLink API, the throwing of a MathLinkException
 * is equivalent to a C-language function returning a result code other than MLEOK.
 */

public class MathLinkException extends Exception {

	String msg;
	int code;
	Throwable wrappedException;

	/**
	 * Creates a MathLinkException given a code and textual description. Programmers will probably
	 * have no need to create their own MathLinkExceptions.
	 * 
	 * @param code the integer error code
	 * @param msg the descriptive message
	 */
	
	public MathLinkException(int code, String msg) {
	
		this.code = code;
		this.msg = msg;
	}

	/**
	 * Creates a MathLinkException given a code. Programmers will probably
	 * have no need to create their own MathLinkExceptions.
	 * 
	 * @param code the integer error code
	 */

	// Only use this form for non-MathLink errors.
	public MathLinkException(int code) {
	
		this.code = code;
		msg = lookupMessageText(code);
	}

	/**
	 * Creates a MathLinkException by wrapping another type of exception. Only advanced
	 * programmers writing their own implementations of the MathLink interface will need
	 * to be concerned with this.
	 * <p>
	 * This technique allows
	 * exceptions thrown by special-purpose implementations of the MathLink interface to be
	 * received and treated like they were native MathLink errors. Thus, client code does not
	 * need to be rewritten to accommodate special exception types thrown by a special type of
	 * MathLink. As an example, this is used by the RMI implementation of MathLink (not part of
	 * current standard JLink package) to wrap java.rmi.RemoteExceptions thrown by the RMI
	 * protocol.
	 * 
	 * @param e the exception to wrap
	 */
	
	public MathLinkException(Throwable e) {
		this(e, "");
	}
	
	/**
	 * Creates a MathLinkException by wrapping another type of exception. Only advanced
	 * programmers writing their own implementations of the MathLink interface will need
	 * to be concerned with this.
	 * <p>
	 * This technique allows
	 * exceptions thrown by special-purpose implementations of the MathLink interface to be
	 * received and treated like they were native MathLink errors. Thus, client code does not
	 * need to be rewritten to accommodate special exception types thrown by a special type of
	 * MathLink. As an example, this is used by the RMI implementation of MathLink (not part of
	 * current standard JLink package) to wrap java.rmi.RemoteExceptions thrown by the RMI
	 * protocol.
	 * 
	 * @param e the exception to wrap
	 * @param extraMsg extra textual information you want included in the output of getMessage(),
	 * above and beyond the default of toString() of the wrapped exception.
	 */
	
	public MathLinkException(Throwable e, String extraMsg) {
	
		code = MathLink.MLE_WRAPPED_EXCEPTION;
		wrappedException = e;
		String separator = "";
		if (extraMsg.endsWith("."))
			separator = " ";
		else if (extraMsg.length() > 0)
			separator = ". ";
		msg = extraMsg + separator + "Was a wrapped exception. Original exception was: " + e.toString();
	}

	/**
	 * Gives the low-level MathLink error code describing the link error.
	 * 
	 * @return the integer error code
	 * @see #toString()
	 * @see #getMessage()
	 */
	
	public int getErrCode() {
		return code;
	}

	/**
	 * Gives the textual message describing the link error.
	 * 
	 * @see #toString()
	 */
	
	// Override Throwable method. 
	public String getMessage() {
		return msg;
	}

	/**
	 * Gives the other type of exception that this MathLinkException &quot;wraps&quot;.
	 * <p>
	 * Some MathLinkExceptions are not &quot;native&quot; MathLink errors, but rather special
	 * exceptions thrown by special-purpose implementations of the MathLink interface.
	 * To allow these exceptions to be received and treated like they were native MathLink errors,
	 * yet retain their full type information, a MathLinkException can be created that
	 * &quot;wraps&quot; another exception. Thus, client code does not
	 * need to be rewritten to accommodate special exception types. As an example, this thechnique
	 * is used by the RMI implementation of MathLink (not part of the
	 * current standard J/Link package) to wrap java.rmi.RemoteExceptions thrown by the RMI
	 * protocol.
	 * 
	 * @return the wrapped exception, or null if this is a normal native MathLinkException
	 * @see #getCause()
	 */
	
	public Throwable getWrappedException() {
		return wrappedException;
	}

	/**
	 * Gives the other type of exception that this MathLinkException &quot;wraps&quot;.
	 * <p>
	 * Identical to the getWrappedException() method, but included to conform to the design
	 * pattern for "chained" exceptions introduced in Java 1.4.
	 * 
	 * @return the wrapped exception, or null if this is a normal native MathLinkException
	 * @see #getWrappedException()
	 */
	
	public Throwable getCause() {
		return getWrappedException();
	}

	/**
	 * Gives a string representation of the exception suitable for display to a user. The
	 * information includes both the error code and the message.
	 * 
	 * @see #getMessage()
	 * @see #getErrCode()
	 */
		
	public String toString() {
		return "MathLinkException: " + String.valueOf(code) + ": " + msg;
	}
	
	static String lookupMessageText(int code) {

		String res = null;
		switch (code) {
			case MathLink.MLE_ARRAY_TOO_SHALLOW:
				res = "Array is not as deep as requested.";
				break;
			case MathLink.MLE_BAD_COMPLEX:
				res = "Expression could not be read as a complex number.";
				break;
			case MathLink.MLE_CONNECT_TIMEOUT:
				res = "The link was not connected before the requested time limit elapsed.";
				break;
			case KernelLink.MLE_BAD_OBJECT:
				res = "Expression on link is not a valid Java object reference.";
				break;
			default:							
				res = "Extended error message not available.";
		}
		return res;
	}

}