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

import com.alibaba.fastjson.JSONArray;

/**
 * KernelLink is the main link interface that J/Link programmers will use. The MathLink
 * interface contains the low-level methods for reading and writing data. KernelLink
 * extends the MathLink interface, adding some higher-level methods that are
 * appropriate only if the program on the other side of the link is a Mathematica kernel.
 * An example is the discardAnswer() method, which reads and discards the sequence of
 * packets the kernel will send in the course of a single evaluation.
 * <p>
 * Objects of type KernelLink are created by the createKernelLink() method in the
 * MathLinkFactory class.
 *
 * @see MathLink
 * @see MathLinkFactory
 */


public interface KernelLink extends MathLink {

	/**
	 * The version string identifying this release.
	 */
	String VERSION = "4.9.1";

	/**
	 * The major version number identifying this release.
	 */
	double VERSION_NUMBER = 1.0 * 4.9;

	// The one and only place that must be changed if I change the package context for the
	// supporting .m file.
	/**
	 * The Mathematica package context for the J/Link support functions. Using this is preferable to
	 * hard-coding "JLink`" in your code if you need to explicitly load the J/Link package file.
	 * <pre>
	 *     BAD:  ml.evaluate("Needs[\"JLink`\"]");
	 *     GOOD: ml.evaluate("Needs[\" + KernelLink.PACKAGE_CONTEXT + \"]");</pre>
	 */
	String PACKAGE_CONTEXT = "JLink`";

    /**
     * Returned by the KernelLink methods getNext() and getType() to represent that
     * a Java object is waiting to be read from the link. This is an extension to
     * the MLTKxxx constants in the MathLink interface.
     */
    int MLTKOBJECT = 100000;

    /**
     * This is an extension to the TYPE_xxx constants in the MathLink interface used in getArray().
     */
    int TYPE_OBJECT = -14;

	/**
	 * Error code in a MathLinkException when getObject() was called and a valid Java
	 * object was not on the link.
	 */
	int MLE_BAD_OBJECT = MLEUSER + 100;


	/**
	 * Sends a string for evaluation.
	 * <p>
	 * This method only sends the computation--it does not read any resulting packets
	 * off the link. You would typically follow a call to evaluate() with either
	 * waitForAnswer() or discardAnswer(), which read data from the link.
	 *
	 * @param s the string of Mathematica code
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #waitForAnswer()
	 * @see #discardAnswer()
	 */

	void evaluate(String s) throws MathLinkException;

	/**
	 * Sends an Expr for evaluation.
	 * <p>
	 * This method only sends the computation--it does not read any resulting packets
	 * off the link. You would typically follow a call to evaluate() with either
	 * waitForAnswer() or discardAnswer(), which read data from the link.
	 *
	 * @param e the Expr to evaluate
	 * @exception com.wolfram.jlink.MathLinkException
	 */

	void evaluate(Expr e) throws MathLinkException;

	/**
	 * Sends a string to evaluate, then reads and discards all output except for the
	 * result, which is returned. The result comes back in Mathematica InputForm, formatted
	 * to the specified page width.
	 *
	 * @param s the string to evaluate
	 * @param pageWidth the page width, in characters, to format the output
	 * @return the answer in InputForm, or null if a MathLinkException occurred
	 */

	String evaluateToInputForm(String s, int pageWidth);

	/**
	 * Sends an Expr to evaluate, then reads and discards all output except for the
	 * result, which is returned. The result comes back in Mathematica InputForm, formatted
	 * to the specified page width.
	 *
	 * @param e the Expr to evaluate
	 * @param pageWidth the page width, in characters, to format the output. Pass 0 for Infinity
	 * @return the answer in InputForm, or null if a MathLinkException occurred
	 * @see #getLastError()
	 */

	String evaluateToInputForm(Expr e, int pageWidth);

	/**
	 * Sends a string to evaluate, then reads and discards all output except for the
	 * result, which is returned. The result comes back in Mathematica OutputForm, formatted
	 * to the specified page width.
	 *
	 * @param s the string to evaluate
	 * @param pageWidth the page width, in characters, to format the output. Pass 0 for Infinity
	 * @return the answer in InputForm, or null if a MathLinkException occurred
	 * @see #getLastError()
	 */

	String evaluateToOutputForm(String s, int pageWidth);

	/**
	 * Sends an Expr to evaluate, then reads and discards all output except for the
	 * result, which is returned. The result comes back in Mathematica OutputForm, formatted
	 * to the specified page width.
	 *
	 * @param e the Expr to evaluate
	 * @param pageWidth the page width, in characters, to format the output. Pass 0 for Infinity
	 * @return the answer in InputForm, or null if a MathLinkException occurred
	 * @see #getLastError()
	 */

	String evaluateToOutputForm(Expr e, int pageWidth);

