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

/**
 * MathLink is the low-level interface that is the root of all link objects in J/Link.
 * The methods in MathLink correspond roughly to a subset of those in the C-language
 * MathLink API. Most programmers will deal instead with objects of type KernelLink, a
 * higher-level interface that extends MathLink and incorporates the assumption that the
 * program on the other side of the link is a Mathematica kernel.
 * <p>
 * You create objects of type MathLink via the MathLinkFactory.createLink() method. Again,
 * though, most programmers will use KernelLink instead of MathLink.
 * <p>
 * Most MathLink methods throw a MathLinkException if a link-related error occurs. Examples
 * would be calling endPacket() before sending a complete expression, or calling getFunction()
 * when an integer is waiting on the link.
 * <p>
 * For additional information about these methods, see the J/Link User Guide, and also
 * the MathLink documentation in the Mathematica book. Most of these methods are
 * substantially similar, if not identical, to their C counterparts as documented in
 * the book.
 *
 * @see KernelLink
 * @see MathLinkFactory
 */

public interface MathLink {


	int ILLEGALPKT		= 0;
	/**
	 * Constant returned by nextPacket.
	 */
	int CALLPKT			= 7;
	/**
	 * Constant returned by nextPacket.
	 */
	int EVALUATEPKT	= 13;
	/**
	 * Constant returned by nextPacket.
	 */
	int RETURNPKT		= 3;
	/**
	 * Constant returned by nextPacket.
	 */
	int INPUTNAMEPKT	= 8;
	/**
	 * Constant returned by nextPacket.
	 */
	int ENTERTEXTPKT	= 14;
	/**
	 * Constant returned by nextPacket.
	 */
	int ENTEREXPRPKT	= 15;
	/**
	 * Constant returned by nextPacket.
	 */
	int OUTPUTNAMEPKT	= 9;
	/**
	 * Constant returned by nextPacket.
	 */
	int RETURNTEXTPKT	= 4;
	/**
	 * Constant returned by nextPacket.
	 */
	int RETURNEXPRPKT	= 16;
	/**
	 * Constant returned by nextPacket.
	 */
	int DISPLAYPKT		= 11;
	/**
	 * Constant returned by nextPacket.
	 */
	int DISPLAYENDPKT	= 12;
	/**
	 * Constant returned by nextPacket.
	 */
	int MESSAGEPKT		= 5;
	/**
	 * Constant returned by nextPacket.
	 */
	int TEXTPKT			= 2;
	/**
	 * Constant returned by nextPacket.
	 */
	int INPUTPKT		= 1;
	/**
	 * Constant returned by nextPacket.
	 */
	int INPUTSTRPKT	= 21;
	/**
	 * Constant returned by nextPacket.
	 */
	int MENUPKT			= 6;
	/**
	 * Constant returned by nextPacket.
	 */
	int SYNTAXPKT		= 10;
	/**
	 * Constant returned by nextPacket.
	 */
	int SUSPENDPKT		= 17;
	/**
	 * Constant returned by nextPacket.
	 */
	int RESUMEPKT		= 18;
	/**
	 * Constant returned by nextPacket.
	 */
	int BEGINDLGPKT	= 19;
	/**
	 * Constant returned by nextPacket.
	 */
	int ENDDLGPKT		= 20;

	int FIRSTUSERPKT	= 128;
	int LASTUSERPKT	= 255;
	int FEPKT			= 100;   // Catch-all for packets that need to go to FE.
	int EXPRESSIONPKT	= 101;  // Sent for Print output


	int MLTERMINATEMESSAGE	= 1;
	int MLINTERRUPTMESSAGE	= 2;
	/**
	 * Used in putMessage() to cause the current Mathematica evaluation to be aborted.
	 */
	int MLABORTMESSAGE		= 3;
	/**
	 * Low-level message type that will be detected by a messagehandler function if the
	 * kernel fails to start because of an authentication error (e.g., incorrect password file).
	 */
	int MLAUTHENTICATEFAILURE = 10;

	/**
	 * Constant for use in putNext() or returned by getNext() and getType().
	 */
	int MLTKFUNC	= 'F';
	/**
	 * Constant for use in putNext() or returned by getNext() and getType().
	 */
	int MLTKSTR		= '"';
	/**
	 * Constant for use in putNext() or returned by getNext() and getType().
	 */
	int MLTKSYM		= '\043';
	/**
	 * Constant for use in putNext() or returned by getNext() and getType().
	 */
	int MLTKREAL	= '*';
	/**
	 * Constant for use in putNext() or returned by getNext() and getType().
	 */
	int MLTKINT		= '+';

	int MLTKERR		= 0;


	int MLEOK		= 0;
	int MLEUSER		= 1000;

	// Some of these need to agree with C code.
	int MLE_NON_ML_ERROR				= MLEUSER;
	int MLE_LINK_IS_NULL				= MLEUSER;
	int MLE_OUT_OF_MEMORY			= MLEUSER + 1;
	int MLE_ARRAY_TOO_SHALLOW		= MLEUSER + 2;
	int MLE_BAD_COMPLEX				= MLEUSER + 3;  // Data on link not appropriate for getComplex()
	int MLE_CREATION_FAILED			= MLEUSER + 4;
	int MLE_CONNECT_TIMEOUT			= MLEUSER + 5;
	int MLE_WRAPPED_EXCEPTION		= MLEUSER + 6;

	int MLE_FIRST_USER_EXCEPTION	= MLEUSER + 1000;

