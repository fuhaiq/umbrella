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
 * In response to an ActionEvent, objects of this class send to Mathematica:
 * <PRE>
 *     userFunc[theActionEvent, theActionEvent.getActionCommand()]</PRE>
 * 
 * userFunc is specified as a string, either a function name or an expression
 * (like a pure function "foo[##]&"), via the setHandler() method. Sending the result of getActionCommand()
 * is an optimization to avoid a call back to Java in the likely event that you want this value.
 */

public class MathActionListener extends MathListener implements ActionListener {

	/**
	 * The constructor that is typically called from Mathematica.
	 */
	
	public MathActionListener() {
		super();
	}

	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent when actionPerformed() is called.
	 */
	
	public MathActionListener(KernelLink ml) {
		super(ml);
	}

	/**
	 * Because the ActionListener interface
	 * has only one method, actionPerformed(), you can specify the Mathematica function to be
	 * called with this constructor, rather than having to separately call setHandler().
	 * Use this constructor from Mathematica code only.
	 * 
	 * @param func The Mathematica function to be executed in response to an ActionEvent.
	 */
	
	public MathActionListener(String func) {
		this();
		setHandler("actionPerformed", func);
	}

	
	////////////////////////////////////  Event handler methods  /////////////////////////////////////////
	
	public void actionPerformed(ActionEvent e) {
		callVoidMathHandler("actionPerformed", new Object[]{e, e.getActionCommand()});
	}

}