	/**
	 * Sends graphics or plotting code to evaluate, then reads and discards all output
	 * except for the image data, which is returned as a byte array of GIF data.
	 * The image will be sized to just fit within a box of <code>width</code> X
	 * <code>height</code>, without changing its aspect ratio. In other words, the image
	 * might not have exactly these dimensions, but it will never be larger.
	 * <p>
	 * To get image data returned in JPEG format instead of GIF, set the Mathematica symbol
	 * JLink`$DefaultImageFormat = "JPEG".
	 * <p>
	 * If s does not evaluate to a graphics expression, then null is returned. It is not enough
	 * that the computation causes a plot to be generated--the <i>return</i> value of the
	 * computation must be a Mathematica Graphics (or Graphics3D, SurfaceGraphics, etc.)
	 * expression. For example:
	 * <pre>
	 *     BAD:  ml.evaluateToImage("Plot[x,{x,0,1}]; 42");
	 *     GOOD: ml.evaluateToImage("Plot[x,{x,0,1}]");</pre>
	 *
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToImage(someInput, 0, 0);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param s the string to evaluate
	 * @param width the image width, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the width
	 * @param height the image height, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the height
	 * @return the GIF data, or null if s does not evaluate to a graphics expression
	 * or if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToImage(String s, int width, int height);

	/**
	 * Sends graphics or plotting code to evaluate, then reads and discards all output
	 * except for the image data, which is returned as a byte array of GIF data.
	 * The image will be sized to just fit within a box of <code>width</code> X
	 * <code>height</code>, without changing its aspect ratio. In other words, the image
	 * might not have exactly these dimensions, but it will never be larger.
	 * <p>
	 * To get image data returned in JPEG format instead of GIF, set the Mathematica symbol
	 * JLink`$DefaultImageFormat = "JPEG".
	 * <p>
	 * If e does not evaluate to a graphics expression, then null is returned. It is not enough
	 * that the computation causes a plot to be generated--the <i>return</i> value of the
	 * computation must be a Mathematica Graphics (or Graphics3D, SurfaceGraphics, etc.)
	 * expression.
	 * <p>
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToImage(someInput, 0, 0);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param e the Expr to evaluate
	 * @param width the image width, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the width
	 * @param height the image height, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the height
	 * @return the GIF data, or null if e does not evaluate to a graphics expression
	 * or if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToImage(Expr e, int width, int height);

	/**
	 * Sends graphics or plotting code to evaluate, then reads and discards all output
	 * except for the image data, which is returned as a byte array of GIF data.
	 * The image will be sized to just fit within a box of <code>width</code> X
	 * <code>height</code>, without changing its aspect ratio. In other words, the image
	 * might not have exactly these dimensions, but it will never be larger. The image
	 * resolution, in dots per inch, is specified by <code>dpi</code>. The <code>useFE</code>
	 * parameter controls whether the notebook front end will be used for rendering
	 * services. If it is used, the front end will be run on the same machine as the kernel.
	 * <p>
	 * To get image data returned in JPEG format instead of GIF, set the Mathematica symbol
	 * JLink`$DefaultImageFormat = "JPEG".
	 * <p>
	 * If s does not evaluate to a graphics expression, then null is returned. It is not enough
	 * that the computation causes a plot to be generated--the <i>return</i> value of the
	 * computation must be a Mathematica Graphics (or Graphics3D, SurfaceGraphics, etc.)
	 * expression. For example:
	 * <pre>
	 *     BAD:  ml.evaluateToImage("Plot[x,{x,0,1}]; 42");
	 *     GOOD: ml.evaluateToImage("Plot[x,{x,0,1}]");</pre>
	 *
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToImage(someInput, 0, 0, 0, false);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param s the string to evaluate
	 * @param width the image width, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the width
	 * @param height the image height, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the height
	 * @param dpi the resolution, in dots (pixels) per inch. Pass 0 for Automatic, or if the expression itself
	 * specifies the resolution
	 * @param useFE whether to use the notebook front end for rendering services
	 * @return the GIF data, or null if s does not evaluate to a graphics expression
	 * or if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToImage(String s, int width, int height, int dpi, boolean useFE);

	/**
	 * Sends graphics or plotting code to evaluate, then reads and discards all output
	 * except for the image data, which is returned as a byte array of GIF data.
	 * The image will be sized to just fit within a box of <code>width</code> X
	 * <code>height</code>, without changing its aspect ratio. In other words, the image
	 * might not have exactly these dimensions, but it will never be larger. The image
	 * resolution, in dots per inch, is specified by <code>dpi</code>. The <code>useFE</code>
	 * parameter controls whether the notebook front end will be used for rendering
	 * services. If it is used, the front end will be run on the same machine as the kernel.
	 * <p>
	 * To get image data returned in JPEG format instead of GIF, set the Mathematica symbol
	 * JLink`$DefaultImageFormat = "JPEG".
	 * <p>
	 * If s does not evaluate to a graphics expression, then null is returned. It is not enough
	 * that the computation causes a plot to be generated--the <i>return</i> value of the
	 * computation must be a Mathematica Graphics (or Graphics3D, SurfaceGraphics, etc.)
	 * expression.
	 * <p>
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToImage(someInput, 0, 0, 0, false);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param e the Expr to evaluate
	 * @param width the image width, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the width
	 * @param height the image height, in pixels. Pass 0 for Automatic, or if the expression itself
	 * specifies the height
	 * @param dpi the resolution, in dots (pixels) per inch. Pass 0 for Automatic, or if the expression itself
	 * specifies the resolution
	 * @param useFE whether to use the notebook front end for rendering services
	 * @return the GIF data, or null if s does not evaluate to a graphics expression
	 * or if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToImage(Expr e, int width, int height, int dpi, boolean useFE);

	/**
	 * Sends a string to evaluate, the result of which will be typeset, converted to a GIF,
	 * and the resulting data returned. The result is typeset in StandardForm if
	 * <code>useStdForm</code> is true, and TraditionalForm otherwise.
	 * The result will be wrapped to <code>width</code> pixels during typesetting.
	 * The notebook front end will be used for rendering services; it will be run on the
	 * same machine as the kernel.
	 * <p>
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToTypeset(someInput, 0, true);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param s the string to evaluate
	 * @param width the width to wrap the output to during typesetting, in pixels
	 * (a rough measure). Pass 0 for Infinity
	 * @param useStdForm true for StandardForm, false for TraditionalForm
	 * @return the GIF data, or null if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToTypeset(String s, int width, boolean useStdForm);

	/**
	 * Sends an Expr to evaluate, the result of which will be typeset, converted to a GIF,
	 * and the resulting data returned. The result is typeset in StandardForm if
	 * <code>useStdForm</code> is true, and TraditionalForm otherwise.
	 * The result will be wrapped to <code>width</code> pixels during typesetting.
	 * The notebook front end will be used for rendering services; it will be run on the
	 * same machine as the kernel.
	 * <p>
	 * To create an Image from the data, you can do the following:
	 * <pre>
	 *     byte[] gifData = ml.evaluateToTypeset(someInput, 0, true);
	 *     java.awt.Image im = java.awt.Toolkit.getDefaultToolkit().createImage(gifData);</pre>
	 *
	 * @param e the Expr to evaluate
	 * @param width the width to wrap the output to during typesetting, in pixels
	 * (a rough measure). Pass 0 for Infinity
	 * @param useStdForm true for StandardForm, false for TraditionalForm
	 * @return the GIF data, or null if a MathLinkException occurred
	 * @see #getLastError()
	 */