	// These must remain in sync with Mathematica and C code. They don't really belong here,
	// but they are used in a few places, so it's convenient to dump them here.
	// If you change any of these, consult KernelLinkImpl, which has a few that
	// pick up where these leave off.

	/**
	 * Constant for use in getArray().
	 */
	int TYPE_BOOLEAN		= -1;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_BYTE			= -2;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_CHAR			= -3;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_SHORT			= -4;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_INT			= -5;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_LONG			= -6;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_FLOAT			= -7;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_DOUBLE		= -8;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_STRING		= -9;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_BIGINTEGER	= -10;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_BIGDECIMAL	= -11;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_EXPR			= -12;
	/**
	 * Constant for use in getArray().
	 */
	int TYPE_COMPLEX		= -13;
	// TYPE_COMPLEX must always be the last one (largest absolute value number) of the set of types that have a byvalue representation.
	// This rule does not apply to TYPE_DOUBLEORINT or TYPE_FLOATORINT, which are defined KernelLinkImpl and are not user-level constants.
	// They are never supplied as an argument to any J/Link method.

	/**
	 * Closes the link. Always call close() on every link when you are done using it.
	 */
	public void close();

	/**
	 * Connects the link, if it has not already been connected. There is a difference between
	 * opening a link (which is what the MathLinkFactory methods createLink() and createKernelLink()
	 * do) and connecting it, which verifies that it is alive and ready for data transfer.
	 * <p>
	 * All the methods that read from the link will connect it if necessary. The connect() method
	 * lets you deliberately control the point in the program where the connection occurs,
	 * without having to read anything.
	 *
	 * @exception MathLinkException
	 */
	public void connect() throws MathLinkException;

	/**
	 * Connects the link, but waits at most timeoutMillis for a response from the other side.
	 * If the timeout passes, a MathLinkException is thrown. This is a handy way to prevent your
	 * thread from hanging indefinitely if there is a problem in connecting. Such problems are not
	 * hard to generate--for example, if the user launches a process other than a Mathematica kernel
	 * in their arguments to MathLinkFactory.createKernelLink(), the Java side will likely hang at
	 * the stage of connecting. If the timeout passes and an exception is thrown, the link cannot
	 * be salvaged and should be closed. Make sure you give adequate time for the kernel to launch.
	 *
	 * @exception MathLinkException
	 * @param timeoutMillis the number of milliseconds to wait
	 * @see #connect()
	 */
	public void connect(long timeoutMillis) throws MathLinkException;

	/**
	 * Same as connect().
	 *
	 * @exception MathLinkException
	 * @see #connect()
	 */
	public void activate() throws MathLinkException;

    /**
     * Gives the name of the link. For typical links, the name of a listen-mode link can be used
     * by the other side to connect to.
     *
     * @exception MathLinkException
     */
    public String name() throws MathLinkException;

	/**
	 * Discards the current packet, if it has been partially read. Has no effect if the
	 * previous packet was fully read.
	 * <p>
	 * This is a useful cleanup function. You can call it when you are finished examining
	 * the contents of a packet that was opened with nextPacket() or waitForAnswer(), whether
	 * you have read the entire packet contents or not. You can be sure that the link is
	 * then in a state where you are ready to read the next packet.
	 * <p>
	 * It is also frequently used in a catch block for a MathLinkException, to clear off
	 * any unread data in a packet before returning to the normal program flow.
	 *
	 * @see #nextPacket()
	 */
	public void newPacket();

	/**
	 * &quot;Opens&quot; the next packet arriving on the link. It is an error to call nextPacket()
	 * while the current packet has unread data; use newPacket() to discard the current packet
	 * first.
	 * <p>
	 * Most programmers will use this method rarely, if ever. J/Link provides higher-level
	 * functions in the KernelLink interface that hide these low-level details of the
	 * packet loop.
	 *
	 * @exception MathLinkException
	 * @return the packet type (e.g., RETURNPKT, TEXTPKT).
	 * @see #newPacket()
	 */
	public int nextPacket() throws MathLinkException;

	/**
	 * Call it when you are finished writing the contents of a single packet.
	 * <p>
	 * Calling endPacket() is not strictly necessary, but it is good style, and it allows
	 * J/Link to immediately generate a MathLinkException if you are not actually
	 * finished with writing the data you promised to send.
	 *
	 * @exception MathLinkException
	 */
	public void endPacket() throws MathLinkException;

	/** Gives the code corresponding to the current error state of the link.
	 *
	 * @return the error code; will be MLEOK if no error.
	 * @see #errorMessage()
	 * @see #clearError()
	 */
	public int error();

	/**
	 * Clears the link error condition, if possible. After an error has occurred,
	 * and a MathLinkException has been caught, you must call clearError() before doing
	 * anything else with the link.
	 *
	 * @return true, if the error state was cleared; false, if the error could not be cleared.
	 * If false, you must close the link.
	 */
	public boolean clearError();

	/** Gives a textual message describing the current error.
	 *
	 * @return the error message
	 * @see #error()
	 */
	public String errorMessage();

	/** Sets the link's error state to the specified value. Afterwards, error() will return this
	 * value. Very few programmers will have any need for this method.
	 *
	 * @param err the error code
	 */
	public void setError(int err);

	/** Indicates whether the link has data waiting to be read. In other words, it tells
	 * whether the next call that reads data will block or not.
	 *
	 * @exception MathLinkException
	 * @return true, if data is waiting; false otherwise
	 */
	public boolean ready() throws MathLinkException;

