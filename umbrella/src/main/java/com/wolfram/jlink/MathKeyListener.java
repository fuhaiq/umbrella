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
 * In response to a KeyEvent, objects of this class send to Mathematica:
 * <PRE>
 *     userCode[theKeyEvent, theKeyEvent.getKeyChar(), theKeyEvent.getKeyCode()]</PRE>
 * 
 * userFunc is specified as a string, either a function name or an expression
 * (like a pure function "foo[##]&"), via the setHandler() method.
 */

public class MathKeyListener extends MathListener implements KeyListener {

	/**
	 * The constructor that is called from Mathematica.
	 */
	
	public MathKeyListener() {
		super();
	}

	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent when KeyEvents arrive.
	 */
	
	public MathKeyListener(KernelLink ml) {
		super(ml);
	}

	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Mathematica code only.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the KeyListener
	 * interface with Mathematica functions.
	 */
	
	public MathKeyListener(String[][] handlers) {
		super(handlers);
	}


	////////////////////////////////////  Event handler methods  /////////////////////////////////////////
	
	public void keyPressed(KeyEvent e) {
		callVoidMathHandler("keyPressed", prepareArgs(e));
	}

	public void keyReleased(KeyEvent e) {
		callVoidMathHandler("keyReleased", prepareArgs(e));
	}

	public void keyTyped(KeyEvent e) {
		callVoidMathHandler("keyTyped", prepareArgs(e));
	}


	private Object[] prepareArgs(KeyEvent e) {
		return new Object[] {e, new Integer(e.getKeyChar()), new Integer(e.getKeyCode())};
	}

}