	byte[] evaluateToTypeset(Expr e, int width, boolean useStdForm);


	/**
	 * Returns the Throwable object that represents any exception detected during the last call
	 * of one of the "evaluateTo" methods (evaluateToInputForm(), evaluateToOutputForm(),
	 * evaluateToImage(), evaluateToTypeset()). For convenience, those methods don't throw
	 * MathLinkException. Instead, they catch any such exceptions and simply return null if one occurred.
	 * Sometimes you want to see what the exact exception was when you get back a null result. The
	 * getLastError() method shows you that exception. Typically, it will be a MathLinkException,
	 * but there are some other rare cases (like an OutOfMemoryError if an image was returned that
	 * would have been too big to handle).
	 *
	 * @return the exception, or null if no exception occurred
	 */
	Throwable getLastError();


	/**
	 * Reads and discards all packets that arrive up until the packet that contains the
	 * result of the computation. It is then your responsibility to read the contents of
	 * the packet that holds the result.
	 * <p>
	 * Use this after sending an expression to evaluate with <code>evaluate()</code> or a manual
	 * sequence of put methods.
	 * <pre>
	 *     ml.evaluate("2+2");
	 *     ml.waitForAnwser();
	 *     int result = ml.getInteger();
	 *     // It is not strictly necessary to call newPacket, since we have read the entire
	 *     // packet contents, but it is good style.
	 *     ml.newPacket();</pre>
	 *
	 * If you are not interested in examining the result of the evaluation, use discardAnswer()
	 * instead.
	 * <p>
	 * Examples of packets that arrive before the result and are discarded are TEXTPKT,
	 * MESSAGEPKT, DISPLAYPKT, etc. If you want to examine or operate on the incoming packets
	 * that are discarded by this method, use the PacketListener interface.
	 * <p>
	 * It returns the packet type that held the result of the computation. Typically, this will
	 * be RETURNPKT. However, if you are manually sending evaluations inside an EnterTextPacket or
	 * EnterExpressionPacket, there are some further issues that you need to understand; consult
	 * the J/Link User Guide for details.
	 *
	 * @return the packet type that held the result, typically RETURNPKT
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #discardAnswer()
	 * @see #evaluate(String)
	 * @see #evaluate(Expr)
	 */

	int waitForAnswer() throws MathLinkException;

	/**
	 * Reads and discards all packets generated during a computation.
	 * <p>
	 * Use this after sending an expression to evaluate with <code>evaluate()</code> or a manual
	 * sequence of put methods.
	 * <p>
	 * If you are interested in examining the result of the evaluation, use waitForAnswer()
	 * instead.
	 * <p>
	 * If you want to examine or operate on the incoming packets
	 * that are discarded by this method, use the PacketListener interface.
	 *
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #waitForAnswer()
	 * @see #evaluate(String)
	 * @see #evaluate(Expr)
	 */

	void discardAnswer() throws MathLinkException;