	/** Immediately transmits any data buffered for sending over the link.
	 * <p>
	 * Any calls that read from the link will flush it, so you only need to call flush()
	 * manually if you want to make sure data is sent right away even though you are
	 * <i>not</i> reading from the link immediately. Calls to ready() will not flush
	 * the link, so if you are sending something and then polling ready() waiting
	 * for the result to arrive (as opposed to just calling nextPacket() or waitForAnswer()),
	 * you must call flush to ensure that the data is sent.
	 *
	 * @exception MathLinkException
	 */
	public void flush() throws MathLinkException;

	/** Gives the type of the next element in the expression currently being read.
	 * <p>
	 * To check the type of a partially read element without advancing to the next element,
	 * use getType().
	 *
	 * @exception MathLinkException
	 * @return one of MLTKINT, MLTKREAL, MLTKSTR, MLTKSYM, MLTKFUNC
	 */
	public int getNext() throws MathLinkException;

	/** Gives the type of the current element in the expression currently being read.
	 * <p>
	 * Unlike getNext(), getType() will not advance to the next element if the current element has
	 * only been partially read.
	 *
	 * @exception MathLinkException
	 * @return one of MLTKINT, MLTKREAL, MLTKSTR, MLTKSYM, MLTKFUNC
	 * @see #getNext()
	 */
	public int getType() throws MathLinkException;

	/** Identifies the type of data element that is to be sent.
	 * <p>
	 * putNext() is rarely needed. The two most likely uses are to put expressions whose heads are
	 * not mere symbols (e.g., Derivative[2][f]) or to put data in textual form.
	 * Calls to putNext() must be followed by putSize() and putData(), or by putArgCount() for the MLTKFUNC
	 * type. Here is how you could send Derivative[2][f]:
	 * <pre>
	 * ml.putNext(MathLink.MLTKFUNC);  // The func we are putting has head Derivative[2], arg f
	 * ml.putArgCount(1);  // this 1 is for the 'f'
	 * ml.putNext(MathLink.MLTKFUNC);  // The func we are putting has head Derivative, arg 2
	 * ml.putArgCount(1);  // this 1 is for the '2'
	 * ml.putSymbol(&quot;Derivative&quot;);
	 * ml.put(2);
	 * ml.putSymbol(&quot;f&quot;);</pre>
	 *
	 * @exception MathLinkException
	 * @param type one of MLTKINT, MLTKREAL, MLTKSTR, MLTKSYM, MLTKFUNC
	 * @see #putSize(int)
	 * @see #putData(byte[])
	 * @see #putArgCount(int)
	 */
	public void putNext(int type) throws MathLinkException;

	/** Reads the argument count of an expression being read manually.
	 * <p>
	 * This method can be used after getNext() or getType() returns the value MLTKFUNC. The argument
	 * count is always followed by the head of the expression. The head is followed by the
	 * arguments; the argument count tells how many there will be.
	 *
	 * @exception MathLinkException
	 * @return the number of arguments in the expression being read
	 * @see #getNext()
	 * @see #getType()
	 */
	public int getArgCount() throws MathLinkException;

	/** Specifies the argument count for a composite expression being sent manually.
	 * <p>
	 * Use it after a call to putNext() with the MLTKFUNC type.
	 *
	 * @exception MathLinkException
	 * @param argCount the number of aruments in the expression being sent
	 * @see #putNext(int)
	 */
	public void putArgCount(int argCount) throws MathLinkException;

	/** Specifies the size in bytes of an element being sent in textual form.
	 * <p>
	 * A typical sequence would be putNext(), followed by putSize(), then putData().
	 *
	 * @exception MathLinkException
	 * @param size the size of the data, in bytes, that will be written
	 *     with the following putData()
	 * @see #putData(byte[])
	 * @see #putNext(int)
	 */
	public void putSize(int size) throws MathLinkException;

	/** Gives the number of bytes that remain to be sent in the element that is currently
	 * being sent in textual form.
	 * <p>
	 * After you have called putSize(), the link knows how many bytes you have promised to send.
	 * This method lets you determine how many you still need to send, in the unlikely event
	 * that you lose track after a series of putData() calls.
	 *
	 * @exception MathLinkException
	 * @return the number of bytes that remain to be sent
	 * @see #putSize(int)
	 * @see #putData(byte[])
	 */
	public int bytesToPut() throws MathLinkException;

	/**
	 * Returns the number of bytes that remain to be read in the element that is currently
	 * being read in textual form.
	 * <p>
	 * Lets you keep track of your progress reading an element through a series
	 * of getData() calls.
	 *
	 * @return the number of bytes that remain to be read in the current element
	 * @exception MathLinkException
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #getData(int)
	 */
	public int bytesToGet() throws MathLinkException;

	/** Used for sending elements in textual form. After calling putNext() and putSize(), a series
	 * of putData() calls are used to send the actual data.
	 * <p>
	 * The so-called &quot;textual&quot; means of sending data is rarely used. Its main use is to allow
	 * a very large string to be sent, where the string data is not held in a single String
	 * object. The most important use of this technique in the C-language MathLink API was
	 * to send integers and reals that were too large to fit into an int or double. This use
	 * is unnecessary in J/Link, since Java has BigInteger and BigDecimal classes, and these
	 * objects can be sent directly with put().
	 *
	 * @param data the textual representation of the data
	 * @exception MathLinkException
	 * @see #putNext(int)
	 * @see #putSize(int)
	 */
	public void putData(byte[] data) throws MathLinkException;

