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
 * ExprFormatException is the exception thrown by the "asXXX()" methods of the Expr class
 * (e.g., asInt(), asDouble(), asArray(), etc.) These methods attempt to return a native Java
 * representation of the Expr's contents, and if this cannot be done because the Expr cannot
 * be represented in the requested form, an ExprFormatException is thrown. For example, if you
 * called asArray() on an Expr that held a String.
 * 
 * @see Expr
 */

public class ExprFormatException extends Exception {

	public ExprFormatException(String msg) {
		super(msg);
	}
}