	/**
	 * Call this to invoke J/Link's internal handling of special packet types.
	 * <p>
	 * If you absolutely must write your own packet loop instead of using the PacketListener
	 * interface (this is strongly discouraged), you should call this method if a call to
	 * nextPacket() returns a packet type that you are not handling entirely with your own code.
	 * In fact, you can call handlePacket() for <i>every</i> packet you read with nextPacket().
	 * For example, here is a basic packet loop:
	 * <pre>
	 *     boolean done = false;
	 *     while (!done) {
	 *          int pkt = ml.nextPacket();
	 *          if (ml.notifyPacketListeners(pkt))
	 *              ml.handlePacket(pkt);
	 *          switch (pkt) {
	 *              case MathLink.RETURNPKT:
	 *                  // read and handle contents of ReturnPacket ...
	 *                  done = true;
	 *                  break;
	 *              case MathLink.TEXTPKT:
	 *                  // read and handle contents of TextPacket ...
	 *                  break;
	 *              .. etc for other packet types
	 *          }
	 *          ml.newPacket();
	 *     }</pre>
	 *
	 * To remind again, writing your own packet loop like this is strongly discouraged. Use
	 * waitForAnswer(), discardAnswer(), or one of the "evaluateTo" methods instead. These
	 * methods hide the packet loop within them. If you want more information about what packet
	 * types arrive and their contents, simply use the PacketListener interface.
	 * <p>
	 * An example of the special type of packets that your packet loop might encounter is CALLPKT.
	 * Encountering a CALLPKT means that Mathematica code is trying to call into Java using the
	 * mechanism described in Part 1 of the J/Link User Guide. Only the internals of J/Link know
	 * how to manage these callbacks, so the handlePacket() method provides a means to invoke
	 * this handling for you.
	 * <p>
	 * If you are using waitForAnswer(), discardAnswer(), or any of the &quot;evaluateTo&quot;
	 * methods, and therefore not writing your own packet loop, you do not need to be concerned
	 * with handlePacket().
	 * <p>
	 * After handlePacket() returns you should call newPacket().
	 * <p>
	 *
	 * @see #waitForAnswer()
	 */

	void handlePacket(int pkt) throws MathLinkException;

	/**
	 * Sends an object.
	 * <p>
	 * Behaves like the put(Object) method in the MathLink interface for objects that
	 * have a meaningful &quot;value&quot; representation in Mathematica. This set is:
	 * <pre>
	 * null: sent as the symbol Null.
	 * strings: sent as Mathematica strings
	 * arrays: sent as lists of the appropriate dimensions
	 * Expr: sent as expressions
	 * Wrapper classes (e.g., Boolean, Integer, Float, etc.): sent as their values
	 * BigDecimal and BigInteger: sent as their values
	 * complex numbers: sent as Complex</pre>
	 *
	 * For other objects, behaves like putReference(). In other words, the KernelLink
	 * interface changes the semantics of put(Object) compared to the MathLink interface
	 * by adding the ability to send object references to Mathematica for objects whose values
	 * are not meaningful. You must call enableObjectReferences() before calling
	 * put(Object) on an object that will be sent by reference. See notes for putReference().
	 *
	 * @param obj the object to send
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #putReference(Object)
	 * @see #enableObjectReferences()
	 */

	void put(Object obj) throws MathLinkException;

	/**
	 * Sends an object to Mathematica &quot;by reference&quot;. You must call
	 * enableObjectReferences() before calling putReference().
	 * <p>
	 * Use this method to pass Java objects to Mathematica so that methods
	 * can be invoked directly from Mathematica code as described in Part 1
	 * of the J/Link User Guide.
	 * <p>
	 * The put() method will also put some objects by reference, but there are
	 * two reasons to call putReference() instead:
	 * <pre>
	 * (1) You want to force an object that would normally be sent by value
	 * (for example, a string) to be sent by reference.
	 * (2) You know that the object is a type that put() will send by
	 * reference. By calling putReference() directly, you can save the
	 * effort put() would expend figuring this out.</pre>
	 *
	 * @param obj the object to send
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #put(Object)
	 * @see #enableObjectReferences()
	 * @see #getObject()
	 */

	void putReference(Object obj) throws MathLinkException;

	/* NOT PUBLIC FOR NOW
	 * Sends an object to Mathematica &quot;by reference&quot; and specifies
	 * the class that the object will be seen as in Mathematica. This is an
	 * advanced variant of putReference(Object) that very few programmers will
	 * need to use. The class you specify must be a parent class or interface
	 * of the object. This is, in effect, an "upcast" wherein you send
	 * an object to Mathematica but specify that it should be typed as a
	 * parent class or interface.
	 * <p>
	 * The reasons you might want to do this are ...
	 * <p>
	 *
	 * @param obj the object to send
	 * @param upCastCls the class that the object will be seen as in Mathematica
	 * @exception com.wolfram.jlink.MathLinkException
	 * @since 2.0
	 * @see #putReference(Object)
	 * @see #enableObjectReferences()

	void putReference(Object obj, Class upCastCls) throws MathLinkException;
	 */