	/** An alternative form of putData() that lets you specify how many bytes from the array should
	 * be sent, rather than just sending the whole thing.
	 *
	 * @param data
	 * @param len
	 * @exception MathLinkException
	 * @see #putData(byte[])
	 */
	public void putData(byte[] data, int len) throws MathLinkException;

	/** Gets a specified number of bytes in the textual form of the expression currently being read.
	 * The returned array will have a length of at most len.
	 * <p>
	 * You can use bytesToGet() to determine if more getData() calls are needed to completely read
	 * the element.
	 *
	 * @param len the maximum number of bytes to read
	 * @exception MathLinkException
	 * @return the data read
	 * @see #bytesToGet()
	 * @see #getNext()
	 * @see #getType()
	 */
	public byte[] getData(int len) throws MathLinkException;

	/** Reads a Mathematica character string.
	 * <p>
	 * Because both Java and Mathematica strings are in Unicode, the read string is an exact
	 * match to its Mathematica representation.
	 *
	 * @exception MathLinkException
	 * @return the string that was read
	 */
	public String getString() throws MathLinkException;

	/** Reads a Mathematica string as an array of bytes.
	 * <p>
	 * In contrast with getString(), this method strips the incoming (16-bit Unicode) character
	 * data into single-byte representation. Characters that cannot be represented faithfully
	 * in single-byte form are replaced by the byte specified by the <code>missing</code>
	 * parameter. This method is primarily useful if you know the incoming data contains only
	 * ASCII characters and you want the data in the form of a byte array.
	 *
	 * @param missing the byte to replace non-ASCII characters with
	 * @exception MathLinkException
	 * @return the data that was read
	 * @see #getString()
	 */
	public byte[] getByteString(int missing) throws MathLinkException;

	/** Sends an array of bytes to Mathematica as a string.
	 * <p>
	 * Use this instead of put() if you have string data in the form of a byte array.
	 * In the C-language MathLink API, MLPutByteString was useful because the more obviously-named
	 * MLPutString required a specially-encoded string. Because Java strings and Mathematica strings
	 * are both Unicode, put() works in J/Link without requiring special encoding. Thus,
	 * putByteString() has little use.
	 *
	 * @param data the string data to send
	 * @exception MathLinkException
	 * @see #put(Object)
	 */
	public void putByteString(byte[] data) throws MathLinkException;

	/** Reads a Mathematica symbol as a string.
	 * <p>
	 * Because Java strings and Mathematica symbols are in Unicode, the read string is an exact
	 * match to its Mathematica representation.
	 *
	 * @exception MathLinkException
	 * @return the symbol
	 * @see #getString()
	 */
	public String getSymbol() throws MathLinkException;

	/** Sends a symbol name.
	 * <p>
	 * Both Java strings and Mathematica symbols are in Unicode, so you can send symbols with the full
	 * Unicode character set.
	 *
	 * @param s the symbol name
	 * @exception MathLinkException
	 */
	public void putSymbol(String s) throws MathLinkException;

	/** Reads the Mathematica symbols True or False as a boolean. More precisely, it returns true if
	 * the symbol True is read, and false if False (or any other non-True symbol) is read.
	 * If you want to make sure that either True or False is on the link, don't use getBoolean();
	 * instead, read the symbol with getSymbol() and test its value yourself.
	 *
	 * @exception MathLinkException
	 * @return the boolean read
	 */
	public boolean getBoolean() throws MathLinkException;

	/** Sends the boolean value as the Mathematica symbol True or False.
	 *
	 * @exception MathLinkException
	 * @param b the value to send
	 */
	public void put(boolean b) throws MathLinkException;

	/** Reads a Mathematica integer as an int.
	 *
	 * @exception MathLinkException
	 * @return the integer read
	 */
	public int getInteger() throws MathLinkException;

	/** Sends an integer value.
	 *
	 * @param i the int to send
	 * @exception MathLinkException
	 */
	public void put(int i) throws MathLinkException;

	/** Reads a Mathematica integer as a long.
	 *
	 * @exception MathLinkException
	 * @return the long that was read
	 */
	public long getLongInteger() throws MathLinkException;

	/** Sends a long value.
	 *
	 * @param i the value to send
	 * @exception MathLinkException
	 */
	public void put(long i) throws MathLinkException;

	/** Reads a Mathematica real number or integer as a double.
	 *
	 * @exception MathLinkException
	 * @return the double value read
	 */
	public double getDouble() throws MathLinkException;

	/** Sends a double value.
	 *
	 * @param d the double to send
	 * @exception MathLinkException
	 */
	public void put(double d) throws MathLinkException;

