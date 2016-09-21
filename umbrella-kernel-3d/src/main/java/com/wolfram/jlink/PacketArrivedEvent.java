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
 * The event type that is sent to PacketListeners to notify them that a packet has
 * arrived on the link.
 * <p>
 * PacketArrivedEvents are sent by the KernelLink methods that run internal packet loops,
 * reading and perhaps discarding packets. These methods are: waitForAnswer(), discardAnswer(),
 * evaluateToInputForm(), evaluateToOutputForm(), evaluateToImage(), and evaluateToTypeset().
 * 
 * @see PacketListener
 * @see KernelLink
 */

public class PacketArrivedEvent extends java.util.EventObject {
	
	private int pkt;

	/**
	 * @param ml
	 * @param pkt the packet type (e.g., RETURNPKT, TEXTPKT, DISPLAYPKT, etc.)
	 */
	
	PacketArrivedEvent(KernelLink ml, int pkt) {
		super(ml);  // So ml can be retrieved by getSource().
		this.pkt = pkt;
	}

	/**
	 * Gives the type of packet that has arrived.
	 * 
	 * @return the packet type (an integer constant from the set defined in the MathLink class)
	 */
	
	public int getPktType() {
		return pkt;
	}

	/**
	 * Gives the link on which the packet has arrived.
	 * 
	 * @return the link
	 */
	
	// Include this just so that the method gets into the javadocs
	public Object getSource() {
		return super.getSource();
	}

	/**
	 * Gives a readable string describing the packet type.
	 * 
	 * @return the description string
	 */
	
	public String toString() {
		return "PacketArrivedEvent on KernelLink " +
				 getSource().toString() +
				 ". Packet type was " +
				 pkt;
	}
}