	/**
	 * Reads a Java object reference from the link.
	 * <p>
	 * Once you have called enableObjectReferences(), you can pass object references to
	 * Mathematica via putReference() and read them from Mathematica via getObject().
	 *
	 * @return the object
	 * @exception com.wolfram.jlink.MathLinkException
	 * @see #putReference(Object)
	 * @see #enableObjectReferences()
	 */

	// Expects to find a symbol on the link (e.g. Java`Objects`vmname`JavaObject1234). It then
	// strips off the instance number and looks it up.
	Object getObject() throws MathLinkException;

	/**
	 * Extends the MathLink method of the same name by allowing the extra return type MLTKOBJECT. This type
	 * is returned whenever a JavaObject expression is waiting on the link. Note that the symbol Null,
	 * although it is a valid Java object reference, will cause getNext() to return MLTKSYM, not MLTKOBJECT.
	 * @see MathLink#getNext()
	 * @see #enableObjectReferences()
	 */
	int getNext() throws MathLinkException;

	/**
	 * Extends the MathLink method of the same name by allowing the extra return type MLTKOBJECT. This type
	 * is returned whenever a JavaObject expression is waiting on the link. Note that the symbol Null,
	 * although it is a valid Java object reference, will cause getType() to return MLTKSYM, not MLTKOBJECT.
	 * @see MathLink#getType()
	 * @see #enableObjectReferences()
	 */
	int getType() throws MathLinkException;

    /**
     * Extends the MathLink method of the same signature by allowing the extra type specification TYPE_OBJECT.
     * This method allows you to read arrays of object references (i.e, arrays of JavaObject expressions).
     * @see MathLink#getArray(int, int)
     * @see #enableObjectReferences()
     */
    Object getArray(int type, int depth) throws MathLinkException;

    /**
     * Extends the MathLink method of the same signature by allowing the extra type specification TYPE_OBJECT.
     * This method allows you to read arrays of object references (i.e, arrays of JavaObject expressions).
     * @see MathLink#getArray(int, int, String[])
     * @see #enableObjectReferences()
     */
    Object getArray(int type, int depth, String[] heads) throws MathLinkException;

    /**
     * An improvement on the KernelLink.getArray(int type, int depth) signature that allows you to specify
     * the type of array to create when reading arrays of Java objects. The old getArray(int type, int depth) method only
     * allowed you to specify TYPE_OBJECT as the type, and this would result in the creation of an array
     * of objects with an element type determined by the first object in the incoming array. This causes
     * problems for arrays of objects of mixed types. For example, if you were reading a list of two objects from
     * Mathematica where the first element of the list was a Frame object and the second was an Exception
     * object, then calling getArray(KernelLink.TYPE_OBJECT, 1) would cause J/Link to examine the first element
     * (a Frame) and then create a Frame[] to hold all the elements. The next object read is not a Frame,
     * however, and this causes an error. This version of the getArray() method allows you to specify that
     * the created array should be typed only as Object[], which allows both the Frame and the Exception to be
     * stored in the same array:
     * <pre>
     *      Object[] a = (Object[]) ml.getArray(Object.class, 1);</pre>
     *
     * @see MathLink#getArray(int, int)
     * @see KernelLink#getArray(Class, int, String[])
     * @see #enableObjectReferences()
     * @since 4.2
     */
    Object getArray(Class elementType, int depth) throws MathLinkException;

    /**
     * An improvement on the KernelLink.getArray(int type, int depth, String[] heads) signature that allows you to specify
     * the type of array to create when reading arrays of Java objects.
     * See the documentation for the KernelLink.getArray(Class, int) method for more information. This signature
     * fills a String array with the heads of the expression found at each level.
     *
     * @see MathLink#getArray(int, int, String[])
     * @see KernelLink#getArray(Class, int)
     * @see #enableObjectReferences()
     * @since 4.2
     */
    Object getArray(Class elementType, int depth, String[] heads) throws MathLinkException;


    /**
     * Call this method to enable the ability to pass Java objects "by reference"
     * to Mathematica. You must call this before attempting to call putReference(),
     * or put() on an object that will be put by reference.
     * <p>
     * This method requires that the JLink.m file be present in the JLink directory
     * (its default location if J/Link is properly installed).
     *
     * @exception com.wolfram.jlink.MathLinkException
     * @see #enableObjectReferences(boolean)
     * @see #putReference(Object)
     */

    void enableObjectReferences() throws MathLinkException;


    /**
     * Call this method to enable the ability to pass Java objects "by reference"
     * to Mathematica. This version of the method supplants the earlier no-argument version
     * by allowing you to specify a boolean argument
     * that controls whether you want the calling Java runtime to become the default target
     * for J/Link calls from Mathematica into Java. The typical choice for this argument is
     * true, and this is the behavior you get if you call enableObjectReferences() with no
     * arguments. Advanced programmers might want to attach a Java runtime to Mathematica and
     * enable J/Link callbacks into the runtime without it becoming the default target. This
     * is done by calling enableObjectReferences(false). Note that the calling JVM will still
     * become the default target even if false is passed in the case where no other Java runtime
     * is currently attached to Mathematica.
     * <p>
     * The method returns the JVM expression that you can use in UseJVM or other constructs
     * to identify this Java runtime as the target for calls from Java into Mathematica.
     * <p>
     * You must call this before attempting to call putReference(),
     * or put() on an object that will be put by reference.
     * <p>
     * This method requires that the JLink.m file be present in the JLink directory
     * (its default location if J/Link is properly installed).
     *
     * @return the JVM expression you can use to identify the current runtime in Mathematica code
     * @exception com.wolfram.jlink.MathLinkException
     * @see #enableObjectReferences()
     * @see #putReference(Object)
     * @since 4.0
     */