	/** Reads a list as a one-dimensional array of booleans.
	 * <p>
	 * The expression being read must be a list or other depth-1 expression of the symbols True and False.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1.
	 * <p>
	 * The expression does not need to have head List.
	 * In other words, it could be List[False, True] or
	 * Foo[True, True]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a depth-1 array of True and False
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public boolean[] getBooleanArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of booleans.
	 * <p>
	 * The expression being read must be a matrix of the symbols True and False.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[True, False], List[True, True]] or
	 * Foo[Bar[True, False], Bar[True, True]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a 2-deep array of True and False
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public boolean[][] getBooleanArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of bytes.
	 * <p>
	 * The expression being read must be a list or deeper array of integers. Values outside
	 * the range of a byte are converted via casting.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public byte[] getByteArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of bytes.
	 * <p>
	 * The expression being read must be a matrix or deeper array of integers. Values outside
	 * the range of a byte are converted via casting.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public byte[][] getByteArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of chars.
	 * <p>
	 * The expression being read must be a list or deeper array of integers. Values outside
	 * the range of a char are converted via casting.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public char[] getCharArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of chars.
	 * <p>
	 * The expression being read must be a matrix or deeper array of integers. Values outside
	 * the range of a char are converted via casting.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public char[][] getCharArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of shorts.
	 * <p>
	 * The expression being read must be a list or deeper array of integers. Values outside
	 * the range of a short are converted via casting.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public short[] getShortArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of shorts.
	 * <p>
	 * The expression being read must be a matrix or deeper array of integers. Values outside
	 * the range of a short are converted via casting.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public short[][] getShortArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of ints.
	 * <p>
	 * The expression being read must be a list or deeper array of integers.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public int[] getIntArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of ints.
	 * <p>
	 * The expression being read must be a matrix or deeper array of integers.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public int[][] getIntArray2() throws MathLinkException;

	/** Reads a list as a one-dimensional array of longs.
	 * <p>
	 * The expression being read must be a list or other depth-1 expression of integers.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1.
	 * <p>
	 * The expression does not need to have head List. In other words, it could be List[1, 2] or Foo[1, 2].
	 * The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a depth-1 array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public long[] getLongArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of longs.
	 * <p>
	 * The expression being read must be a matrix of integers.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1, 2], List[3, 4]] or
	 * Foo[Bar[1, 2], Bar[3, 4]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a depth-2 array of integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public long[][] getLongArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of floats.
	 * <p>
	 * The expression being read must be a list or deeper array of real numbers or integers.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1., 2.], List[3., 4.]] or
	 * Foo[Bar[1., 2.], Bar[3., 4.]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of reals or integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public float[] getFloatArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of floats.
	 * <p>
	 * The expression being read must be a matrix or deeper array of real numbers or integers.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1., 2.], List[3., 4.]] or
	 * Foo[Bar[1., 2.], Bar[3., 4.]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an of array reals or integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public float[][] getFloatArray2() throws MathLinkException;

	/** Reads a list or array as a one-dimensional array of doubles.
	 * <p>
	 * The expression being read must be a list or deeper array of real numbers or integers.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1. If the arriving expression
	 * has depth greater than 1 (e.g., it is a matrix), it will be flattened to a 1-dimensional
	 * array.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1., 2.], List[3., 4.]] or
	 * Foo[Bar[1., 2.], Bar[3., 4.]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of reals or integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public double[] getDoubleArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of doubles.
	 * <p>
	 * The expression being read must be a matrix or deeper array of real numbers or integers.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2. If
	 * the arriving expression has depth greater than 2, it will be flattened to a
	 * 2-dimensional array. If the arriving expression has a depth less than 2 (i.e., it is
	 * a flat list), a MathLinkException will be thrown.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1., 2.], List[3., 4.]] or
	 * Foo[Bar[1., 2.], Bar[3., 4.]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not an array of reals or integers
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public double[][] getDoubleArray2() throws MathLinkException;

	/** Reads a list as a one-dimensional array of strings.
	 * <p>
	 * The expression being read must be a list or other depth-1 expression of strings or symbols.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1.
	 * <p>
	 * The expression does not need to have head List.
	 * In other words, it could be List[&quot;A&quot;, &quot;B&quot;] or
	 * Foo[&quot;A&quot;, &quot;B&quot;]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a depth-1 array of strings or symbols
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public String[] getStringArray1() throws MathLinkException;

	/** Reads an array as a two-dimensional array of strings.
	 * <p>
	 * The expression being read must be a matrix of strings or symbols.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[&quot;A&quot;, &quot;B&quot;], List[&quot;C&quot;, D&quot;]] or
	 * Foo[Bar[&quot;A&quot;, &quot;B&quot;], Bar[&quot;C&quot;, &quot;D&quot;]]. The
	 * information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a matrix of strings or symbols
	 * @return the array read
	 * @see #getArray(int, int)
	 */
	public String[][] getStringArray2() throws MathLinkException;

	/** Reads a list as a one-dimensional array of complex numbers.
	 * <p>
	 * The expression being read must be a list or other depth-1 expression of Complex.
	 * The &quot;1&quot; suffix in the method name indicates that the returned array has depth 1.
	 * <p>
	 * This method will read the expression, but return null, if no Java class has yet been
	 * established to use for complex numbers by calling setComplexClass.
	 * <p>
	 * The expression does not need to have head List.
	 * In other words, it could be List[1+I, 2-I] or
	 * Foo[1+I, 2-I]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a depth-1 array of Complex
	 * @return the array read, or null if no Java class has been declared for complex numbers via setComplexClass()
	 * @see #setComplexClass(Class)
	 * @see #getArray(int, int)
	 */
	public Object[] getComplexArray1() throws MathLinkException;

