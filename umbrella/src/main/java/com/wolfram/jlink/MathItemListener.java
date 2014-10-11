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
 * In response to an ItemEvent, objects of this class send to Mathematica:
 * <PRE>
 *     userCode[theItemEvent, theItemEvent.getStateChange()]</PRE>
 * 
 * userFunc is specified as a string, either a function name or an expression
 * (like a pure function "foo[##]&"), via the setHandler() method.
 */

public class MathItemListener extends MathListener implements ItemListener {

	/**
	 * The constructor that is typically called from Mathematica.
	 */
	
	public MathItemListener() {
		super();
	}
	
	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent when itemStateChanged is called.
	 */
	
	public MathItemListener(KernelLink ml) {
		super(ml);
	}

	/**
	 * Because the ItemListener interface
	 * has only one method, itemStateChanged(), you can specify the Mathematica function to be
	 * called with this constructor, rather than having to separately call setHandler().
	 * Use this constructor from Mathematica code only.
	 * 
	 * @param func The Mathematica function to be executed in response to an ItemEvent.
	 */
	
	public MathItemListener(String func) {
		this();
		setHandler("itemStateChanged", func);
	}


	////////////////////////////////////  Event handler methods  /////////////////////////////////////////
	
	public void itemStateChanged(ItemEvent e) {
		callVoidMathHandler("itemStateChanged", new Object[]{e, new Integer(e.getStateChange())});
	}

}