    Expr enableObjectReferences(boolean becomeDefaultJVM) throws MathLinkException;


	/**
	 * Adds the specified PacketListener to receive PacketArrivedEvents.
	 * <p>
	 * Use this method to register a PacketListener object to receive
	 * notifications when packets are read by any of the KernelLink methods that
	 * run internal packet loops. These methods are: waitForAnswer(), discardAnswer(),
	 * evaluateToInputForm(), evaluateToOutputForm(), evaluateToImage(), and evaluateToTypeset().
	 * <p>
	 * J/Link programmers are discouraged from writing their own packet loops (i.e., repeatedly
	 * calling nextPacket() and newPacket() until a desired packet arrives). Instead, they
	 * should create a class that implements PacketListener, register an object of this
	 * class using addPacketListener(), and then use the KernelLink methods that manage
	 * the packet loop for you.
	 *
	 * @param listener the PacketListener to register
	 * @see #removePacketListener(PacketListener)
	 * @see PacketArrivedEvent
	 * @see PacketPrinter
	 */
	void addPacketListener(PacketListener listener);

	/**
	 * Removes the specified PacketListener object so that it no longer receives
	 * PacketArrivedEvents.
	 *
	 * @param listener the PacketListener to remove
	 * @see #addPacketListener(PacketListener)
	 * @see PacketArrivedEvent
	 * @see PacketPrinter
	 */
	void removePacketListener(PacketListener listener);

    /**
     * Call this method to invoke the normal notification of registered PacketListeners
     * if you are manually reading packets with nextPacket().
     * <p>
     * The KernelLink methods that run internal packet loops (waitForAnswer(), discardAnswer(),
     * evaluateToInputForm(), evaluateToOutputForm(), evaluateToImage(), and evaluateToTypeset())
     * call this method for each packet to trigger the notification of registered PacketListeners.
     * If you are reading packets yourself by calling nextPacket() (this is discouraged), you can call
     * notifyPacketListeners() to cause PacketListeners to be notified for these packets as well.
     * Call it right after nextPacket(), passing it the packet type as returned by nextPacket().
     * The state of the link (the current position in the incoming data stream) will be
     * unaffected by the call.
     *
     * @param pkt the packet type (as returned by nextPacket())
     * @return true, if one of the listeners wanted to suppress default packet handling; you will probably ignore this
     * @see #removePacketListener(PacketListener)
     * @see #addPacketListener(PacketListener)
     */

    boolean notifyPacketListeners(int pkt);


    /**
     * Gets the JLinkClassLoader that is used by this link to load classes via the
     * LoadJavaClass[] Mathematica function.
     * <p>
     * In rare cases programmers might need to obtain the class loader that J/Link uses to
     * load classes from Mathematica. One reason for this is if you want to load a class
     * in Java code that is found in the special set of extra locations that J/Link's class loader
     * knows about (such as Java subdirectories of Mathematica application directories; see the
     * J/Link User Guide for more information on how J/Link finds classes). Another reason is if
     * want to obtain a class object from a class name and the class has already been loaded
     * from a special location by J/Link's class loader.
     * <code>
     * Class cls = Class.forName("Some.class.that.only.JLink.can.find", ml.getClassLoader());
     * </code>
     * If you are calling this method from a standalone Java program you should first call
     * enableObjectReferences() to initialize the class loader with the special Mathematica-specific
     * locations for Java classes.
     *
     * @see #setClassLoader(JLinkClassLoader)
     * @see #enableObjectReferences()
     * @see JLinkClassLoader
     */

    JLinkClassLoader getClassLoader();

    /**
     * Sets the JLinkClassLoader that is used by this link to load classes via the
     * LoadJavaClass[] Mathematica function.
     * <p>
     * Advanced programmers might need to specify the class loader that J/Link will use to
     * load classes from Mathematica. The only reason to do this is if you want the class loader
     * to have a specific parent loader, because that parent loader has certain capabilities or
     * can load classes from certain locations that J/Link would otherwise not know about.
     * <code>
     * KernelLink ml = MathLinkFactory.createKernelLink(...);
     * ml.discardAnswer();
     * ml.setClassLoader(new JLinkClassLoader(mySpecialParentClassLoader));
     * ml.enableObjectReferences();
     * </code>
     * This method can only be called in a standalone Java program (i.e., not in Java code called
     * from Mathematica), and should be called before enableObjectReferences().
     *
     * @see #getClassLoader()
     * @see #enableObjectReferences()
     * @see JLinkClassLoader
     */

    void setClassLoader(JLinkClassLoader loader);