	/** Reads a matrix as a two-dimensional array of complex numbers.
	 * <p>
	 * The expression being read must be a matrix of Complex.
	 * The &quot;2&quot; suffix in the method name indicates that the returned array has depth 2.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth.
	 * In other words, it could be List[List[1+I, 2-I], List[3+I, 4-I]] or
	 * Foo[Bar[1+I, 2-I], Bar[3+I, 4-I]]. The information about the heads is lost;
	 * if you need this information you will need to either use getArray(int, int, String[]) or read
	 * the expression as an Expr and examine it using the Expr methods.
	 *
	 * @exception MathLinkException if the incoming expression is not a matrix of Complex
	 * @return the array read, or null if no Java class has been declared for complex numbers via setComplexClass()
	 * @see #setComplexClass(Class)
	 * @see #getArray(int, int)
	 */
	public Object[][] getComplexArray2() throws MathLinkException;

	/**
	 * Reads an array of the specified type and depth.
	 * <p>
	 * The getXXXArrayN methods are all just convenience methods that call this method.
	 * Use this method instead when you want to read an array of dimensionality deeper than 2.
	 * Cast the result of this method to the desired array type, as in:
	 * <pre>
	 *     int[][][] result = (int[][][]) ml.getArray(TYPE_INT, 3); </pre>
	 * For the types TYPE_BYTE, TYPE_CHAR, TYPE_SHORT, TYPE_INT, TYPE_FLOAT, and TYPE_DOUBLE,
	 * if the expression has depth greater than <code>depth</code>, it will be flattened to a
	 * Java array of dimension <code>depth</code>. If the expression has a depth less than
	 * <code>depth</code>, a MathLinkException will be thrown.
	 * <p>
	 * The maximum depth supported is 5.
	 * <p>
	 * The expression does not need to have head List. It can have any heads, at any depth,
	 * and the heads do not have to agree at each depth, meaning that you could read the
	 * expression {foo[1, 2], bar[3, 4]} as an int[][].
	 * The information about the heads is lost; if you need this information you can use
	 * the alternative signature getArray(int type, int depth, String[] heads), or
	 * read the expression as an Expr and examine it using the Expr methods.
	 *
	 * @param type The desired type for the returned array. Must be one of TYPE_BOOLEAN, TYPE_BYTE,
	 * TYPE_CHAR, TYPE_SHORT, TYPE_INT, TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE, TYPE_STRING,
	 * TYPE_BIGINTEGER, TYPE_BIGDECIMAL, TYPE_EXPR, or TYPE_COMPLEX.
	 * @param depth the depth of the returned array
	 * @return the array read, or null if type is TYPE_COMPLEX and no Java class has
	 * been declared for complex numbers via setComplexClass()
	 * @exception MathLinkException if the incoming expression is not an array of the requested type
	 * or is not as deep as requested
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #getArray(int, int, String[])
	 */
	public Object getArray(int type, int depth) throws MathLinkException;

	/**
	 * Reads an array of the specified type and depth and records the heads at each level.
	 * <p>
	 * Identical to getArray(int type, int depth), except that this form fills a String array
	 * with the heads of the expression found at each level. To use this method, allocate a
	 * String array of length 'depth' and pass it in. For example:
	 * <pre>
	 * // Assume that the expression {foo[1, 2], foo[3, 4]} is waiting on the link.
	 * String[] heads = new String[2];
	 * int[][] intArray2D = (int[][]) ml.getArray(MathLink.TYPE_INT, 2, heads);
	 * // Now heads[0] is "List" and heads[1] is "foo".</pre>
	 *
	 * This method does not enforce a requirement that the heads be identical across a level.
	 * If the expression looks like {foo[1, 2], bar[3, 4]} then the heads array would become
	 * {"List", "foo"}, ignoring the fact that foo was not the head of every subexpression at level
	 * 1. In other words, if heads[i] is "foo", then it is only guaranteed that <i>some</i> expression
	 * at level i had head foo, not that <i>all</i> of them did. If you want to be absolutely sure about
	 * the heads of every subpart, read the expression as an Expr and use the Expr methods to inspect it.
	 *
	 * @param type The desired type for the returned array. Must be one of TYPE_BOOLEAN, TYPE_BYTE,
	 * TYPE_CHAR, TYPE_SHORT, TYPE_INT, TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE, TYPE_STRING,
	 * TYPE_BIGINTEGER, TYPE_BIGDECIMAL, TYPE_EXPR, or TYPE_COMPLEX.
	 * @param depth the depth of the returned array
	 * @param heads an array of length 'depth' that will be filled with the heads at each level
	 * @return the array read, or null if type is TYPE_COMPLEX and no Java class has
	 * been declared for complex numbers via setComplexClass()
	 * @exception MathLinkException if the incoming expression is not an array of the requested type,
	 * or is not as deep as requested
	 * @see #getArray(int, int)
	 * @since 2.0
	 */
	public Object getArray(int type, int depth, String[] heads) throws MathLinkException;

	/**
	 * Reads a function name and argument count.
	 *
	 * @return an object describing the head and argument count of the function
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public MLFunction getFunction() throws MathLinkException;

	/**
	 * Sends a function name and argument count.
	 * <p>
	 * Follow this with calls to put the arguments.
	 *
	 * @param f the function name
	 * @param argCount the number of arguments to follow
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public void putFunction(String f, int argCount) throws MathLinkException;

	/**
	 * Reads a function name and argument count and compares the function name with <code>f</code>.
	 * If they do not match, a MathLinkException is thrown.
	 * <p>
	 * Similar to getFunction() in that it reads the name and argument count off the link.
	 * Use it in situations where you want an error to occur if the function is not what
	 * you expect.
	 *
	 * @param f the function name that you are expecting
	 * @return the argument count
	 * @exception com.wolfram.jlink.MathLinkException if the current expression is not a function, or if it does not have the specified name
	 * @see #checkFunctionWithArgCount(String, int)
	 */
	public int checkFunction(String f) throws MathLinkException;

