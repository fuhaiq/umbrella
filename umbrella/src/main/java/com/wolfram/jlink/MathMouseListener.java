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

public class MathMouseListener extends MathListener implements MouseListener {

	/**
	 * The constructor that is called from Mathematica.
	 */
	
	public MathMouseListener() {
		super();
	}

	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent when MouseEvents arrive.
	 */
	
	public MathMouseListener(KernelLink ml) {
		super(ml);
	}

	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Mathematica code only.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the MouseListener
	 * interface with Mathematica functions.
	 */
	
	public MathMouseListener(String[][] handlers) {
		super(handlers);
	}
	

	////////////////////////////////////  Event handler methods  /////////////////////////////////////////
	
	public void mouseClicked(MouseEvent e) {
		callVoidMathHandler("mouseClicked", prepareArgs(e));
	}

	public void mouseEntered(MouseEvent e) {
		callVoidMathHandler("mouseEntered", prepareArgs(e));
	}

	public void mouseExited(MouseEvent e) {
		callVoidMathHandler("mouseExited", prepareArgs(e));
	}

	public void mousePressed(MouseEvent e) {
		callVoidMathHandler("mousePressed", prepareArgs(e));
	}

	public void mouseReleased(MouseEvent e) {
		callVoidMathHandler("mouseReleased", prepareArgs(e));
	}
	

	private Object[] prepareArgs(MouseEvent e) {
		return new Object[]{e, new Integer(e.getX()), new Integer(e.getY()), new Integer(e.getClickCount())};
	}

}
