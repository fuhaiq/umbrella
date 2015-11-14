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
 * MLFunction is a simple class that is nothing more than a holder for a function
 * name and argument count.
 * <p>
 * MLFunction is returned by the getFunction() method in the MathLink interface.
 */

public class MLFunction implements java.io.Serializable {

	/**
	 * The function's name.
	 */

	public String name;

	/**
	 * The function's argument count.
	 */
     
	public int argCount;

	// Note ctor is not public.
	protected MLFunction(String name, int argc) {

		this.name = name;
		this.argCount = argc;
	}
}