	/**
	 * Reads a function name and argument count and compares the function name with
	 * <code>f</code> and the argument count with <code>argCount</code>.
	 * If they do not match, a MathLinkException is thrown.
	 * <p>
	 * Similar to getFunction() in that it reads the name and argument count off the link.
	 * Use it in situations where you want an error to occur if the function is not what
	 * you expect.
	 *
	 * @param f the function name that you are expecting
	 * @param argCount the argument count that you are expecting
	 * @exception com.wolfram.jlink.MathLinkException if the current expression is not a function, or if it does not have the specified name
	 * @see #checkFunction(String)
	 */
	public void checkFunctionWithArgCount(String f, int argCount) throws MathLinkException;

	/**
	 * Reads a complex number. This can be an integer, real, or a Mathematica expression
	 * with head Complex. You must first specify a Java class for complex numbers using setComplexClass().
	 * <p>
	 * This method will read the number, but return null, if no Java class has yet been
	 * established to use for complex numbers.
	 *
	 * @return an object of the class you have specified to map to Mathematica Complex numbers,
	 * or null if you have not specified any class (using setComplexClass()).
	 * @exception com.wolfram.jlink.MathLinkException if the current expression is not an integer, real, or Mathematica Complex.
	 * @see #setComplexClass(Class)
	 */
	public Object getComplex() throws MathLinkException;

	/**
	 * Writes a complete expression from the link <code>source</code> to this link.
	 *
	 * @param source the link to read from
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #transferToEndOfLoopbackLink(LoopbackLink)
	 */
	public void transferExpression(MathLink source) throws MathLinkException;

	/**
	 * Writes the entire contents of the LoopbackLink <code>source</code> to this link.
	 *
	 * @param source the LoopbackLink to read from
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #transferExpression(MathLink)
	 */
	public void transferToEndOfLoopbackLink(LoopbackLink source) throws MathLinkException;

	/**
	 * Reads an expression from the link and creates an Expr from it.
	 * <p>
	 * The returned Expr can be examined and manipulated later.
	 *
	 * @return the Expr read
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #peekExpr()
	 */
	public Expr getExpr() throws MathLinkException;

	/**
	 * Creates an Expr from the current expression, but does not drain it off the link.
	 * <p>
	 * Like getExpr(), but peekExpr() does not actually remove anything from the link. In other
	 * words, it leaves the link in the same state it was in before peekExpr() was called.
	 *
	 * @return the Expr read
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #getExpr()
	 */
	public Expr peekExpr() throws MathLinkException;

	/**
	 * Reads a low-level MathLink message.
	 * <p>
	 * This method no longer functions in J/Link 2.0. You must use addMessageHandler() if
	 * you want to be able to receive messages from Mathematica.
	 *
	 * @return the message read; will most likely be one of MLINTERRUPTMESSAGE or MLABORTMESSAGE
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #addMessageHandler(Class, Object, String)
	 * @deprecated
	 */
	public int getMessage() throws MathLinkException;

	/**
	 * Sends a low-level MathLink message.
	 * <p>
	 * To abort a Mathematica computation, use putMessage(MLABORTMESSAGE). If a
	 * computation is successfully aborted, it will return the symbol $Aborted.
	 * <p>
	 * Do not confuse this type of message, used mainly for communicating requests to
	 * interrupt or abort computations, with Mathematica warning messages, which are
	 * unrelated.
	 *
	 * @param msg the message to send; will be one of MLABORTMESSAGE or MLINTERRUPTMESSAGE. Most users
	 * will only be concerned with MLABORTMESSAGE.
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public void putMessage(int msg) throws MathLinkException;

	/**
	 * Tells whether a low-level MathLink message has arrived.
	 * <p>
	 * This method no longer functions in J/Link 2.0. You must use addMessageHandler() if
	 * you want to be able to receive messages from Mathematica.
	 *
	 * @return true if a message is waiting to be read with getMessage(); false otherwise
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #addMessageHandler(Class, Object, String)
	 * @deprecated
	 */
	public boolean messageReady() throws MathLinkException;

	/**
	 * Creates a mark at the current point in the incoming MathLink data stream.
	 * <p>
	 * Marks can returned to later, to re-read data. A common use is to create a mark,
	 * call some method for reading data, and if a MathLinkException is thrown, seek back
	 * to the mark and try a different method of reading the data.
	 * <p>
	 * Make sure to always call destroyMark() on any marks you create. Failure
	 * to do so will cause a memory leak.
	 * <p>
	 * Some of the usefulness of marks in the C-language MathLink API is obviated by
	 * J/Link's Expr class.
	 *
	 * @return the mark. You cannot do anything with this value except use it
	 * in seekMark() or destroyMark(); it has no meaning as a number.
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #seekMark(long)
	 * @see #destroyMark(long)
	 */
	public long createMark() throws MathLinkException;

	/**
	 * Resets the current position in the incoming MathLink data stream to an earlier point.
	 *
	 * @param mark the mark, created by createMark(), that identifies the desired position to reset to
	 * @see #createMark()
	 * @see #seekMark(long)
	 */
	public void seekMark(long mark);
	/**
	 * Destroys a mark. Always call destroyMark() on any marks you create with createMark().
	 *
	 * @param mark the mark to destroy
	 */
	public void destroyMark(long mark);

