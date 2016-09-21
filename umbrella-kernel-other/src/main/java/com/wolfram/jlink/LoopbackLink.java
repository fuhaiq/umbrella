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
 * LoopbackLink is the link interface that represents a special type of link
 * known as a loopback link. Loopback links are links that have both ends
 * connected to the same program, much like a FIFO queue. Loopback links are useful
 * as temporary holders of expressions that are being moved between links, or as
 * scratchpads on which expressions can be built up and then transferred to other
 * links in a single call.
 * Much of the utility of loopback links to users of the C-language MathLink API
 * is obviated by J/Link's Expr class, which provides many of the same features
 * in a more accessible way (Expr uses loopback links heavily in its implementation).
 * <p>
 * Objects of type LoopbackLink are created by the createLoopbackLink method in the
 * MathLinkFactory class.
 * <p>
 * LoopbackLink has no methods; it is simply a type that marks certain links as having
 * special properties.
 *
 * @see Expr
 * @see MathLink
 * @see MathLinkFactory
 */


public interface LoopbackLink extends MathLink {

}
