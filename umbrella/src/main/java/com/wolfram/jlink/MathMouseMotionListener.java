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

import java.awt.event.*;

/**
 * This class lets you trigger a call into Mathematica on the occurrence of a particular event.
 * Like all the MathXXXListener classes, it is intended to be used primarily from Mathematica, although it
 * can be used from Java code as well.
 * <P>
 * In response to a MouseEvent, objects of this class send to Mathematica:
 * <PRE>
 *     userCode[theMouseEvent, theMouseEvent.getX(), theMouseEvent.getY(), theMouseEvent.getClickCount()]</PRE>
 * 
 * userFunc is specified as a string, either a function name or an expression
 * (like a pure function "foo[##]&"), via the setHandler() method.
 */

public class MathMouseMotionListener extends MathListener implements MouseMotionListener {

	/**
	 * The constructor that is called from Mathematica.
	 */
	
	public MathMouseMotionListener() {
		super();
	}

	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent when MouseEvents arrive.
	 */
	
	public MathMouseMotionListener(KernelLink ml) {
		super(ml);
	}

	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Mathematica code only.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the MouseMotionListener
	 * interface with Mathematica functions.
	 */
	
	public MathMouseMotionListener(String[][] handlers) {
		super(handlers);
	}


	////////////////////////////////////  Event handler methods  /////////////////////////////////////////
	
	public void mouseDragged(MouseEvent e) {
		callVoidMathHandler("mouseDragged", prepareArgs(e));
	}

	public void mouseMoved(MouseEvent e) {
		callVoidMathHandler("mouseMoved", prepareArgs(e));
	}


	private Object[] prepareArgs(MouseEvent e) {
		return new Object[]{e, new Integer(e.getX()), new Integer(e.getY()), new Integer(e.getClickCount())};
	}

}