	/**
	 * Sends an object, including strings and arrays.
	 * <p>
	 * Only a limited set of Java objects can be usefully sent across a link with
	 * this method. These are objects whose &quot;values&quot; have a meaningful
	 * representation in Mathematica:
	 * <pre>
	 * null: sent as the symbol Null.
	 * strings: sent as Mathematica strings
	 * arrays: sent as lists of the appropriate dimensions
	 * Expr: sent as expressions
	 * Wrapper classes (e.g., Boolean, Integer, Float, etc.): sent as their values
	 * BigDecimal and BigInteger: sent as their values
	 * complex numbers: sent as Complex</pre>
	 *
	 * All other objects have no meaningful representation in Mathematica. For these
	 * objects, the relatively useless obj.toString() is sent. The put(Object) method in
	 * the KernelLink interface will put these objects &quot;by reference&quot;.
	 *
	 * @param obj the object to put
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public void put(Object obj) throws MathLinkException;

	/**
	 * Sends an array object. Unlike put(Object), this method lets you specify
	 * the heads you want for each dimension.
	 * <pre>
	 *     int[][] a = {{1,2},{3,4}};
	 *     // The following are equivalent, sending to Mathematica the matrix: {{1,2},{3,4}}
	 *     ml.put(a);
	 *     ml.put(a, null);
	 *     ml.put(a, new String[] {&quot;List&quot;, &quot;List&quot;});</pre>
	 *
	 * @param obj must be an array
	 * @param heads the heads at each dimension
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public void put(Object obj, String[] heads) throws MathLinkException;

	/**
	 * Specifies the class you want to map to Mathematica's Complex numbers.
	 * <p>
	 * After calling setComplexClass(), you can use getComplex() to read an incoming Complex
	 * and create an instance of your class, and you can use put() to send objects of your class
	 * to Mathematica as Complex.
	 * <p>
	 * To be suitable, your class must have a public constructor of the form
	 * <code>MyComplex(double re, double im)</code>, and it must have two public methods
	 * <code>double re()</code> and <code>double im()</code>.
	 *
	 * @param cls the class to map to Mathematica Complex numbers
	 * @return true, if the class is a suitable choice, false otherwise
	 * @see #getComplexClass()
	 * @see #getComplex()
	 */
	public boolean setComplexClass(Class cls);

	/**
	 * Gives the Java class that you have specified to map to Mathematica's Complex numbers.
	 * <p>
	 * You specify the class you want using setComplexClass().
	 *
	 * @return the class, or null if no class has yet been specified
	 * @see #setComplexClass(Class)
	 */
	public Class getComplexClass();

	/**
	 * Sets the Java method you want called as a yield function.
	 * <p>
	 * The method must be public and its signature must be (V)Z (e.g., <code>public boolean foo()</code>).
	 * You can pass null for <code>cls</code> if <code>obj</code> is provided.
	 * If the method is static, pass null as <code>obj</code>.
	 * <p>
	 * Yield functions are an advanced topic, and are discussed in greater detail
	 * in the User Guide. Few users will need to use one.
	 *
	 * @param cls the class in which the method resides
	 * @param obj the object on which the method should be invoked (use null if the method is static)
	 * @param methName the name of the method to invoke
	 * @return true, if the specified method is suitable (i.e., could be found, and has the
	 * correct signature); false otherwise
	 */
	public boolean setYieldFunction(Class cls, Object obj, String methName);

	/**
	 * Specifies the Java method you want called as a message handler.
	 * <p>
	 * Do not confuse this type of message, used mainly for communicating requests to
	 * interrupt or abort a computation, with Mathematica warning and error messages, which are
	 * unrelated.
	 * <p>
	 * The method you specify will be added to the set that are called whenever a MathLink
	 * message is received by your Java program. The method must be public and its signature must
	 * be (II)V (e.g., <code>public void foo(int msgType, int ignore)</code>). You can pass null for
	 * <code>cls</code> if <code>obj</code> is provided. If the method is static, pass null
	 * for <code>obj</code>. The first argument passed to your function when it is called is
	 * the integer code giving the message type (e.g., MLABORTMESSAGE, MLINTERRUPTMESSAGE, etc.)
	 * The second argument is undocumented and should be ignored.
	 * <p>
	 * Do not attempt to use the link from within your message handler function.
	 * <p>
	 * You can set more than one message handler, hence the name &quot;addMessageHandler&quot;.
	 * <p>
	 * Message handlers are an advanced technique. Few programmers will need to use one.
	 *
	 * @param cls the class in which the method resides
	 * @param obj the object on which the method should be invoked (use null if the method is static)
	 * @param methName the name of the method to invoke
	 * @return true, if the specified method is suitable (i.e., could be found, and has the
	 * correct signature); false otherwise
	 * @see #removeMessageHandler(String)
	 */
	public boolean addMessageHandler(Class cls, Object obj, String methName);

	/**
	 * Removes a message handler you previously set up with addMessageHandler.
	 * <p>
	 * Do not confuse this type of message, used mainly for communicating requests to
	 * interrupt or abort a computation, with Mathematica warning and error messages, which are
	 * unrelated.
	 * <p>
	 * Message handlers are an advanced topic, and are discussed in greater detail
	 * in the User Guide. Few users will need to use one.
	 *
	 * @param methName the name of the method to remove
	 * @return true, if the specified method was previously set as a message handler; false otherwise
	 * @see #addMessageHandler(Class, Object, String)
	 */
	public boolean removeMessageHandler(String methName);

}