	/**
	 * Sends a request to the kernel to abort the current evaluation.
	 * <p>
	 * This method is typically called from a different thread than the one that is performing
	 * the computation. That "computation" thread is typically blocking in waitForAnswer() or some
	 * other method that is waiting for the result to arrive from Mathematica. If you want to
	 * abort the computation so that the main thread does not have to continue waiting, you
	 * can call abortEvaluation() on some other thread (perhaps in response to some user action
	 * like clicking an "abort" button). The computation will terminate and return the symbol $Aborted.
	 * Be aware that Mathematica is not always in a state where it is receptive to abort requests. A quick
	 * return of $Aborted is not always guaranteed.
	 * <p>
	 * What this method does is simply send an MLABORTMESSAGE to the kernel. It is provided for
	 * convenience to shield programmers from such low-level details.
	 *
	 * @since 2.0
	 * @see #interruptEvaluation()
	 * @see #abandonEvaluation()
	 * @see #terminateKernel()
	 */
	void abortEvaluation();

	/**
	 * Sends a request to the kernel to interrupt the current evaluation. Interrupt requests
	 * should not be confused with abort requests. Interrupt requests generate a special
	 * MenuPacket from the kernel that needs a response. You must be prepared to handle this
	 * packet if you call interruptEvaluation(). You can use the com.wolfram.jlink.ui.InterruptDialog
	 * class for this if you choose.
	 * <p>
	 * This method is typically called from a different thread than the one that is performing
	 * the computation. That "computation" thread is typically blocking in waitForAnswer() or some
	 * other method that is waiting for the result to arrive from Mathematica. If you want to
	 * interrupt the computation to provide your users with choices ("abort", "continue", "enter dialog",
	 * etc.), you can call interruptEvaluation() on some other thread (perhaps in response to
	 * the user clicking an "interrupt" button). Be aware that Mathematica is not always in a state
	 * where it is receptive to interrupt requests.
	 * <p>
	 * What this method does is simply send an MLINTERRUPTMESSAGE to the kernel. It is provided for
	 * convenience to shield programmers from such low-level details.
	 *
	 * @since 2.0
	 * @see com.wolfram.jlink.ui.InterruptDialog
	 * @see #abortEvaluation()
	 * @see #abandonEvaluation()
	 * @see #terminateKernel()
	 */
	void interruptEvaluation();

	/**
	 * Causes any method that is blocking waiting for output from the kernel to return immediately
	 * and throw a MathLinkException. This is a "last-ditch" method when you absolutely want to
	 * break out of any methods that are waiting for a result from the kernel. The link should always
	 * be closed after you call this.
	 * <p>
	 * This method is typically called from a different thread than the one that is performing
	 * the computation. That "computation" thread is typically blocking in waitForAnswer() or some
	 * other method that is waiting for the result to arrive from Mathematica. If you want to force that
	 * method to return immediately (it will throw a MathLinkException), call abandonEvaluation().
	 * The code in the catch handler for the MathLinkException will find that clearError() returns
	 * false, indicating that the link is irrecoverably damaged. You should then call terminateKernel()
	 * followed by close().
	 *
	 * @since 2.0
	 * @see #abortEvaluation()
	 * @see #interruptEvaluation()
	 * @see #terminateKernel()
	 */
	void abandonEvaluation();

	/**
	 * Sends a request to the kernel to shut down.
	 * <p>
	 * Most of the time, when you call close() on a link, the kernel will quit. If the kernel is busy
	 * with a computation, however, it will not stop just because the link closes. Use terminateKernel()
	 * to force the kernel to quit even though it may be busy. This is not an operating system-level "kill"
	 * command, and it is not absolutely guaranteed that the kernel will die immediately.
	 * <p>
	 * This method is safe to call from any thread. Any method that is blocking waiting for a result
	 * from Mathematica (such as waitForAnswer()) will return immediately and throw a MathLinkException.
	 * You will typically call close() immediately after terminateKernel(), as the link will die when the
	 * kernel quits.
	 * <p>
	 * A typical usage scenario is as follows. You have a thread that is blocking in waitForAnswer()
	 * waiting for the result of some computation, and you decide that it must return right away and
	 * you are willing to sacrifice the kernel to guarantee this. You then call abandonEvaluation() on
	 * a separate thread. This causes waitForAnswer() to immediately throw a MathLinkException. You
	 * catch this exception, discover that clearError() returns false indicating that the link is hopeless,
	 * and then you call terminateKernel() followed by close(). The reason terminateKernel() is useful
	 * here is that because you called abandonEvaluation(), the kernel may still be computing and it
	 * may not die when you call close(). You call terminateKernel() to give it a little help.
	 * <p>
	 * What this method does is simply send an MLTERMINATEMESSAGE to the kernel. It is provided for
	 * convenience to shield programmers from such low-level details.
	 *
	 * @since 2.0
	 * @see #abortEvaluation()
	 * @see #interruptEvaluation()
	 * @see #abandonEvaluation()
	 */
	void terminateKernel();


	////////////////////////////////////////  Rest are for "StdLink"'s only.  ///////////////////////////////////////

	// It is perhaps a design error that these methods are here, as they are not relevant to all uses of a
	// KernelLink. The alternative is that they be static methods in the StdLink class. It is not feasible
	// to put them in a StdLink subinterface of KernelLink, as there is no way to know at the time the link
	// class is created whether it will ever need the extra capabilities of a StdLink (an ordinary KernelLink
	// can become a "Stdlink" at runtime if the user calls Install[$ParentLink] amd Install.install(kl)).

	// Note that these "output" funcs don't throw MathLinkException.

	/**
	 * Prints the specified text in the user's Mathematica session.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 * <p>
	 * The KernelLink object on which this method will be called will probably be obtained via
	 * {@link StdLink#getLink() StdLink.getLink()}.
	 *
	 * @param s the text to print
	 * @see #message(String, String)
	 * @see #message(String, String[])
	 */

	void print(String s);

	/**
	 * Prints the specified message in the user's Mathematica session.
	 * <p>
	 * Use this form for messages that take at most 1 argument to be spliced into their text.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 * <p>
	 * The KernelLink object on which this method will be called will probably be obtained via
	 * {@link StdLink#getLink() StdLink.getLink()}.
	 *
	 * @param symtag the message designation, in the usual Symbol::tag style
	 * @param arg a string to be spliced into the message text, in the same way it would be used
	 * by Mathematica's Message function. Pass null if no arguments are needed.
	 * @see #message(String, String[])
	 * @see #print(String)
	 */

	void message(String symtag, String arg);

	/**
	 * Prints the specified message in the user's Mathematica session.
	 * <p>
	 * Use this form for messages that take more than one argument to be spliced into their text.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 * <p>
	 * The KernelLink object on which this method will be called will probably be obtained via
	 * {@link StdLink#getLink() StdLink.getLink()}.
	 *
	 * @param symtag the message designation, in the usual Symbol::tag style,
	 * like Mathematica's Message function
	 * @param args an array of strings to be spliced into the message text, in the same way as
	 * they are used by Mathematica's Message function.
	 * @see #message(String, String)
	 * @see #print(String)
	 */

	void message(String symtag, String[] args);

	/**
	 * Informs J/Link that your code will be manually sending the result back to Mathematica.
	 * This circumvents the normal automatic return of whatever the method being called
	 * returns.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 * <p>
	 * The KernelLink object on which this method will be called will probably be obtained via
	 * {@link StdLink#getLink() StdLink.getLink()}.
	 * <p>
	 * The name &quot;beginManual&quot; was chosen instead of, say, &quot;setManual&quot;
	 * to emphasize that the link enters a special mode the moment this method is called.
	 * To allow the most graceful exception handling, you should delay calling beginManual()
	 * until right before you begin to write the result on the link.
	 */

	void beginManual();

	/**
	 * Tells whether the user has attempted to abort the computation.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 * <p>
	 * When the user tries to interrupt a Mathematica computation that is in the middle of a call
	 * into Java, the interrupt request is sent to Java. If a Java method makes no attempt to
	 * honor interrupt requests, then after the method call completes J/Link will execute the
	 * Mathematica function Abort[], causing the entire computation to end and return the result
	 * $Aborted. If you want to detect interrupts within a Java method, for example to break out of
	 * a long Java computation, call wasInterrupted() to determine if an interrupt request has
	 * been received. If it returns true, then you can simply return from your method, and J/Link
	 * will take care of calling Abort[] for you. If the method returns a value, the value will
	 * be ignored. For example, you could put code like the following into a time-intensive loop
	 * you were running:
	 * <pre>
	 *     KernelLink ml = StdLink.getLink();
	 *     if (ml.wasInterrupted())
	 *         return;</pre>
	 * If you want to do something other than call Abort[] in response to the interrupt you should
	 * call beginManual(), send back a result manually, and then call clearInterrupt(). The
	 * clearInterrupt() method tels J/Link that you have handled the interrupt manually and therefore
	 * J/Link should not try to send back Abort[]:
	 * <pre>
	 *     KernelLink ml = StdLink.getLink();
	 *     if (ml.wasInterrupted()) {
	 *         try {
	 *             ml.beginManual();
	 *             ml.put("Interrupted at iteration " + i);
	 *             ml.clearInterrupt();
	 *         } catch (MathLinkException e) {}
	 *         return;
	 *     }</pre>
	 *
	 * @return true, if the user tried to interrupt the execution from their Mathematica
	 * session; false otherwise
	 * @see #clearInterrupt()
	 */

	boolean wasInterrupted();


	/**
	 * Call this method after you have handled an interrupt request manually.
	 * See the documentation for the wasInterrupted() method for more information.
	 * <p>
	 * This method is usable only in Java code that is invoked in a call from
	 * Mathematica, as described in Part 1 of the J/Link User Guide. In other words,
	 * it is only used in code that is called from a Mathematica session via the
	 * &quot;installable Java&quot; mechanism. Programmers who are launching the kernel and
	 * controlling it from Java code will have no use for this method.
	 *
	 * @see #wasInterrupted()
	 * @see #beginManual()
	 */
	void clearInterrupt();

	JSONArray result();
}